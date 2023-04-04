/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.IdentifierLsaType
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.LogisticAddressDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmMultipleNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataMappingDto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.LegalForm
import org.eclipse.tractusx.bpdm.pool.entity.Region
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalFormRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.springframework.stereotype.Service

/**
 * Service for fetching and mapping metadata entities referenced by [LegalEntityDto]
 */
@Service
class MetadataMappingService(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val legalFormRepository: LegalFormRepository,
    private val regionRepository: RegionRepository
) {

    /**
     * Fetch metadata entities referenced in [partners] and map them by their referenced keys
     */
    fun mapRequests(partners: Collection<LegalEntityDto>): LegalEntityMetadataMappingDto {
        return LegalEntityMetadataMappingDto(
            idTypes = mapLegalEntityIdentifierTypes(partners),
            legalForms = mapLegalForms(partners)
        )
    }

    /**
     * Fetch metadata entities referenced in [partners] and map them by their referenced keys
     */
    fun mapRequests(partners: Collection<LogisticAddressDto>): AddressMetadataMappingDto {
        return AddressMetadataMappingDto(
            idTypes = mapAddressIdentifierTypes(partners),
            // TODO enable regionCodes later
//            regions = mapAddressRegions(partners)
            regions = mapOf()
        )
    }

    /**
     * Fetch [IdentifierType] referenced in [partners] and map them by their referenced keys
     */
    fun mapLegalEntityIdentifierTypes(partners: Collection<LegalEntityDto>): Map<String, IdentifierType>{
        val technicalKeys = partners.flatMap { it.identifiers.map { id -> id.type } }.toSet()
        return mapIdentifierTypes(IdentifierLsaType.LEGAL_ENTITY, technicalKeys)
    }

    /**
     * Fetch [IdentifierType] referenced in [partners] and map them by their referenced keys
     */
    fun mapAddressIdentifierTypes(partners: Collection<LogisticAddressDto>): Map<String, IdentifierType>{
        val technicalKeys = partners.flatMap { it.identifiers.map { id -> id.type } }.toSet()
        return mapIdentifierTypes(IdentifierLsaType.ADDRESS, technicalKeys)
    }

    /**
     * Fetch [LegalForm] referenced in [partners] and map them by their referenced keys
     */
    fun mapLegalForms(partners: Collection<LegalEntityDto>): Map<String, LegalForm>{
        return mapLegalForms(partners.mapNotNull { it.legalForm }.toSet())
    }

    fun mapAddressRegions(partners: Collection<LogisticAddressDto>): Map<String, Region> {
        val regionCodes = partners.mapNotNull { it.physicalPostalAddress.baseAddress.administrativeAreaLevel1 }
            .plus(partners.mapNotNull { it.alternativePostalAddress?.baseAddress?.administrativeAreaLevel1 })
            .toSet()

        return regionRepository.findByRegionCodeIn(regionCodes)
            .associateBy { it.regionCode }
            .also {
                assertKeysFound(regionCodes, it)
            }
    }


    private fun mapIdentifierTypes(lsaType: IdentifierLsaType, keys: Set<String>): Map<String, IdentifierType>{
        val typeMap = identifierTypeRepository.findByLsaTypeAndTechnicalKeyIn(lsaType, keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private fun mapLegalForms(keys: Set<String>): Map<String, LegalForm>{
        val typeMap = legalFormRepository.findByTechnicalKeyIn(keys).associateBy { it.technicalKey }
        assertKeysFound(keys, typeMap)
        return typeMap
    }

    private inline fun <reified T> assertKeysFound(keys: Set<String>, typeMap: Map<String, T>){
        val keysNotfound = keys.minus(typeMap.keys)
        if(keysNotfound.isNotEmpty()) throw BpdmMultipleNotFoundException(T::class.simpleName!!, keysNotfound )
    }

}