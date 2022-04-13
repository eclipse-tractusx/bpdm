package com.catenax.gpdm.component.elastic.impl.service

import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.config.ElasticSearchConfigProperties
import com.catenax.gpdm.entity.BaseEntity
import com.catenax.gpdm.entity.SyncType
import com.catenax.gpdm.service.SyncRecordService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.EntityManager

/**
 * Provides functionality for managing the Elasticsearch index
 */
@Service
class ElasticSyncService(
    val elasticSyncPageService: ElasticSyncPageService,
    val configProperties: ElasticSearchConfigProperties,
    val entityManager: EntityManager,
    private val syncRecordService: SyncRecordService
) {

    /**
     * Asynchronous version of [exportPaginated]
     */
    @Async
    fun exportPaginatedAsync(fromTime: OffsetDateTime, saveState: String?) {
        exportPaginated(fromTime, saveState)
    }

    /**
     * Export new changes of the business partner records to the Elasticsearch index
     *
     * A new change is discovered by comparing the updated timestamp of the business partner record with the time of the last export
     */
    fun exportPaginated(fromTime: OffsetDateTime, saveState: String?) {
        var page = saveState?.toIntOrNull() ?: 0
        val fromTimeDate = Date.from(fromTime.toInstant())
        var docsPage: Page<BusinessPartnerDoc>

        do {
            try {
                val pageRequest = PageRequest.of(page, configProperties.exportPageSize, Sort.by(BaseEntity::updatedAt.name).ascending())
                docsPage = elasticSyncPageService.exportPartnersToElastic(fromTimeDate, pageRequest)
                page++
                val record = syncRecordService.getOrCreateRecord(SyncType.ELASTIC)
                val newCount = record.count + docsPage.content.size
                syncRecordService.setProgress(record, newCount, newCount.toFloat() / docsPage.totalElements)

                //Clear session after each page import to improve JPA performance
                entityManager.clear()

            } catch (exception: RuntimeException) {
                syncRecordService.setSynchronizationError(syncRecordService.getOrCreateRecord(SyncType.ELASTIC), exception.message!!, page.toString())
                throw exception
            }
        } while (docsPage.totalPages > page)

        syncRecordService.setSynchronizationSuccess(syncRecordService.getOrCreateRecord(SyncType.ELASTIC))
    }
}