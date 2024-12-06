package com.example.insync_smartcontrolsystem;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private static final String TAG = "StatisticsActivity";
    private LineChart chart;
    private DatabaseReference chartsRef;
    private BottomNavigationView bottomNavigationView;
    private TextView soundDataValue;
    private TextView temperatureDataValue;
    private TextView humidityDataValue;
    private DatabaseReference sensorDataRef;
    private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_statistics);
            Log.d(TAG, "onCreate started");

            // Hide the action bar if present
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            try {
                // Initialize views first
                initializeViews();

                // Only proceed with other initializations if views are properly initialized
                if (chart != null && bottomNavigationView != null) {
                    // Setup bottom navigation
                    setupBottomNavigation();

                    // Initialize Firebase
                    initializeFirebase();

                    // Initialize sensor data
                    initializeSensorData();
                } else {
                    Log.e(TAG, "Essential views are null. Aborting initialization.");
                    showToast("Error initializing views");
                    finish();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in initialization: " + e.getMessage(), e);
                showToast("Error initializing application");
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void initializeViews() {
        try {
            // Find all required views
            chart = findViewById(R.id.chart);
            bottomNavigationView = findViewById(R.id.bottomNavigationView);
            soundDataValue = findViewById(R.id.sound_data_value);
            temperatureDataValue = findViewById(R.id.temperature_data_value);
            humidityDataValue = findViewById(R.id.humidity_data_value);
            
            if (chart == null || bottomNavigationView == null || 
                soundDataValue == null || temperatureDataValue == null || 
                humidityDataValue == null) {
                throw new IllegalStateException("One or more required views not found");
            }
            
            // Initialize chart with safe defaults
            setupChart();
            
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw e; // Rethrow to be handled by onCreate
        }
    }

    private void setupChartAndLoadData() {
        try {
            if (chart == null) {
                Log.e(TAG, "Cannot setup chart - chart view is null");
                return;
            }

            // Load sample data first
            loadSampleData();
            Log.d(TAG, "Sample data loaded");

            // Then try to load real data
            loadChartData();
            Log.d(TAG, "Started loading real data");

        } catch (Exception e) {
            Log.e(TAG, "Error in setupChartAndLoadData: " + e.getMessage());
            showToast("Error setting up chart");
        }
    }

    private void initializeFirebase() {
        try {
            if (isFinishing()) {
                Log.w(TAG, "Activity is finishing, skipping Firebase initialization");
                return;
            }

            FirebaseDatabase database = FirebaseDatabase.getInstance("https://insyncweb-default-rtdb.firebaseio.com/");
            if (database == null) {
                throw new IllegalStateException("Failed to get Firebase instance");
            }
            Log.d(TAG, "Firebase instance obtained successfully");

            // Get reference to the charts data
            chartsRef = database.getReference("charts/chartData");
            if (chartsRef == null) {
                throw new IllegalStateException("Failed to get charts reference");
            }
            Log.d(TAG, "Firebase reference obtained: " + chartsRef.toString());

            // Test the connection and check data
            database.getReference(".info/connected").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing()) return;
                    
                    boolean connected = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    if (connected) {
                        Log.d(TAG, "Connected to Firebase successfully");
                        loadChartData();
                    } else {
                        Log.e(TAG, "Not connected to Firebase");
                        showToast("Not connected to database. Loading sample data.");
                        loadSampleData();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isFinishing()) return;
                    
                    Log.e(TAG, "Firebase connection check failed: " + error.getMessage());
                    showToast("Database connection error. Loading sample data.");
                    loadSampleData();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage(), e);
            showToast("Error initializing database. Loading sample data.");
            loadSampleData();
        }
    }

    private Map<String, Object> createSampleDataPoint(long value) {
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("values", value);
        return dataPoint;
    }

    private void setupChart() {
        try {
            // Basic chart setup
            chart.setDrawGridBackground(false);
            chart.getDescription().setEnabled(false);
            chart.setTouchEnabled(true);
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);
            chart.setPinchZoom(true);
            chart.setBackgroundColor(Color.WHITE);
            chart.setNoDataText("Loading chart data...");
            chart.setNoDataTextColor(Color.BLACK);

            // X-Axis setup
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
            xAxis.setTextColor(Color.BLACK);
            xAxis.setTextSize(12f);
            xAxis.setLabelCount(6);

            // Left Y-Axis setup
            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setTextColor(Color.BLACK);
            leftAxis.setTextSize(12f);

            // Disable right Y-Axis
            chart.getAxisRight().setEnabled(false);

            // Enable legend
            chart.getLegend().setEnabled(true);
            chart.getLegend().setTextColor(Color.BLACK);
            chart.getLegend().setTextSize(12f);

            // Enable animation
            chart.animateX(1500);

            Log.d(TAG, "Chart setup successful");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up chart: " + e.getMessage());
            showToast("Error setting up chart");
        }
    }

    private void loadSampleData() {
        try {
            List<Entry> entries = new ArrayList<>();
            // Add sample data points
            entries.add(new Entry(0, 50f));
            entries.add(new Entry(1, 75f));
            entries.add(new Entry(2, 60f));
            entries.add(new Entry(3, 85f));
            entries.add(new Entry(4, 70f));
            entries.add(new Entry(5, 90f));

            LineDataSet dataSet = createLineDataSet(entries, "Sample Energy Usage");
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.invalidate();
            Log.d(TAG, "Sample data loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading sample data: " + e.getMessage());
            showToast("Error loading sample data");
        }
    }

    private void loadChartData() {
        if (chartsRef == null) {
            Log.e(TAG, "Charts reference is null");
            loadSampleData();
            return;
        }

        Log.d(TAG, "Starting to load chart data from path: " + chartsRef.toString());
        
        chartsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (!snapshot.exists()) {
                        Log.w(TAG, "No data exists at path: " + chartsRef.toString());
                        loadSampleData();
                        return;
                    }

                    Log.d(TAG, "Raw data received from Firebase: " + snapshot.getValue());
                    
                    List<Entry> entries = new ArrayList<>();
                    boolean hasValidData = false;
                    
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        try {
                            String key = dataSnapshot.getKey();
                            Object value = dataSnapshot.getValue();
                            
                            Log.d(TAG, "Processing data point - Key: " + key + ", Value: " + value);
                            
                            if (key != null && value != null) {
                                int index = Integer.parseInt(key);
                                float dataValue;
                                
                                if (value instanceof Number) {
                                    dataValue = ((Number) value).floatValue();
                                    entries.add(new Entry(index, dataValue));
                                    hasValidData = true;
                                    Log.d(TAG, "Added entry: index=" + index + ", value=" + dataValue);
                                } else if (value instanceof Map) {
                                    // Handle case where value might be nested
                                    Map<String, Object> valueMap = (Map<String, Object>) value;
                                    if (valueMap.containsKey("value")) {
                                        Object nestedValue = valueMap.get("value");
                                        if (nestedValue instanceof Number) {
                                            dataValue = ((Number) nestedValue).floatValue();
                                            entries.add(new Entry(index, dataValue));
                                            hasValidData = true;
                                            Log.d(TAG, "Added nested entry: index=" + index + ", value=" + dataValue);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing entry: " + e.getMessage());
                        }
                    }

                    if (hasValidData) {
                        Collections.sort(entries, (e1, e2) -> Float.compare(e1.getX(), e2.getX()));
                        Log.d(TAG, "Final entries count: " + entries.size());
                        
                        LineDataSet dataSet = createLineDataSet(entries, "Monthly Usage");
                        LineData lineData = new LineData(dataSet);
                        chart.setData(lineData);
                        chart.invalidate();
                        
                        Log.d(TAG, "Chart updated successfully with Firebase data");
                    } else {
                        Log.w(TAG, "No valid entries found in Firebase data");
                        loadSampleData();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing Firebase data: " + e.getMessage(), e);
                    loadSampleData();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase data load cancelled: " + error.getMessage());
                showToast("Error loading data: " + error.getMessage());
                loadSampleData();
            }
        });
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(true);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setCircleRadius(4f);
        return dataSet;
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_statistics);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_statistics) {
                return true;
            } else if (itemId == R.id.nav_learning) {
                startActivity(new Intent(this, LearningActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void initializeSensorData() {
        DatabaseReference sensorRef = FirebaseDatabase.getInstance("https://insyncweb-default-rtdb.firebaseio.com/").getReference("sensors");
        DatabaseReference devicesRef = FirebaseDatabase.getInstance("https://insyncweb-default-rtdb.firebaseio.com/").getReference("devices");
        
        // Smart Speaker (Sound) Data Listener
        devicesRef.child("speaker").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                if (deviceSnapshot.exists()) {
                    int speakerState = deviceSnapshot.getValue(Integer.class);
                    boolean isSpeakerOn = speakerState == 1;

                    // Add a listener for sound data that updates in real-time
                    sensorRef.child("sound").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (isSpeakerOn && snapshot.exists()) {
                                Object value = snapshot.getValue();
                                String soundLevel = null;
                                if (value instanceof Double) {
                                    soundLevel = String.format("%.1f", (Double) value);
                                } else if (value instanceof Long) {
                                    soundLevel = String.valueOf((Long) value);
                                } else if (value instanceof String) {
                                    soundLevel = (String) value;
                                }
                                soundDataValue.setText(soundLevel != null ? soundLevel + " dB" : "N/A");
                            } else {
                                soundDataValue.setText("Sound: OFF");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fetching sound data: " + error.getMessage());
                            soundDataValue.setText("Error");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching speaker state: " + error.getMessage());
            }
        });

        // Thermometer (Temperature) Data Listener
        devicesRef.child("thermometer").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                if (deviceSnapshot.exists()) {
                    int thermometerState = deviceSnapshot.getValue(Integer.class);
                    boolean isThermometerOn = thermometerState == 1;

                    // Add a listener for temperature data that updates in real-time
                    sensorRef.child("temperature").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (isThermometerOn && snapshot.exists()) {
                                Double temperature = snapshot.getValue(Double.class);
                                String tempDisplay = temperature != null ? String.format("%.1f °C", temperature) : "N/A";
                                temperatureDataValue.setText(tempDisplay);
                            } else {
                                temperatureDataValue.setText("Temp: OFF");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fetching temperature data: " + error.getMessage());
                            temperatureDataValue.setText("Error");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching thermometer state: " + error.getMessage());
            }
        });

        // Humidity Data Listener (updates in real-time)
        sensorRef.child("humidity").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object value = snapshot.getValue();
                    String humidity = null;
                    if (value instanceof Double) {
                        humidity = String.format("%.1f", (Double) value);
                    } else if (value instanceof Long) {
                        humidity = String.valueOf((Long) value);
                    } else if (value instanceof String) {
                        humidity = (String) value;
                    }
                    humidityDataValue.setText(humidity != null ? humidity + " %" : "N/A");
                } else {
                    humidityDataValue.setText("N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching humidity data: " + error.getMessage());
                humidityDataValue.setText("Error");
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
