package com.example.webpursuer.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import com.example.webpursuer.data.GeneratedReport
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfExportManager {

    fun exportReportAsPdf(context: Context, report: GeneratedReport) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Prepare text content (convert markdown/text to simple HTML for basic formatting)
        val htmlContent = convertMarkdownToHtml(report.content)
        val spannedText = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val staticLayout = StaticLayout.Builder.obtain(
            spannedText,
            0,
            spannedText.length,
            textPaint,
            pageInfo.pageWidth - 40 // 20 padding each side
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()

        // Draw on canvas
        canvas.translate(20f, 20f)
        staticLayout.draw(canvas)

        pdfDocument.finishPage(page)

        // Save file
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val file = File(reportsDir, "report_${report.id}.pdf")

        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            sharePdf(context, file)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("PdfExport", "Error writing PDF: ${e.message}")
        }
    }

    private fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Share Report PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required when starting from non-activity context if applicable, though usually context is activity
        context.startActivity(chooser)
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        // Very basic conversion
        var html = markdown
            .replace("\n", "<br>")
            .replace(Regex("# (.*)"), "<h1>$1</h1>")
            .replace(Regex("## (.*)"), "<h2>$1</h2>")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            .replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
        
        return html
    }
}
