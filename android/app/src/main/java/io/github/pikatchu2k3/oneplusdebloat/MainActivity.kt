package io.github.pikatchu2k3.oneplusdebloat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.pikatchu2k3.oneplusdebloat.ui.CleanupApp
import io.github.pikatchu2k3.oneplusdebloat.ui.CleanupTheme
import io.github.pikatchu2k3.oneplusdebloat.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CleanupTheme {
                CleanupApp(viewModel)
            }
        }
    }
}
