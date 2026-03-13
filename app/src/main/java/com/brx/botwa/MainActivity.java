package com.brx.botwa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout simples criado programaticamente para evitar conflitos de recursos
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Bot WhatsApp Integrado");
        title.setTextSize(24);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, 50);
        layout.addView(title);

        Button btnStart = new Button(this);
        btnStart.setText("Iniciar Bot");
        btnStart.setPadding(20, 20, 20, 20);
        btnStart.setOnClickListener(v -> {
            // Inicia a atividade do terminal (antiga BotWAActivity)
            Intent intent = new Intent(this, com.brx.botwa.app.BotWAActivity.class);
            startActivity(intent);
        });
        layout.addView(btnStart);

        setContentView(layout);
    }
}
