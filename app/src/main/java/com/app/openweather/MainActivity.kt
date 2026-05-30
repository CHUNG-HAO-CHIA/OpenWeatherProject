package com.app.openweather

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.openweather.navigation.AppNavGraph
import com.app.openweather.ui.theme.OpenWeatherTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenWeatherTheme {
                AppNavGraph()
            }
        }
    }
}
