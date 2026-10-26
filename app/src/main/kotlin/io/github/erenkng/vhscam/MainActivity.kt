package io.github.erenkng.vhscam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.erenkng.vhscam.ui.AppRoot
import io.github.erenkng.vhscam.ui.theme.VhsCamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            VhsCamTheme {
                AppRoot()
            }
        }
    }
}
