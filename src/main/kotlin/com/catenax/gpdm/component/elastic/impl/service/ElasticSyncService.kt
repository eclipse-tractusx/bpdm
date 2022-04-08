package com.catenax.gpdm.component.elastic.impl.service

import com.catenax.gpdm.component.elastic.impl.repository.BusinessPartnerDocRepository
import com.catenax.gpdm.config.ElasticSearchConfigProperties
import com.catenax.gpdm.dto.elastic.ExportResponse
import com.catenax.gpdm.entity.BaseEntity
import com.catenax.gpdm.entity.ConfigurationEntry
import com.catenax.gpdm.repository.ConfigurationEntryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.persistence.EntityManager

/**
 * Provides functionality for managing the Elasticsearch index
 */
@Service
class ElasticSyncService(
    val elasticSyncPageService: ElasticSyncPageService,
    val businessPartnerDocRepository: BusinessPartnerDocRepository,
    val configurationEntryRepository: ConfigurationEntryRepository,
    val configProperties: ElasticSearchConfigProperties,
    val entityManager: EntityManager
) {
    val formatter = SimpleDateFormat("d-MMM-yyyy,HH:mm:ss:SSS")

    /**
     * Export new changes of the business partner records to the Elasticsearch index
     *
     * A new change is discovered by comparing the updated timestamp of the business partner record with the time of the last export
     */
    fun exportPartnersToElastic(): ExportResponse {
        val exportedBpns: MutableSet<String> = mutableSetOf()
        val fromTime = getOrCreateTimestamp()
        var page = 0

        val importTime = Instant.now()

        do {
            val pageRequest = PageRequest.of(page, configProperties.exportPageSize, Sort.by(BaseEntity::updatedAt.name).ascending())
            val docsPage = elasticSyncPageService.exportPartnersToElastic(fromTime, pageRequest)
            page++
            exportedBpns += docsPage.map { it.bpn }

            //Clear session after each page import to improve JPA performance
            entityManager.clear()
        } while (docsPage.totalPages > page)

        setTimestamp(Date.from(importTime))

        return ExportResponse(exportedBpns.size, exportedBpns)
    }

    /**
     * Clears the whole index and resets the time of the last update
     */
    @Transactional
    fun clearElastic(){
        businessPartnerDocRepository.deleteAll()
        setTimestamp(Date(0))
    }



    private  fun getOrCreateTimestamp(): Date{
        return formatter.parse(getOrCreateEntry().value)
    }

    private fun setTimestamp(time: Date){
        val entry = getOrCreateEntry()
        entry.value = createTimestampString(time)
        configurationEntryRepository.save(entry)
    }

    private fun getOrCreateEntry(): ConfigurationEntry{
        return configurationEntryRepository.findByKey(configProperties.exportTimeKey)?: run {
            val newEntry = ConfigurationEntry(configProperties.exportTimeKey, createTimestampString(Date(0)))
            configurationEntryRepository.save(newEntry)
        }
    }

    private fun createTimestampString(timestamp: Date): String{
        return formatter.format(timestamp)
    }
}