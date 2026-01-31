package com.apka.terminarzkliniki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.apka.terminarzkliniki.notifications.ensureNotificationChannel
import com.apka.terminarzkliniki.ui.screens.AppNav
import com.apka.terminarzkliniki.ui.theme.TerminarzKlinikiTheme

// Główna aktywność aplikacji – uruchamia Compose i nawigację.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kanał powiadomień
        ensureNotificationChannel(this)

        setContent {
            TerminarzKlinikiTheme(dynamicColor = false) {
                AppNav()
            }
        }
    }
}
