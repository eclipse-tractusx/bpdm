package com.catenax.gpdm.adapter.cdq

import com.catenax.gpdm.adapter.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.entity.ConfigurationEntry
import com.catenax.gpdm.repository.ConfigurationEntryRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class PartnerImportService (
    val adapterProperties: CdqAdapterConfigProperties,
    val partnerImportPageService: PartnerImportPageService,
    val configurationEntryRepository: ConfigurationEntryRepository
        ){

    fun import(): Collection<BusinessPartnerResponse>{
        val lastUpdateEntry = getLastUpdateEntry()
        val modifiedAfterDate = OffsetDateTime.parse(lastUpdateEntry.value)

        var startAfter: String? = null
        var totalCollection: Collection<BusinessPartnerResponse> = emptyList()

        do{
            val response = partnerImportPageService.import(modifiedAfterDate, startAfter)
            totalCollection = totalCollection.plus(response.partners)
            startAfter = response.startAfter
        }while(startAfter != null)

        saveLastUpdateEntry(lastUpdateEntry)

        return totalCollection
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