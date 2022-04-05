package com.catenax.gpdm.component.cdq.service

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.component.cdq.dto.ImportResponse
import com.catenax.gpdm.entity.ConfigurationEntry
import com.catenax.gpdm.repository.entity.ConfigurationEntryRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.persistence.EntityManager

@Service
class PartnerImportService(
    val adapterProperties: CdqAdapterConfigProperties,
    val partnerImportPageService: PartnerImportPageService,
    val configurationEntryRepository: ConfigurationEntryRepository,
    val entityManager: EntityManager
) {

    fun import(): ImportResponse {
        val lastUpdateEntry = getLastUpdateEntry()
        val modifiedAfterDate = OffsetDateTime.parse(lastUpdateEntry.value)

        var startAfter: String? = null
        var totalCollection: Collection<String> = emptyList()

        do {
            val response = partnerImportPageService.import(modifiedAfterDate, startAfter)
            totalCollection = totalCollection.plus(response.partners.map { it.bpn })
            startAfter = response.nextStartAfter

            //Clear session after each page import to improve JPA performance
            entityManager.clear()
        } while (startAfter != null)

        saveLastUpdateEntry(lastUpdateEntry)

        return ImportResponse(totalCollection.size, totalCollection)
    }

    private fun saveLastUpdateEntry(entry: ConfigurationEntry){
        entry.value = DateTimeFormatter.ISO_INSTANT.format(OffsetDateTime.now(ZoneOffset.UTC))
        configurationEntryRepository.save(entry)
    }

    private fun getLastUpdateEntry(): ConfigurationEntry{
        return configurationEntryRepository.findByKey(adapterProperties.timestampKey)?: run {
            val newEntry = ConfigurationEntry(adapterProperties.timestampKey,
                DateTimeFormatter.ISO_INSTANT.format( OffsetDateTime.of(LocalDateTime.of(2000, Month.JANUARY,1, 0, 0), ZoneOffset.UTC)))
            configurationEntryRepository.save(newEntry)
        }
    }

}