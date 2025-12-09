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

                // Check for shared text intent
                var startUrl: String? = null
                if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        startUrl = com.example.webpursuer.util.UrlUtils.extractUrlFromText(sharedText)
                    }
                }

                if (startUrl != null) {
                   // If we have a shared URL, launch BrowserActivity directly and finish this one to avoid back stack confusion or just keep it?
                   // User likely wants to create a monitor immediately.
                   // We should do this in a LaunchedEffect or similar side effect to not block composition, 
                   // or just start activity and finish?
                   // Since we are in setContent, better use LaunchedEffect.
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                       val browserIntent = android.content.Intent(context, com.example.webpursuer.ui.BrowserActivity::class.java).apply {
                           putExtra("initialUrl", startUrl)
                       }
                       context.startActivity(browserIntent)
                       // Optional: finish MainActivity if you want the user to return to the previous app after saving.
                       // For now, let's keep MainActivity in back stack so they return to app home.
                    }
                }

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
