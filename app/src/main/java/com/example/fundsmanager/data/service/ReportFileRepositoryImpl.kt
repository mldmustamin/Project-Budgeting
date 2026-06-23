package com.example.fundsmanager.data.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.fundsmanager.domain.model.ProjectReportData
import com.example.fundsmanager.domain.model.TransactionReportRow
import com.example.fundsmanager.domain.service.ReportFile
import com.example.fundsmanager.domain.service.ReportFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ReportFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReportFileRepository {

    override suspend fun createPdf(reportData: ProjectReportData): ReportFile = withContext(Dispatchers.IO) {
        val file = reportFile(reportData.project.name, "pdf")
        writePdf(reportData, file)
        ReportFile(file, PDF_MIME, "Bagikan PDF")
    }

    override suspend fun createExcel(reportData: ProjectReportData): ReportFile = withContext(Dispatchers.IO) {
        val file = reportFile(reportData.project.name, "xlsx")
        writeXlsx(reportData, file)
        ReportFile(file, XLSX_MIME, "Bagikan Excel")
    }

    private fun reportFile(projectName: String, extension: String): File {
        val safeProjectName = projectName.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "Project" }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        return File(reportsDir, "FundsManager_${safeProjectName}_$timestamp.$extension")
    }

    private fun writePdf(reportData: ProjectReportData, file: File) {
        val document = PdfDocument()
        val pageWidth = 842
        val pageHeight = 595
        val margin = 32f
        val lineHeight = 16f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = 0xFF202124.toInt() }
        val titlePaint = Paint(paint).apply { textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val sectionPaint = Paint(paint).apply { textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val headerPaint = Paint(paint).apply { textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFFFFFFFF.toInt() }
        val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 0.8f; color = 0xFFB8C2CC.toInt() }
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        var pageNumber = 0
        var page = newPage(document, pageWidth, pageHeight, ++pageNumber)
        var canvas = page.canvas
        var y = margin

        fun finishPage() {
            drawFooter(canvas, paint, pageWidth, pageHeight, pageNumber)
            document.finishPage(page)
        }

        fun ensureSpace(required: Float) {
            if (y + required <= pageHeight - margin - 18f) return
            finishPage()
            page = newPage(document, pageWidth, pageHeight, ++pageNumber)
            canvas = page.canvas
            y = margin
        }

        canvas.drawText("Funds Manager", margin, y, titlePaint)
        y += 20f
        canvas.drawText("Laporan Project", margin, y, sectionPaint)
        y += lineHeight
        canvas.drawText("Project: ${reportData.project.name}", margin, y, paint)
        y += lineHeight
        canvas.drawText("Tanggal Export: ${reportData.exportedAt}", margin, y, paint)
        y += 22f

        canvas.drawText("Ringkasan", margin, y, sectionPaint)
        y += 14f
        val summaryRows = listOf(
            "Total Dana Masuk" to reportData.summary.totalFundIn,
            "Posisi Bersih" to reportData.summary.netPosition,
            "Dilaporkan ke Kantor" to reportData.summary.totalOfficeReported,
            "Pengeluaran Real" to reportData.summary.totalOfficeReal,
            "Expense Pribadi" to reportData.summary.totalPersonalExpense,
            "Total Keluar Real" to reportData.summary.totalCashOut,
            "Selisih / Hemat" to reportData.summary.saving,
            "Sisa Berdasarkan Laporan" to reportData.summary.remainingReported,
            "Sisa Real" to reportData.summary.remainingReal
        )
        summaryRows.chunked(3).forEach { row ->
            ensureSpace(28f)
            row.forEachIndexed { index, item ->
                val x = margin + index * 250f
                canvas.drawText(item.first, x, y, paint)
                drawRightText(canvas, formatRupiah(item.second), x + 210f, y, paint)
            }
            y += 18f
        }
        y += 10f

        canvas.drawText("Transaksi", margin, y, sectionPaint)
        y += 12f
        drawPdfTableHeader(canvas, margin, y, headerPaint, fillPaint, borderPaint)
        y += 22f

        if (reportData.transactions.isEmpty()) {
            ensureSpace(24f)
            canvas.drawText("Belum ada transaksi.", margin, y, paint)
            y += 18f
        } else {
            reportData.transactions.forEachIndexed { index, row ->
                val descriptionLines = wrapText(row.description, paint, 175f).take(3)
                val noteLines = wrapText(row.note.orEmpty(), paint, 90f).take(2)
                val rowHeight = maxOf(30f, (maxOf(descriptionLines.size, noteLines.size) * 11f) + 14f)
                ensureSpace(rowHeight)
                fillPaint.color = if (index % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF6F8FA.toInt()
                canvas.drawRect(margin, y - 13f, pageWidth - margin, y - 13f + rowHeight, fillPaint)
                canvas.drawRect(margin, y - 13f, pageWidth - margin, y - 13f + rowHeight, borderPaint)
                canvas.drawText(row.date, margin + 4f, y, paint)
                canvas.drawText(row.typeLabel, margin + 76f, y, paint)
                descriptionLines.forEachIndexed { lineIndex, line -> canvas.drawText(line, margin + 158f, y + lineIndex * 11f, paint) }
                canvas.drawText(row.accountName.ifBlank { "-" }, margin + 340f, y, paint)
                canvas.drawText(row.categoryName ?: "-", margin + 425f, y, paint)
                drawRightText(canvas, formatRupiah(row.reportedAmount), margin + 590f, y, paint)
                drawRightText(canvas, formatRupiah(row.realAmount), margin + 682f, y, paint)
                drawRightText(canvas, formatRupiah(row.saving), margin + 750f, y, paint)
                noteLines.forEachIndexed { lineIndex, line -> canvas.drawText(line, margin + 754f, y + lineIndex * 11f, paint) }
                canvas.drawText(row.receiptStatus, margin + 754f, y + 22f, paint)
                y += rowHeight
            }
        }
        finishPage()
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }

    private fun newPage(document: PdfDocument, width: Int, height: Int, pageNumber: Int): PdfDocument.Page {
        return document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
    }

    private fun drawPdfTableHeader(canvas: Canvas, x: Float, y: Float, paint: Paint, fillPaint: Paint, borderPaint: Paint) {
        fillPaint.color = 0xFF1F4E79.toInt()
        canvas.drawRect(x, y - 13f, 810f, y + 8f, fillPaint)
        canvas.drawRect(x, y - 13f, 810f, y + 8f, borderPaint)
        listOf("Tanggal", "Jenis", "Keterangan", "Akun", "Kategori", "Dilaporkan", "Real", "Selisih", "Catatan / Bukti")
            .zip(listOf(4f, 76f, 158f, 340f, 425f, 530f, 622f, 690f, 754f))
            .forEach { (label, offset) -> canvas.drawText(label, x + offset, y, paint) }
    }

    private fun drawFooter(canvas: Canvas, paint: Paint, pageWidth: Int, pageHeight: Int, pageNumber: Int) {
        canvas.drawText("Generated by Funds Manager", 32f, pageHeight - 18f, paint)
        drawRightText(canvas, "Offline-first local report | Halaman $pageNumber", pageWidth - 32f, pageHeight - 18f, paint)
    }

    private fun drawRightText(canvas: Canvas, text: String, rightX: Float, y: Float, paint: Paint) {
        canvas.drawText(text, rightX - paint.measureText(text), y, paint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("-")
        val lines = mutableListOf<String>()
        var current = ""
        text.split(' ').forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotBlank()) lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        return lines.ifEmpty { listOf(text) }
    }

    private fun writeXlsx(reportData: ProjectReportData, file: File) {
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.textEntry("[Content_Types].xml", contentTypesXml())
            zip.textEntry("_rels/.rels", rootRelsXml())
            zip.textEntry("xl/workbook.xml", workbookXml())
            zip.textEntry("xl/_rels/workbook.xml.rels", workbookRelsXml())
            zip.textEntry("xl/styles.xml", stylesXml())
            zip.textEntry("xl/worksheets/sheet1.xml", summarySheetXml(reportData))
            zip.textEntry("xl/worksheets/sheet2.xml", transactionsSheetXml(reportData.transactions))
        }
    }

    private fun summarySheetXml(reportData: ProjectReportData): String {
        val rows = listOf(
            1 to listOf(textCell("A", 1, "Funds Manager"), textCell("B", 1, "Laporan Project")),
            2 to listOf(textCell("A", 2, "Project"), textCell("B", 2, reportData.project.name)),
            3 to listOf(textCell("A", 3, "Tanggal Export"), textCell("B", 3, reportData.exportedAt)),
            5 to listOf(textCell("A", 5, "Total Dana Masuk"), numberCell("B", 5, reportData.summary.totalFundIn)),
            6 to listOf(textCell("A", 6, "Posisi Bersih"), numberCell("B", 6, reportData.summary.netPosition)),
            7 to listOf(textCell("A", 7, "Dilaporkan ke Kantor"), numberCell("B", 7, reportData.summary.totalOfficeReported)),
            8 to listOf(textCell("A", 8, "Pengeluaran Real"), numberCell("B", 8, reportData.summary.totalOfficeReal)),
            9 to listOf(textCell("A", 9, "Expense Pribadi"), numberCell("B", 9, reportData.summary.totalPersonalExpense)),
            10 to listOf(textCell("A", 10, "Total Keluar Real"), numberCell("B", 10, reportData.summary.totalCashOut)),
            11 to listOf(textCell("A", 11, "Selisih / Hemat"), numberCell("B", 11, reportData.summary.saving)),
            12 to listOf(textCell("A", 12, "Sisa Berdasarkan Laporan"), numberCell("B", 12, reportData.summary.remainingReported)),
            13 to listOf(textCell("A", 13, "Sisa Real"), numberCell("B", 13, reportData.summary.remainingReal)),
            15 to listOf(textCell("A", 15, "Generated by Funds Manager"), textCell("B", 15, "Offline-first local report"))
        )
        return worksheetXml(rows)
    }

    private fun transactionsSheetXml(transactions: List<TransactionReportRow>): String {
        val rows = mutableListOf<Pair<Int, List<String>>>()
        rows += 1 to listOf("Tanggal", "Jenis", "Keterangan", "Akun", "Kategori", "Nominal Dilaporkan", "Nominal Real", "Selisih", "Catatan", "Status Bukti")
            .mapIndexed { index, label -> textCell(columnName(index), 1, label, style = 1) }
        transactions.forEachIndexed { index, row ->
            val r = index + 2
            rows += r to listOf(
                textCell("A", r, row.date),
                textCell("B", r, row.typeLabel),
                textCell("C", r, row.description),
                textCell("D", r, row.accountName),
                textCell("E", r, row.categoryName.orEmpty()),
                numberCell("F", r, row.reportedAmount),
                numberCell("G", r, row.realAmount),
                numberCell("H", r, row.saving),
                textCell("I", r, row.note.orEmpty()),
                textCell("J", r, row.receiptStatus)
            )
        }
        return worksheetXml(rows)
    }

    private fun worksheetXml(rows: List<Pair<Int, List<String>>>): String = buildString {
        append(XML_HEADER)
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        append("<cols><col min=\"1\" max=\"10\" width=\"18\" customWidth=\"1\"/></cols><sheetData>")
        rows.forEach { (rowNumber, cells) -> append("<row r=\"$rowNumber\">${cells.joinToString("")}</row>") }
        append("</sheetData></worksheet>")
    }

    private fun textCell(column: String, row: Int, value: String, style: Int = 0): String {
        val styleAttr = if (style > 0) " s=\"$style\"" else ""
        return "<c r=\"$column$row\" t=\"inlineStr\"$styleAttr><is><t>${value.xmlEscape()}</t></is></c>"
    }

    private fun numberCell(column: String, row: Int, value: Long): String = "<c r=\"$column$row\"><v>$value</v></c>"

    private fun columnName(index: Int): String = ('A'.code + index).toChar().toString()

    private fun ZipOutputStream.textEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.xmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
        formatter.maximumFractionDigits = 0
        return "Rp ${formatter.format(amount)}"
    }

    private fun contentTypesXml() = XML_HEADER + """
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml() = XML_HEADER + """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml() = XML_HEADER + """
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Ringkasan" sheetId="1" r:id="rId1"/>
            <sheet name="Transaksi" sheetId="2" r:id="rId2"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml() = XML_HEADER + """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun stylesXml() = XML_HEADER + """
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
          <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
          <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
        </styleSheet>
    """.trimIndent()

    private companion object {
        const val PDF_MIME = "application/pdf"
        const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    }
}
