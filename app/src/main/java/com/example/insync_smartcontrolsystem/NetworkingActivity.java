package com.example.insync_smartcontrolsystem;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NetworkingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networking); // Ensure this matches your XML layout file

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set window insets padding for edge-to-edge effect
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.networking_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
