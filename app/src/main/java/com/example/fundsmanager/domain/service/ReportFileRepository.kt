package com.example.fundsmanager.domain.service

import com.example.fundsmanager.domain.model.ProjectReportData
import java.io.File

data class ReportFile(
    val file: File,
    val mimeType: String,
    val chooserTitle: String
)

interface ReportFileRepository {
    suspend fun createPdf(reportData: ProjectReportData): ReportFile
    suspend fun createExcel(reportData: ProjectReportData): ReportFile
}
