package com.murmli.webpursuer.data

import kotlinx.coroutines.flow.Flow

class GeneratedReportRepository(private val generatedReportDao: GeneratedReportDao) {
    fun getReportsFor(reportId: Int): Flow<List<GeneratedReport>> =
            generatedReportDao.getAllForReport(reportId)

    suspend fun getReport(id: Int): GeneratedReport? = generatedReportDao.getById(id)

    suspend fun delete(report: GeneratedReport) = generatedReportDao.delete(report)
}
