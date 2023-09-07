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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.LogisticAddressDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.dto.AddressMetadataDto
import org.eclipse.tractusx.bpdm.pool.dto.LegalEntityMetadataDto
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.springframework.stereotype.Service

@Service
class RequestValidationService(
    private val legalEntityRepository: LegalEntityRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository,
    private val bpnIssuingService: BpnIssuingService,
    private val siteRepository: SiteRepository,
    private val addressRepository: LogisticAddressRepository,
    private val metadataService: MetadataService
) {
    fun validateLegalEntityCreates(
        requests: Collection<LegalEntityPartnerCreateRequest>
    ): Map<LegalEntityPartnerCreateRequest, Collection<ErrorInfo<LegalEntityCreateError>>> {

        val legalEntityRequests = requests.map { it.legalEntity }
        val legalAddressRequests = requests.map { it.legalAddress }

        val legalEntityMetadata = metadataService.getMetadata(legalEntityRequests).toKeys()
        val addressMetadata = metadataService.getMetadata(legalAddressRequests).toKeys()
        val duplicateIdentifierCandidates = getDuplicateLegalEntityCandidates(legalEntityRequests)
        val duplicateIdentifiers = findDuplicateLegalEntityIdentifiers(legalEntityRequests)
        return requests.flatMap { request ->
            val legalEntity = request.legalEntity
            val legalAddress = request.legalAddress

            val validationErrors =
                validateLegalFormExists(legalEntity, legalEntityMetadata.legalForms, LegalEntityCreateError.LegalFormNotFound, request.index) +
                        validateIdentifierTypesExists(
                            legalEntity,
                            legalEntityMetadata.idTypes,
                            LegalEntityCreateError.LegalEntityIdentifierNotFound,
                            request.index
                        ) +
                        validateRegionExists(legalAddress, addressMetadata.regions, LegalEntityCreateError.LegalAddressRegionNotFound, request.index) +
                        validateIdentifierTypeExists(
                            legalAddress,
                            addressMetadata.idTypes,
                            LegalEntityCreateError.LegalAddressIdentifierNotFound,
                            request.index
                        ) +
                        validateIdentifierNotExist(
                            legalEntity,
                            duplicateIdentifierCandidates,
                            null,
                            LegalEntityCreateError.LegalEntityDuplicateIdentifier,
                            request.index
                        ) +
                        validateDuplicates(legalEntity,duplicateIdentifiers,request.index)
            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateDuplicates(legalEntity: LegalEntityDto,duplicateIdentifiers: Set<LegalEntityIdentifierDto>, entityKey: String?) : Collection<ErrorInfo<LegalEntityCreateError>> {
        val errorList = mutableListOf<ErrorInfo<LegalEntityCreateError>>()
        duplicateIdentifiers.forEach { duplicate ->
            if (legalEntity.identifiers.contains(duplicate)) {
                val error = ErrorInfo(
                    LegalEntityCreateError.LegalEntityDuplicateIdentifier,
                    "Identifier $duplicate is duplicated among legal entities in the request",
                    entityKey
                )
                errorList.add(error)
            }
        }

        return errorList
    }
    fun validateLegalEntityUpdates(
        requests: Collection<LegalEntityPartnerUpdateRequest>
    ): Map<LegalEntityPartnerUpdateRequest, Collection<ErrorInfo<LegalEntityUpdateError>>> {

        val legalEntityRequests = requests.map { it.legalEntity }
        val legalAddressRequests = requests.map { it.legalAddress }

        val legalEntityMetadata = metadataService.getMetadata(legalEntityRequests).toKeys()
        val addressMetadata = metadataService.getMetadata(legalAddressRequests).toKeys()

        val duplicateIdentifierCandidates = getDuplicateLegalEntityCandidates(requests.map { it.legalEntity })
        val existingLegalEntityBpns = legalEntityRepository.findDistinctByBpnIn(requests.map { it.bpnl }).map { it.bpn }.toSet()

        return requests.flatMap { request ->
            val legalEntity = request.legalEntity
            val legalAddress = request.legalAddress

            val validationErrors =
                validateLegalFormExists(legalEntity, legalEntityMetadata.legalForms, LegalEntityUpdateError.LegalFormNotFound, request.bpnl) +
                        validateIdentifierTypesExists(
                            legalEntity,
                            legalEntityMetadata.idTypes,
                            LegalEntityUpdateError.LegalEntityIdentifierNotFound,
                            request.bpnl
                        ) +
                        validateRegionExists(legalAddress, addressMetadata.regions, LegalEntityUpdateError.LegalAddressRegionNotFound, request.bpnl) +
                        validateIdentifierTypeExists(
                            legalAddress,
                            addressMetadata.idTypes,
                            LegalEntityUpdateError.LegalAddressIdentifierNotFound,
                            request.bpnl
                        ) +
                        validateIdentifierNotExist(
                            legalEntity,
                            duplicateIdentifierCandidates,
                            request.bpnl,
                            LegalEntityUpdateError.LegalEntityDuplicateIdentifier,
                            request.bpnl
                        ) +
                        validateUpdateBpnExists(request.bpnl, existingLegalEntityBpns, LegalEntityUpdateError.LegalEntityNotFound)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateSiteCreates(
        requests: Collection<SitePartnerCreateRequest>,
    ): Map<SitePartnerCreateRequest, Collection<ErrorInfo<SiteCreateError>>> {

        val mainAddressRequests = requests.map { it.site.mainAddress }
        val addressMetadata = metadataService.getMetadata(mainAddressRequests).toKeys()

        val requestedParentBpns = requests.map { it.bpnlParent }
        val existingParentBpns = legalEntityRepository.findDistinctByBpnIn(requestedParentBpns).map { it.bpn }.toSet()

        return requests.flatMap { request ->
            val mainAddress = request.site.mainAddress

            val validationErrors = validateRegionExists(mainAddress, addressMetadata.regions, SiteCreateError.MainAddressRegionNotFound, request.index) +
                    validateIdentifierTypeExists(mainAddress, addressMetadata.idTypes, SiteCreateError.MainAddressIdentifierNotFound, request.index) +
                    validateParentBpnExists(request.bpnlParent, request.index, existingParentBpns, SiteCreateError.LegalEntityNotFound)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateSiteUpdates(
        requests: Collection<SitePartnerUpdateRequest>,
    ): Map<SitePartnerUpdateRequest, Collection<ErrorInfo<SiteUpdateError>>> {

        val mainAddressRequests = requests.map { it.site.mainAddress }
        val addressMetadata = metadataService.getMetadata(mainAddressRequests).toKeys()

        val requestedSiteBpns = requests.map { it.bpns }
        val existingSiteBpns = siteRepository.findDistinctByBpnIn(requestedSiteBpns).map { it.bpn }.toSet()

        return requests.flatMap { request ->
            val mainAddress = request.site.mainAddress

            val validationErrors = validateRegionExists(mainAddress, addressMetadata.regions, SiteUpdateError.MainAddressRegionNotFound, request.bpns) +
                    validateIdentifierTypeExists(mainAddress, addressMetadata.idTypes, SiteUpdateError.MainAddressIdentifierNotFound, request.bpns) +
                    validateUpdateBpnExists(request.bpns, existingSiteBpns, SiteUpdateError.SiteNotFound)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateAddressCreates(
        requests: Collection<AddressPartnerCreateRequest>
    ): Map<AddressPartnerCreateRequest, Collection<ErrorInfo<AddressCreateError>>> {

        val mainAddressRequests = requests.map { it.address }
        val addressMetadata = metadataService.getMetadata(mainAddressRequests).toKeys()

        val requestsWithParentType = requests.map { Pair(it, bpnIssuingService.translateToBusinessPartnerType(it.bpnParent)) }
        val requestsByParentType = requestsWithParentType.groupBy({ it.second }, { it.first })

        val legalEntityParentBpns = requestsByParentType[BusinessPartnerType.LEGAL_ENTITY]?.map { it.bpnParent } ?: emptyList()
        val existingLegalEntities = legalEntityRepository.findDistinctByBpnIn(legalEntityParentBpns).map { it.bpn }.toSet()

        val siteParentBpns = requestsByParentType[BusinessPartnerType.SITE]?.map { it.bpnParent } ?: emptyList()
        val existingSites = siteRepository.findDistinctByBpnIn(siteParentBpns).map { it.bpn }.toSet()

        return requestsWithParentType.flatMap { (request, type) ->
            val address = request.address

            val validationErrors = validateAddressParent(request, type, existingLegalEntities, existingSites) +
                    validateRegionExists(address, addressMetadata.regions, AddressCreateError.RegionNotFound, request.index) +
                    validateIdentifierTypeExists(address, addressMetadata.idTypes, AddressCreateError.IdentifierNotFound, request.index)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }

    fun validateAddressUpdates(
        requests: Collection<AddressPartnerUpdateRequest>
    ): Map<AddressPartnerUpdateRequest, Collection<ErrorInfo<AddressUpdateError>>> {

        val mainAddressRequests = requests.map { it.address }
        val addressMetadata = metadataService.getMetadata(mainAddressRequests).toKeys()

        val requestedAddressBpns = requests.map { it.bpna }
        val existingAddressBpns = addressRepository.findDistinctByBpnIn(requestedAddressBpns).map { it.bpn }.toSet()

        return requests.flatMap { request ->
            val address = request.address

            val validationErrors = validateRegionExists(address, addressMetadata.regions, AddressUpdateError.RegionNotFound, request.bpna) +
                    validateIdentifierTypeExists(address, addressMetadata.idTypes, AddressUpdateError.IdentifierNotFound, request.bpna) +
                    validateUpdateBpnExists(request.bpna, existingAddressBpns, AddressUpdateError.AddressNotFound)

            validationErrors.map { Pair(request, it) }

        }.groupBy({ it.first }, { it.second })
    }


    fun findDuplicateLegalEntityIdentifiers(
        legalEntityRequests: List<LegalEntityDto>
    ): Set<LegalEntityIdentifierDto> {
        val allIdentifiers = legalEntityRequests.flatMap { it.identifiers }

        return allIdentifiers.groupBy { it }
            .filter { it.value.size > 1 }
            .keys
            .toSet()
    }


    private fun <ERROR : ErrorCode> validateIdentifierTypesExists(request: LegalEntityDto, existingTypes: Set<String>, error: ERROR, entityKey: String?)
            : Collection<ErrorInfo<ERROR>> {
        val requestedTypes = request.identifiers.map { it.type }
        val missingTypes = requestedTypes - existingTypes

        return missingTypes.map {
            ErrorInfo(
                error,
                "Legal Entity Identifier Type '$it' does not exist",
                entityKey
            )
        }
    }

    private fun <ERROR : ErrorCode> validateLegalFormExists(request: LegalEntityDto, existingLegalForms: Set<String>, error: ERROR, entityKey: String?)
            : Collection<ErrorInfo<ERROR>> {

        if (request.legalForm != null) {
            if (!existingLegalForms.contains(request.legalForm)) {
                return listOf(ErrorInfo(error, "Legal Form '${request.legalForm}' does not exist", entityKey))
            }
        }

        return emptyList()
    }

    private fun <ERROR : ErrorCode> validateIdentifierTypeExists(request: LogisticAddressDto, existingTypes: Set<String>, error: ERROR, entityKey: String?)
            : Collection<ErrorInfo<ERROR>> {
        val requestedTypes = request.identifiers.map { it.type }
        val missingTypes = requestedTypes - existingTypes

        return missingTypes.map {
            ErrorInfo(
                error,
                "Address Identifier Type '$it' does not exist",
                entityKey
            )
        }
    }

    private fun <ERROR : ErrorCode> validateRegionExists(request: LogisticAddressDto, existingRegions: Set<String>, error: ERROR, entityKey: String?)
            : Collection<ErrorInfo<ERROR>> {
        val requestedTypes = listOfNotNull(
            request.physicalPostalAddress.administrativeAreaLevel1,
            request.alternativePostalAddress?.administrativeAreaLevel1
        )

        val missingTypes = requestedTypes - existingRegions

        return missingTypes.map {
            ErrorInfo(
                error,
                "Address Identifier Type '$it' does not exist",
                entityKey
            )
        }
    }

    private fun <ERROR : ErrorCode> validateUpdateBpnExists(bpnToUpdate: String, existingBpns: Set<String>, error: ERROR)
            : Collection<ErrorInfo<ERROR>> {
        return if (!existingBpns.contains(bpnToUpdate))
            listOf(ErrorInfo(error, "Business Partner with BPN '$bpnToUpdate' can't be updated as it doesn't exist", bpnToUpdate))
        else
            emptyList()
    }

    private fun <ERROR : ErrorCode> validateParentBpnExists(parentBpn: String, entityKey: String?, existingParentBpns: Set<String>, error: ERROR)
            : Collection<ErrorInfo<ERROR>> {
        return if (!existingParentBpns.contains(parentBpn))
            listOf(ErrorInfo(error, "Parent with BPN '$parentBpn'not found", entityKey))
        else
            emptyList()
    }

    private fun getDuplicateLegalEntityCandidates(requests: Collection<LegalEntityDto>)
            : Map<IdentifierCandidateKey, IdentifierCandidate> {
        val idValues = requests.flatMap { it.identifiers }.map { it.value }
        return legalEntityIdentifierRepository.findByValueIn(idValues)
            .map { IdentifierCandidate(bpn = it.legalEntity.bpn, type = it.type.technicalKey, value = it.value) }
            .associateBy { IdentifierCandidateKey(it.type, it.value) }
    }

    private fun <ERROR : ErrorCode> validateIdentifierNotExist(
        legalEntity: LegalEntityDto,
        existingIdentifiers: Map<IdentifierCandidateKey, IdentifierCandidate>,
        bpn: String?,
        error: ERROR,
        entityKey: String?
    )
            : Collection<ErrorInfo<ERROR>> {
        return legalEntity.identifiers.mapNotNull {
            val identifierPair = IdentifierCandidateKey(type = it.type, value = it.value)
            existingIdentifiers[identifierPair]?.let { candidate ->
                if (candidate.bpn != bpn)
                    ErrorInfo(error, "Duplicate Legal Entity Identifier: Value '${it.value}' of Type '${it.type}'", entityKey)
                else
                    null
            }
        }
    }

    private fun validateAddressParent(
        request: AddressPartnerCreateRequest,
        type: BusinessPartnerType?,
        existingLegalEntities: Set<String>,
        existingSites: Set<String>
    )
            : Collection<ErrorInfo<AddressCreateError>> {
        return when (type) {
            BusinessPartnerType.LEGAL_ENTITY -> validateParentBpnExists(
                request.bpnParent,
                request.index,
                existingLegalEntities,
                AddressCreateError.LegalEntityNotFound
            )

            BusinessPartnerType.SITE -> validateParentBpnExists(request.bpnParent, request.index, existingSites, AddressCreateError.SiteNotFound)
            else -> listOf(ErrorInfo(AddressCreateError.BpnNotValid, "Parent '${request.bpnParent}' is not a valid BPNL/BPNS", request.index))
        }
    }

    private fun LegalEntityMetadataDto.toKeys(): LegalEntityMetadataKeys {
        return LegalEntityMetadataKeys(
            idTypes = idTypes.map { it.technicalKey }.toSet(),
            legalForms = legalForms.map { it.technicalKey }.toSet()
        )
    }

    private fun AddressMetadataDto.toKeys(): AddressMetadataKeys {
        return AddressMetadataKeys(
            idTypes = idTypes.map { it.technicalKey }.toSet(),
            regions = regions.map { it.regionCode }.toSet()
        )
    }


    private data class LegalEntityMetadataKeys(
        val idTypes: Set<String>,
        val legalForms: Set<String>
    )

    private data class AddressMetadataKeys(
        val idTypes: Set<String>,
        val regions: Set<String>
    )


    private data class IdentifierCandidateKey(
        val type: String,
        val value: String
    )

    private data class IdentifierCandidate(
        val bpn: String,
        val type: String,
        val value: String
    )
}