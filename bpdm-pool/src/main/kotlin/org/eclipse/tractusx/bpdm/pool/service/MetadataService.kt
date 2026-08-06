/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.IBaseLegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.IBaseLogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityInvariantHeaderMetadataDto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.springframework.stereotype.Service

/**
 * Fetches the metadata entities that legal entity and address requests reference.
 */
@Service
class MetadataService(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val legalFormRepository: LegalFormRepository,
    private val regionRepository: RegionRepository
) {

    fun getMetadata(requests: Collection<IBaseLegalEntityDto>): LegalEntityInvariantHeaderMetadataDto {
        val idTypeKeys = requests.flatMap { it.identifiers }.map { it.type }.toSet()
        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.LEGAL_ENTITY, idTypeKeys)

        val legalFormKeys = requests.mapNotNull { it.legalForm }.toSet()
        val legalForms = legalFormRepository.findByTechnicalKeyIn(legalFormKeys)

        return LegalEntityInvariantHeaderMetadataDto(idTypes, legalForms)
    }

    fun getRegions(requests: Collection<IBaseLogisticAddressDto>): Set<RegionDb> {

        val regionKeys = requests.mapNotNull { it.physicalPostalAddress?.administrativeAreaLevel1 }
            .plus(requests.mapNotNull { it.alternativePostalAddress?.administrativeAreaLevel1 })
            .toSet()
        val regions = regionRepository.findByRegionCodeIn(regionKeys)
        return regions
    }

    fun getIdentifiers(requests: Collection<IBaseLogisticAddressDto>): Set<IdentifierTypeDb> {
        val idTypeKeys = requests.flatMap { it.identifiers }.map { it.type }.toSet()
        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.ADDRESS, idTypeKeys)
        return idTypes
    }

}