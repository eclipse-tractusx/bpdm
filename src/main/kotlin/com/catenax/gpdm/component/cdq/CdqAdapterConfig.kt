package com.catenax.gpdm.component.cdq

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.TypeKeyNameCdq
import com.catenax.gpdm.component.cdq.dto.TypeKeyNameUrlCdq
import com.catenax.gpdm.component.cdq.service.CdqRequestMappingService
import com.catenax.gpdm.component.cdq.service.PartnerImportPageService
import com.catenax.gpdm.repository.entity.ConfigurationEntryRepository
import com.catenax.gpdm.service.BusinessPartnerService
import com.catenax.gpdm.service.MetadataService
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ComponentScan
@ConfigurationPropertiesScan
class CdqAdapterConfig(
    val adapterProperties: CdqAdapterConfigProperties,
    val cdqIdProperties: CdqIdentifierConfigProperties,
) {
    @Bean
    fun adapterClient(): WebClient{
        return WebClient.builder()
            .baseUrl("${adapterProperties.host}/${adapterProperties.api}/storages/${adapterProperties.storage}")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("x-api-key", adapterProperties.apiKey)
            .build()
    }

    @Bean
    fun importService(
        webClient: WebClient,
        configurationEntryRepository: ConfigurationEntryRepository,
        adapterProperties: CdqAdapterConfigProperties,
        cdqIdConfigProperties: CdqIdentifierConfigProperties,
        metadataService: MetadataService,
        mappingService: CdqRequestMappingService,
        businessPartnerService: BusinessPartnerService
    ): PartnerImportPageService {
        return PartnerImportPageService(
            webClient,
            adapterProperties,
            cdqIdConfigProperties,
            metadataService,
            mappingService,
            businessPartnerService,
            createCdqIdentifierType(),
            createCdqImportedStatus(),
            createCdqIdentifierIssuer()
        )
    }

    private fun createCdqIdentifierType(): TypeKeyNameUrlCdq {
        return TypeKeyNameUrlCdq(cdqIdProperties.typeKey, cdqIdProperties.typeName, "")
    }

    private fun createCdqIdentifierIssuer(): TypeKeyNameUrlCdq {
        return TypeKeyNameUrlCdq(cdqIdProperties.issuerKey, cdqIdProperties.issuerName, "")
    }

    private fun createCdqImportedStatus(): TypeKeyNameCdq {
        return TypeKeyNameCdq(cdqIdProperties.statusImportedKey, cdqIdProperties.statusImportedName)
    }
}