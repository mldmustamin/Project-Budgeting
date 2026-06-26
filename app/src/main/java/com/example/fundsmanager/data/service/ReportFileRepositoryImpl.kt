package com.example.fundsmanager.data.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
import java.time.Instant
import java.time.ZoneId
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
        val footerHeight = 26f
        val pageBottom = pageHeight - margin - footerHeight
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = 0xFF202124.toInt() }
        val titlePaint = Paint(paint).apply { textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val sectionPaint = Paint(paint).apply { textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val headerPaint = Paint(paint).apply { textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFFFFFFFF.toInt() }
        val subInfoPaint = Paint(paint).apply { textSize = 8.5f; color = 0xFF5F6368.toInt() }
        val bodyPaint = Paint(paint).apply { textSize = 8.2f }
        val secondaryBodyPaint = Paint(subInfoPaint).apply { textSize = 8f }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f; color = 0xFFB8C2CC.toInt() }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val tableColumns = listOf(
            PdfColumn("Tanggal", 62f),
            PdfColumn("Jenis", 76f),
            PdfColumn("Keterangan", 184f),
            PdfColumn("Akun", 98f),
            PdfColumn("Kategori", 94f),
            PdfColumn("Dilaporkan", 88f, alignRight = true),
            PdfColumn("Real", 88f, alignRight = true),
            PdfColumn("Selisih", 88f, alignRight = true)
        )
        val tableWidth = tableColumns.sumOf { it.width.toDouble() }.toFloat()
        val tablePaddingX = 6f
        val tablePaddingY = 6f
        val tableLineHeight = 10.5f
        val headerLineHeight = 10f
        val transactionHeaderHeight = 30f
        val detailLabelWidth = 92f
        var pageNumber = 0
        var page = newPage(document, pageWidth, pageHeight, ++pageNumber)
        var canvas = page.canvas
        var y = margin
        var transactionTableStarted = false

        fun finishPage() {
            drawFooter(canvas, paint, pageWidth, pageHeight, pageNumber)
            document.finishPage(page)
        }

        fun ensureSpace(required: Float) {
            if (y + required <= pageBottom) return
            finishPage()
            page = newPage(document, pageWidth, pageHeight, ++pageNumber)
            canvas = page.canvas
            y = margin
            if (transactionTableStarted) {
                canvas.drawText("Transaksi", margin, y, sectionPaint)
                y += 12f
                drawPdfTableHeader(
                    canvas = canvas,
                    x = margin,
                    y = y,
                    columns = tableColumns,
                    tableWidth = tableWidth,
                    lineHeight = headerLineHeight,
                    textPaint = headerPaint,
                    fillPaint = fillPaint,
                    borderPaint = borderPaint,
                    paddingX = tablePaddingX,
                    paddingY = tablePaddingY
                )
                y += transactionHeaderHeight
            }
        }

        canvas.drawText("Funds Manager", margin, y, titlePaint)
        y += 20f
        canvas.drawText("Laporan Project", margin, y, sectionPaint)
        y += lineHeight
        canvas.drawText("Project: ${reportData.project.name}", margin, y, paint)
        y += lineHeight
        canvas.drawText("Periode Project: ${formatProjectPeriod(reportData.project.startAt, reportData.project.completedAt)}", margin, y, subInfoPaint)
        y += lineHeight
        canvas.drawText("Tanggal Export: ${reportData.exportedAt}", margin, y, paint)
        y += lineHeight
        canvas.drawText("Jumlah Transaksi: ${reportData.transactions.size}", margin, y, subInfoPaint)
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
            ensureSpace(42f)
            row.forEachIndexed { index, item ->
                val cardX = margin + index * 250f
                fillPaint.color = 0xFFF7FBF8.toInt()
                canvas.drawRoundRect(cardX, y - 12f, cardX + 225f, y + 18f, 8f, 8f, fillPaint)
                canvas.drawRoundRect(cardX, y - 12f, cardX + 225f, y + 18f, 8f, 8f, borderPaint)
                canvas.drawText(item.first, cardX + 8f, y, subInfoPaint)
                drawRightText(canvas, formatRupiah(item.second), cardX + 217f, y + 14f, paint)
            }
            y += 36f
        }
        y += 10f

        canvas.drawText("Transaksi", margin, y, sectionPaint)
        y += 12f
        drawPdfTableHeader(
            canvas = canvas,
            x = margin,
            y = y,
            columns = tableColumns,
            tableWidth = tableWidth,
            lineHeight = headerLineHeight,
            textPaint = headerPaint,
            fillPaint = fillPaint,
            borderPaint = borderPaint,
            paddingX = tablePaddingX,
            paddingY = tablePaddingY
        )
        y += transactionHeaderHeight
        transactionTableStarted = true

        if (reportData.transactions.isEmpty()) {
            val emptyHeight = 28f
            ensureSpace(emptyHeight)
            fillPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRect(margin, y, margin + tableWidth, y + emptyHeight, fillPaint)
            canvas.drawRect(margin, y, margin + tableWidth, y + emptyHeight, borderPaint)
            canvas.drawText("Belum ada transaksi.", margin + tablePaddingX, y + 17f, bodyPaint)
            y += emptyHeight
        } else {
            reportData.transactions.forEachIndexed { index, row ->
                val cellLines = listOf(
                    wrapText(row.date, bodyPaint, tableColumns[0].width - (tablePaddingX * 2)),
                    wrapText(row.typeLabel, bodyPaint, tableColumns[1].width - (tablePaddingX * 2)),
                    wrapText(row.description, bodyPaint, tableColumns[2].width - (tablePaddingX * 2)),
                    wrapText(row.accountName.ifBlank { "-" }, bodyPaint, tableColumns[3].width - (tablePaddingX * 2)),
                    wrapText(row.categoryName ?: "-", bodyPaint, tableColumns[4].width - (tablePaddingX * 2)),
                    wrapText(formatRupiah(row.reportedAmount), bodyPaint, tableColumns[5].width - (tablePaddingX * 2)),
                    wrapText(formatRupiah(row.realAmount), bodyPaint, tableColumns[6].width - (tablePaddingX * 2)),
                    wrapText(formatRupiah(row.saving), bodyPaint, tableColumns[7].width - (tablePaddingX * 2))
                )
                val detailLines = wrapText(
                    buildReceiptDetail(row),
                    secondaryBodyPaint,
                    tableWidth - detailLabelWidth - (tablePaddingX * 3)
                )
                val mainRowHeight = maxOf(34f, (cellLines.maxOf { it.size } * tableLineHeight) + (tablePaddingY * 2))
                val detailRowHeight = maxOf(24f, (detailLines.size * tableLineHeight) + (tablePaddingY * 2))
                val rowHeight = mainRowHeight + detailRowHeight
                ensureSpace(rowHeight)
                fillPaint.color = if (index % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF6F8FA.toInt()
                drawPdfTableRow(
                    canvas = canvas,
                    rowArea = RectF(margin, y, margin + tableWidth, y + mainRowHeight),
                    columns = tableColumns,
                    linesByColumn = cellLines,
                    bodyPaint = bodyPaint,
                    fillPaint = fillPaint,
                    borderPaint = borderPaint,
                    paddingX = tablePaddingX,
                    paddingY = tablePaddingY,
                    lineHeight = tableLineHeight
                )
                drawPdfDetailRow(
                    canvas = canvas,
                    rowArea = RectF(margin, y + mainRowHeight, margin + tableWidth, y + rowHeight),
                    label = "Catatan / Bukti",
                    lines = detailLines,
                    labelPaint = bodyPaint,
                    textPaint = secondaryBodyPaint,
                    fillPaint = fillPaint,
                    borderPaint = borderPaint,
                    paddingX = tablePaddingX,
                    paddingY = tablePaddingY,
                    lineHeight = tableLineHeight,
                    labelWidth = detailLabelWidth
                )
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

    private fun drawPdfTableHeader(
        canvas: Canvas,
        x: Float,
        y: Float,
        columns: List<PdfColumn>,
        tableWidth: Float,
        lineHeight: Float,
        textPaint: Paint,
        fillPaint: Paint,
        borderPaint: Paint,
        paddingX: Float,
        paddingY: Float
    ) {
        fillPaint.color = 0xFF1F4E79.toInt()
        val headerTop = y
        val headerBottom = y + 30f
        canvas.drawRect(x, headerTop, x + tableWidth, headerBottom, fillPaint)
        canvas.drawRect(x, headerTop, x + tableWidth, headerBottom, borderPaint)
        var currentX = x
        columns.forEach { column ->
            val cellLeft = currentX
            val cellRight = currentX + column.width
            canvas.drawLine(cellLeft, headerTop, cellLeft, headerBottom, borderPaint)
            val lines = wrapText(column.label, textPaint, column.width - (paddingX * 2))
            val baseY = headerTop + paddingY + textBaselineOffset(textPaint)
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, cellLeft + paddingX, baseY + (index * lineHeight), textPaint)
            }
            currentX = cellRight
        }
        canvas.drawLine(x + tableWidth, headerTop, x + tableWidth, headerBottom, borderPaint)
    }

    private fun drawPdfTableRow(
        canvas: Canvas,
        rowArea: RectF,
        columns: List<PdfColumn>,
        linesByColumn: List<List<String>>,
        bodyPaint: Paint,
        fillPaint: Paint,
        borderPaint: Paint,
        paddingX: Float,
        paddingY: Float,
        lineHeight: Float
    ) {
        canvas.drawRect(rowArea, fillPaint)
        canvas.drawRect(rowArea, borderPaint)
        var currentX = rowArea.left
        columns.forEachIndexed { index, column ->
            val cellLeft = currentX
            val cellRight = currentX + column.width
            canvas.drawLine(cellLeft, rowArea.top, cellLeft, rowArea.bottom, borderPaint)
            val lines = linesByColumn[index]
            val baseY = rowArea.top + paddingY + textBaselineOffset(bodyPaint)
            lines.forEachIndexed { lineIndex, line ->
                val textY = baseY + (lineIndex * lineHeight)
                if (column.alignRight) {
                    drawRightText(canvas, line, cellRight - paddingX, textY, bodyPaint)
                } else {
                    canvas.drawText(line, cellLeft + paddingX, textY, bodyPaint)
                }
            }
            currentX = cellRight
        }
        canvas.drawLine(rowArea.right, rowArea.top, rowArea.right, rowArea.bottom, borderPaint)
    }

    private fun drawPdfDetailRow(
        canvas: Canvas,
        rowArea: RectF,
        label: String,
        lines: List<String>,
        labelPaint: Paint,
        textPaint: Paint,
        fillPaint: Paint,
        borderPaint: Paint,
        paddingX: Float,
        paddingY: Float,
        lineHeight: Float,
        labelWidth: Float
    ) {
        canvas.drawRect(rowArea, fillPaint)
        canvas.drawRect(rowArea, borderPaint)
        val labelRight = rowArea.left + labelWidth
        canvas.drawLine(labelRight, rowArea.top, labelRight, rowArea.bottom, borderPaint)
        val labelY = rowArea.top + paddingY + textBaselineOffset(labelPaint)
        canvas.drawText(label, rowArea.left + paddingX, labelY, labelPaint)
        val textBaseY = rowArea.top + paddingY + textBaselineOffset(textPaint)
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, labelRight + paddingX, textBaseY + (index * lineHeight), textPaint)
        }
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
        text.split('\n').forEach { paragraph ->
            if (paragraph.isBlank()) {
                lines += "-"
            } else {
                var current = ""
                paragraph.split(Regex("\\s+")).forEach { word ->
                    val fragments = splitLongWord(word, paint, maxWidth)
                    fragments.forEach { fragment ->
                        val candidate = if (current.isBlank()) fragment else "$current $fragment"
                        if (paint.measureText(candidate) <= maxWidth) {
                            current = candidate
                        } else {
                            if (current.isNotBlank()) lines += current
                            current = fragment
                        }
                    }
                }
                if (current.isNotBlank()) lines += current
            }
        }
        return lines.ifEmpty { listOf(text) }
    }

    private fun splitLongWord(word: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(word) <= maxWidth) return listOf(word)
        val chunks = mutableListOf<String>()
        var current = ""
        word.forEach { char ->
            val candidate = current + char
            if (current.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                chunks += current
                current = char.toString()
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) chunks += current
        return chunks
    }

    private fun buildReceiptDetail(row: TransactionReportRow): String {
        val note = row.note?.trim().orEmpty().ifBlank { "-" }
        val receipt = row.receiptStatus.trim().ifBlank { "Belum ada bukti" }
        return "Catatan: $note\nBukti: $receipt"
    }

    private fun textBaselineOffset(paint: Paint): Float = -paint.fontMetrics.ascent

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
            1 to listOf(textCell("A", 1, "Funds Manager", style = 2), textCell("B", 1, "Laporan Project", style = 2)),
            2 to listOf(textCell("A", 2, "Project", style = 1), textCell("B", 2, reportData.project.name)),
            3 to listOf(textCell("A", 3, "Periode Project", style = 1), textCell("B", 3, formatProjectPeriod(reportData.project.startAt, reportData.project.completedAt))),
            4 to listOf(textCell("A", 4, "Tanggal Export", style = 1), textCell("B", 4, reportData.exportedAt)),
            5 to listOf(textCell("A", 5, "Jumlah Transaksi", style = 1), numberCell("B", 5, reportData.transactions.size.toLong(), style = 4)),
            7 to listOf(textCell("A", 7, "Total Dana Masuk", style = 1), numberCell("B", 7, reportData.summary.totalFundIn, style = 3)),
            8 to listOf(textCell("A", 8, "Posisi Bersih", style = 1), numberCell("B", 8, reportData.summary.netPosition, style = 3)),
            9 to listOf(textCell("A", 9, "Dilaporkan ke Kantor", style = 1), numberCell("B", 9, reportData.summary.totalOfficeReported, style = 3)),
            10 to listOf(textCell("A", 10, "Pengeluaran Real", style = 1), numberCell("B", 10, reportData.summary.totalOfficeReal, style = 3)),
            11 to listOf(textCell("A", 11, "Expense Pribadi", style = 1), numberCell("B", 11, reportData.summary.totalPersonalExpense, style = 3)),
            12 to listOf(textCell("A", 12, "Total Keluar Real", style = 1), numberCell("B", 12, reportData.summary.totalCashOut, style = 3)),
            13 to listOf(textCell("A", 13, "Selisih / Hemat", style = 1), numberCell("B", 13, reportData.summary.saving, style = 3)),
            14 to listOf(textCell("A", 14, "Sisa Berdasarkan Laporan", style = 1), numberCell("B", 14, reportData.summary.remainingReported, style = 3)),
            15 to listOf(textCell("A", 15, "Sisa Real", style = 1), numberCell("B", 15, reportData.summary.remainingReal, style = 3)),
            17 to listOf(textCell("A", 17, "Generated by Funds Manager"), textCell("B", 17, "Offline-first local report"))
        )
        return worksheetXml(rows, listOf(
            1 to 26.0,
            2 to 26.0,
            3 to 18.0,
            4 to 18.0
        ))
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
                numberCell("F", r, row.reportedAmount, style = 3),
                numberCell("G", r, row.realAmount, style = 3),
                numberCell("H", r, row.saving, style = 3),
                textCell("I", r, row.note.orEmpty()),
                textCell("J", r, row.receiptStatus)
            )
        }
        return worksheetXml(
            rows,
            listOf(
                1 to 14.0,
                2 to 22.0,
                3 to 34.0,
                4 to 20.0,
                5 to 20.0,
                6 to 18.0,
                7 to 18.0,
                8 to 16.0,
                9 to 28.0,
                10 to 16.0
            )
        )
    }

    private fun worksheetXml(rows: List<Pair<Int, List<String>>>, columns: List<Pair<Int, Double>>): String = buildString {
        append(XML_HEADER)
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        append("<cols>")
        columns.forEach { (index, width) ->
            append("<col min=\"$index\" max=\"$index\" width=\"$width\" customWidth=\"1\"/>")
        }
        append("</cols><sheetData>")
        rows.forEach { (rowNumber, cells) -> append("<row r=\"$rowNumber\">${cells.joinToString("")}</row>") }
        append("</sheetData></worksheet>")
    }

    private fun textCell(column: String, row: Int, value: String, style: Int = 0): String {
        val styleAttr = if (style > 0) " s=\"$style\"" else ""
        return "<c r=\"$column$row\" t=\"inlineStr\"$styleAttr><is><t>${value.xmlEscape()}</t></is></c>"
    }

    private fun numberCell(column: String, row: Int, value: Long, style: Int = 0): String {
        val styleAttr = if (style > 0) " s=\"$style\"" else ""
        return "<c r=\"$column$row\"$styleAttr><v>$value</v></c>"
    }

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
          <numFmts count="1"><numFmt numFmtId="164" formatCode="&quot;Rp&quot; #,##0"/></numFmts>
          <fonts count="3">
            <font><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="14"/><name val="Calibri"/></font>
          </fonts>
          <fills count="3">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFEAF3FF"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFF7FBF8"/><bgColor indexed="64"/></patternFill></fill>
          </fills>
          <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="5">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="1" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
            <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="164" fontId="0" fillId="2" borderId="0" xfId="0" applyNumberFormat="1" applyFill="1"/>
            <xf numFmtId="0" fontId="0" fillId="2" borderId="0" xfId="0" applyFill="1"/>
          </cellXfs>
        </styleSheet>
    """.trimIndent()

    private fun formatProjectPeriod(startAt: Long, completedAt: Long?): String {
        val start = formatDate(startAt.takeIf { it > 0 })
        val end = formatDate(completedAt)
        return if (end == "-") "$start - Berjalan" else "$start - $end"
    }

    private fun formatDate(epochMillis: Long?): String {
        val value = epochMillis ?: return "-"
        return runCatching {
            Instant.ofEpochMilli(value)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrDefault("-")
    }

    private companion object {
        const val PDF_MIME = "application/pdf"
        const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    }
}

private data class PdfColumn(
    val label: String,
    val width: Float,
    val alignRight: Boolean = false
)
