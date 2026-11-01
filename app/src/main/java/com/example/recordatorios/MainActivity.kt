package com.example.recordatorios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.recordatorios.ui.navigation.AppNavHost
import com.example.recordatorios.ui.theme.RecordatoriosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecordatoriosTheme {
                AppNavHost()
            }
        }
    }
}
