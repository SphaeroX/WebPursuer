package com.example.webpursuer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.webpursuer.ui.HomeScreen
import com.example.webpursuer.ui.MonitorViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: MonitorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onAddMonitorClick = {
                        // TODO: Navigate to Add Monitor Screen
                        // For now, let's just add a dummy monitor for testing
                        viewModel.addMonitor(
                            com.example.webpursuer.data.Monitor(
                                url = "https://example.com",
                                name = "Example Monitor",
                                selector = "body"
                            )
                        )
                    }
                )
            }
        }
    }
}
