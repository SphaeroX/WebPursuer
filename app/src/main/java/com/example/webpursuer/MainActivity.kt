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
                val context = androidx.compose.ui.platform.LocalContext.current
                val database = com.example.webpursuer.data.AppDatabase.getDatabase(context)
                val generatedReportRepository = com.example.webpursuer.data.GeneratedReportRepository(database.generatedReportDao())

                val monitorViewModel: MonitorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val reportViewModel: com.example.webpursuer.ui.ReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                
                val monitorId = intent.getIntExtra("monitorId", -1).takeIf { it != -1 }
                val checkLogId = intent.getIntExtra("checkLogId", -1).takeIf { it != -1 }
                val generatedReportId = intent.getIntExtra("generatedReportId", -1).takeIf { it != -1 }

                HomeScreen(
                    monitorViewModel = monitorViewModel,
                    reportViewModel = reportViewModel,
                    generatedReportRepository = generatedReportRepository,
                    initialDiffLogId = checkLogId,
                    initialMonitorId = monitorId,
                    initialGeneratedReportId = generatedReportId,
                    onAddMonitorClick = {
                        val intent = android.content.Intent(this, com.example.webpursuer.ui.BrowserActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
