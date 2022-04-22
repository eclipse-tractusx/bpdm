package com.catenax.gpdm.component.cdq

import com.catenax.gpdm.component.cdq.config.CdqAdapterConfigProperties
import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.TypeKeyNameCdq
import com.catenax.gpdm.component.cdq.dto.TypeKeyNameUrlCdq
import com.catenax.gpdm.component.cdq.service.CdqRequestMappingService
import com.catenax.gpdm.component.cdq.service.PartnerImportPageService
import com.catenax.gpdm.repository.ConfigurationEntryRepository
import com.catenax.gpdm.service.BusinessPartnerBuildService
import com.catenax.gpdm.service.BusinessPartnerFetchService
import com.catenax.gpdm.service.MetadataService
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient


@EnableAsync
@Configuration
@ComponentScan
@ConfigurationPropertiesScan
class CdqAdapterConfig(
    val adapterProperties: CdqAdapterConfigProperties,
    val cdqIdProperties: CdqIdentifierConfigProperties,
) {

    companion object{
        const val memorySize = 1 * 1024 * 1024 // 1mb
    }

    @Bean
    fun adapterClient(): WebClient{
        return WebClient.builder()
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs { codecs: ClientCodecConfigurer -> codecs.defaultCodecs().maxInMemorySize(memorySize) }
                .build())
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
        businessPartnerFetchService: BusinessPartnerFetchService,
        businessPartnerBuildService: BusinessPartnerBuildService,
    ): PartnerImportPageService {
        return PartnerImportPageService(
            webClient,
            adapterProperties,
            cdqIdConfigProperties,
            metadataService,
            mappingService,
            businessPartnerFetchService,
            createCdqIdentifierType(),
            createCdqImportedStatus(),
            createCdqIdentifierIssuer(),
            businessPartnerBuildService
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