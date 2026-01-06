package com.murmli.webpursuer.utils

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
import com.murmli.webpursuer.data.GeneratedReport
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportManager {

    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40
    private const val CONTENT_WIDTH_MAX = PAGE_WIDTH - (2 * MARGIN)

    fun exportReportAsPdf(context: Context, report: GeneratedReport) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint =
                Paint().apply {
                    color = Color.BLACK
                    textSize = 24f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
        val metaPaint =
                Paint().apply {
                    color = Color.DKGRAY
                    textSize = 12f
                    isAntiAlias = true
                }
        val textPaint =
                TextPaint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    isAntiAlias = true
                }
        val footerPaint =
                Paint().apply {
                    color = Color.GRAY
                    textSize = 10f
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                }

        // Prepare text content
        val htmlContent = convertMarkdownToHtml(report.content)
        val spannedText = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY)

        // Create StaticLayout to measure and layout text
        val staticLayout =
                StaticLayout.Builder.obtain(
                                spannedText,
                                0,
                                spannedText.length,
                                textPaint,
                                CONTENT_WIDTH_MAX
                        )
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(4f, 1f)
                        .setIncludePad(true)
                        .build()

        // Pagination variables
        var currentY = 0
        var pageNumber = 1
        val totalHeight = staticLayout.height
        var textStartLine = 0

        while (textStartLine < staticLayout.lineCount) {
            val pageInfo =
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yOffset = MARGIN.toFloat()

            // Draw Header (Title & Meta) only on first page, or simplified on others?
            // Let's do a nice header on first page, minimal on others or just content.
            if (pageNumber == 1) {
                // Title
                canvas.drawText("WebPursuer Report", MARGIN.toFloat(), yOffset + 24, titlePaint)
                yOffset += 40

                // Metadata
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date(report.timestamp))
                canvas.drawText("Date: $dateStr", MARGIN.toFloat(), yOffset, metaPaint)
                canvas.drawText(
                        "Report ID: #${report.reportId}",
                        MARGIN.toFloat() + 200,
                        yOffset,
                        metaPaint
                )
                yOffset += 30

                // Separator line
                paint.color = Color.LTGRAY
                paint.strokeWidth = 1f
                canvas.drawLine(
                        MARGIN.toFloat(),
                        yOffset,
                        (PAGE_WIDTH - MARGIN).toFloat(),
                        yOffset,
                        paint
                )
                yOffset += 20
            } else {
                // Space for top margin on subsequent pages
                yOffset = MARGIN.toFloat()
            }

            // Calculate available height for text on this page
            // Footer takes ~20pts at bottom
            val footerHeight = 20
            val availableHeight = PAGE_HEIGHT - MARGIN - footerHeight - yOffset

            // Determine how many lines verify fit in this page
            val startLineTop = staticLayout.getLineTop(textStartLine)
            // We want finding the line whose bottom is <= startLineTop + availableHeight
            // But getLineTop is relative to the layout start.
            // current layout top relative to this page is drawn at yOffset.
            // So we need portion of staticLayout from 'startLineTop' to 'startLineTop +
            // availableHeight'

            var textEndLine =
                    staticLayout.getLineForVertical(startLineTop + availableHeight.toInt())
            // getLineForVertical returns the line containing that point.
            // If the point is past the last line, it returns the last line.
            // We need to ensure we don't split a line if it doesn't fit fully,
            // but standard getLineForVertical is usually okay for text,
            // except if line height > available height (unlikely)

            // Adjust if end line is the last line
            if (textEndLine >= staticLayout.lineCount) {
                textEndLine = staticLayout.lineCount - 1
            } else {
                // Check if the end line actually fits completely
                if (staticLayout.getLineBottom(textEndLine) > startLineTop + availableHeight) {
                    textEndLine-- // Move back one line if it exceeds
                }
            }

            if (textEndLine < textStartLine) {
                // Should not happen unless available height is extremely small
                textEndLine = textStartLine
            }

            // Draw content
            canvas.save()
            canvas.translate(MARGIN.toFloat(), yOffset)

            // We clip to show only the lines for this page.
            // Use translate to move the static layout "up" so the correct lines appear at (0,0) of
            // our translated canvas
            // Effectively: we interpret (0,0) as the top of 'textStartLine'

            val drawHeight = staticLayout.getLineBottom(textEndLine) - startLineTop
            canvas.clipRect(0, 0, CONTENT_WIDTH_MAX, drawHeight)
            canvas.translate(0f, -startLineTop.toFloat())

            staticLayout.draw(canvas)
            canvas.restore()

            // Draw Footer
            val footerY = (PAGE_HEIGHT - MARGIN).toFloat()
            canvas.drawText(
                    "Page $pageNumber",
                    (PAGE_WIDTH - MARGIN).toFloat(),
                    footerY,
                    footerPaint
            )

            pdfDocument.finishPage(page)

            pageNumber++
            textStartLine = textEndLine + 1
        }

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
            try {
                pdfDocument.close()
            } catch (ex: Exception) {
                /* ignore */
            }
        }
    }

    private fun sharePdf(context: Context, file: File) {
        val uri: Uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

        val chooser = Intent.createChooser(intent, "Share Report PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        return markdown.replace("\n", "<br>")
                .replace(Regex("# (.*)"), "<h1>$1</h1>")
                .replace(Regex("## (.*)"), "<h2>$1</h2>")
                .replace(Regex("### (.*)"), "<h3>$1</h3>")
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
                .replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
                .replace(
                        Regex("`(.*?)`"),
                        "<span style=\"background-color: #E0E0E0; font-family: monospace;\">$1</span>"
                )
    }
}
