package com.catenax.gpdm.service

import com.catenax.gpdm.config.ElasticSearchConfigProperties
import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.entity.BaseEntity
import com.catenax.gpdm.entity.ConfigurationEntry
import com.catenax.gpdm.repository.entity.BusinessPartnerRepository
import com.catenax.gpdm.repository.entity.ConfigurationEntryRepository
import com.catenax.gpdm.repository.elastic.BusinessPartnerDocRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.time.*
import java.util.*

@Service
@ConditionalOnProperty(
    value = ["bpdm.elastic.enabled"],
    havingValue = "true",
    matchIfMissing = false)
class ElasticSyncService(
    val businessPartnerRepository: BusinessPartnerRepository,
    val businessPartnerDocRepository: BusinessPartnerDocRepository,
    val documentMappingService: DocumentMappingService,
    val configurationEntryRepository: ConfigurationEntryRepository,
    val configProperties: ElasticSearchConfigProperties
) {
    val formatter = SimpleDateFormat("d-MMM-yyyy,HH:mm:ss")

    @Transactional
    fun exportPartnersToElastic(): Set<BusinessPartnerDoc>{
        val allDocs: MutableSet<BusinessPartnerDoc> = mutableSetOf()
        val fromTime = getOrCreateTimestamp()
        var page = 0

        do{
            val pageRequest =  PageRequest.of(page, configProperties.exportPageSize, Sort.by(BaseEntity::updatedAt.name).ascending())
            val partnersToExport = businessPartnerRepository.findByUpdatedAtAfter(fromTime, pageRequest)
            val createdDocs = businessPartnerDocRepository.saveAll(partnersToExport.map { documentMappingService.toDocument(it) }).toSet()

            page++
            allDocs += createdDocs
        }while(partnersToExport.totalPages > page)

        setTimestamp(Date.from(Instant.now()))

        return allDocs
    }

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