/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.ImportResponsePage
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.MetadataService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class PartnerImportPageService(
    private val webClient: WebClient,
    private val adapterProperties: CdqAdapterConfigProperties,
    cdqIdConfigProperties: CdqIdentifierConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: CdqToRequestMapper,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val cdqIdentifierType = TypeKeyNameUrlCdq(cdqIdConfigProperties.typeKey, cdqIdConfigProperties.typeName, "")
    private val cdqIdentifierStatus = TypeKeyNameCdq(cdqIdConfigProperties.statusImportedKey, cdqIdConfigProperties.statusImportedName)
    private val cdqIssuer = TypeKeyNameUrlCdq(cdqIdConfigProperties.issuerKey, cdqIdConfigProperties.issuerName, "")

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun import(modifiedAfter: Instant, startAfter: String?): ImportResponsePage {
        logger.debug { "Import new business partner starting after ID '$startAfter'" }

        val partnerCollection = webClient
            .get()
            .uri { builder ->
                builder
                    .path("/businesspartners")
                    .queryParam("modifiedAfter", toModifiedAfterFormat(modifiedAfter))
                    .queryParam("limit", adapterProperties.importLimit)
                    .queryParam("datasource", adapterProperties.datasource)
                    .queryParam("featuresOn", "USE_NEXT_START_AFTER")
                if (startAfter != null) builder.queryParam("startAfter", startAfter)
                builder.build()
            }
            .retrieve()
            .bodyToMono<PagedResponseCdq<BusinessPartnerCdq>>()
            .block()!!

        logger.debug { "Received ${partnerCollection.values.size} to import from CDQ" }

        addNewMetadata(partnerCollection.values)

        val partnersToUpsert = partnerCollection.values.map {
            it.copy(
                identifiers = it.identifiers.plus(
                    IdentifierCdq(
                        cdqIdentifierType,
                        it.id!!,
                        cdqIssuer,
                        cdqIdentifierStatus
                    )
                )
            )
        }

        val (hasBpn, hasNoBpn) = partnersToUpsert.partition { it.identifiers.any { id -> id.type?.technicalKey == bpnConfigProperties.id } }
        val legalEntitiesToCreate = hasNoBpn.map { mappingService.toCreateRequest(it) }
        val legalEntitiesToUpdate = hasBpn.map { mappingService.toUpdateRequest(it) }

        val createdLegalEntities = businessPartnerBuildService.createLegalEntities(legalEntitiesToCreate)
        val updatedLegalEntities = businessPartnerBuildService.updateLegalEntities(legalEntitiesToUpdate)

        return ImportResponsePage(
            partnerCollection.total,
            partnerCollection.nextStartAfter,
            createdLegalEntities + updatedLegalEntities
        )
    }

    private fun addNewMetadata(partners: Collection<BusinessPartnerCdq>){
        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.status?.technicalKey == null) null else id.status } }
            .plus(cdqIdentifierStatus)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierStati(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierStatus(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.type?.technicalKey == null) null else id.type } }
            .plus(cdqIdentifierType)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierTypes(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierType(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.issuingBody?.technicalKey == null) null else id.issuingBody } }
            .plus(cdqIssuer)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIssuingBodies(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIssuingBody(it) }

        partners
            .filter { it.legalForm?.technicalKey != null }
            .map { it.legalForm!! to it }
            .associateBy { it.first.technicalKey }
            .minus(metadataService.getLegalForms(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it.first, it.second) }
            .forEach { metadataService.createLegalForm(it) }
    }


    private fun toModifiedAfterFormat(dateTime: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(dateTime)
    }


}