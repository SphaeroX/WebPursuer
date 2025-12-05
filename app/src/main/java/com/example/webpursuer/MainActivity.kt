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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            MaterialTheme {
                val monitorViewModel: MonitorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val reportViewModel: com.example.webpursuer.ui.ReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                HomeScreen(
                    monitorViewModel = monitorViewModel,
                    reportViewModel = reportViewModel,
                    onAddMonitorClick = {
                        val intent = android.content.Intent(this, com.example.webpursuer.ui.BrowserActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
