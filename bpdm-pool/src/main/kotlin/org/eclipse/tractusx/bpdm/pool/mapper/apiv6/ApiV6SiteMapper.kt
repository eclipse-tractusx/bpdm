/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.SiteUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteCreateWithLegalAddressAsMainV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteCreateResult
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError.LegalEntityNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError.SiteNotFound
import org.springframework.stereotype.Component

/**
 * Maps site records and rejections to their API v6 form.
 */
@Component
class ApiV6SiteMapper(
    private val apiV6AddressMapper: ApiV6AddressMapper,
    private val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper,
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6
) {

    /**
     * Returns the API DTO for [record], together with its main address.
     */
    fun toDto(record: SiteRecord): SiteWithMainAddressVerboseV6Dto =
        SiteWithMainAddressVerboseV6Dto(
            site = toSiteDto(record),
            mainAddress = apiV6AddressMapper.toDto(record.mainAddress)
        )

    /**
     * Returns the exception the API reports a rejected site fetch with.
     */
    fun toGetException(errors: List<SiteGetError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is SiteNotFound -> BpdmNotFoundException("Site", error.bpn)
        }

    /**
     * Returns the exception the API reports a rejected search for the sites of a legal entity with.
     */
    fun toLegalEntitySiteSearchException(errors: List<LegalEntitySiteSearchError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is LegalEntityNotFound -> BpdmNotFoundException("Business Partner", error.bpn)
        }

    /**
     * Returns the response for a create batch, each result reported under the index of the request at its position.
     */
    fun toCreateResponse(requests: List<SitePartnerCreateV6Request>, results: List<SiteCreateResult>): SitePartnerCreateV6ResponseWrapper {
        val entities = mutableListOf<SitePartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<SiteCreateErrorV6>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is SiteCreateResult.Created -> entities.add(toCreateDto(result.site, request.index))
                is SiteCreateResult.Rejected -> errors.addAll(result.errors.map { toCreateErrorInfo(it, request.index) })
            }
        }
        return SitePartnerCreateV6ResponseWrapper(entities, errors)
    }

    /**
     * Returns the response for a batch of sites created on their legal addresses, each created site reported under its
     * position in the batch and each rejection under the request value it concerns.
     */
    fun toLegalAddressAsMainCreateResponse(
        requests: List<SiteCreateWithLegalAddressAsMainV6Request>,
        results: List<SiteCreateResult>
    ): SitePartnerCreateV6ResponseWrapper {
        val entities = mutableListOf<SitePartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<SiteCreateErrorV6>>()
        requests.zip(results).forEachIndexed { position, (request, result) ->
            when (result) {
                is SiteCreateResult.Created -> entities.add(toCreateDto(result.site, position.toString()))
                is SiteCreateResult.Rejected -> errors.addAll(result.errors.map { toLegalAddressAsMainCreateErrorInfo(it, request, position) })
            }
        }
        return SitePartnerCreateV6ResponseWrapper(entities, errors)
    }

    /**
     * Returns the response for an update batch, each result reported under the BPN of the request at its position.
     */
    fun toUpdateResponse(requests: List<SitePartnerUpdateV6Request>, results: List<SiteUpdateResult>): SitePartnerUpdateV6ResponseWrapper {
        val entities = mutableListOf<SitePartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<SiteUpdateErrorV6>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is SiteUpdateResult.Updated -> entities.add(toCreateDto(result.site, request.bpns))
                is SiteUpdateResult.Rejected -> errors.addAll(result.errors.map { toUpdateErrorInfo(it, request.bpns) })
            }
        }
        return SitePartnerUpdateV6ResponseWrapper(entities, errors)
    }

    /**
     * Returns the API DTO for [record], without its main address.
     */
    fun toSiteDto(record: SiteRecord): SiteVerboseV6Dto =
        SiteVerboseV6Dto(
            bpns = record.bpn,
            name = record.name,
            states = record.states.map { toStateDto(it) },
            bpnLegalEntity = record.legalEntityBpn,
            confidenceCriteria = apiV6BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isCatenaXMemberData = record.isDataSpaceParticipant,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    private fun toCreateDto(record: SiteRecord, entityKey: String?): SitePartnerCreateVerboseV6Dto =
        SitePartnerCreateVerboseV6Dto(
            site = toSiteDto(record),
            mainAddress = apiV6AddressMapper.toDto(record.mainAddress),
            index = entityKey
        )

    private fun toLegalAddressAsMainCreateErrorInfo(
        error: SiteCreateParseError,
        request: SiteCreateWithLegalAddressAsMainV6Request,
        position: Int
    ): ErrorInfoV6<SiteCreateErrorV6> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV6(SiteCreateErrorV6.LegalEntityNotFound, "Parent ${error.bpn} not found for site to create", request.bpnLParent)
            is LegalAddressAlreadyMainAddress ->
                ErrorInfoV6(
                    SiteCreateErrorV6.MainAddressDuplicateIdentifier,
                    "Can't create site for legal entity ${request.bpnLParent} with legal address as site main address: " +
                            "Legal address already belongs to site ${error.bpnSite}",
                    request.name
                )
            else -> toCreateErrorInfo(error, position.toString())
        }

    private fun toCreateErrorInfo(error: SiteCreateParseError, entityKey: String?): ErrorInfoV6<SiteCreateErrorV6> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV6(SiteCreateErrorV6.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is LegalAddressAlreadyMainAddress ->
                ErrorInfoV6(SiteCreateErrorV6.MainAddressDuplicateIdentifier, "Legal address already belongs to site '${error.bpnSite}'", entityKey)
            is AddressContentParseError -> addressParseErrorMapperV6.toSiteCreateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress,
            is UnresolvableAddress,
            is ScriptVariantCoverageStillNeeded,
            is SiteContentParseError -> throw internalError(error)
        }

    private fun toUpdateErrorInfo(error: SiteUpdateParseError, entityKey: String?): ErrorInfoV6<SiteUpdateErrorV6> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfoV6(SiteUpdateErrorV6.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapperV6.toSiteUpdateErrorInfo(error, entityKey)
            // Reachable over v6: a v6 write sends no script variants, so it can drop coverage another business partner
            // still needs.
            is ScriptVariantCoverageStillNeeded,
            is ScriptVariantNotCoveredByAddress,
            is SiteContentParseError -> throw internalError(error)
        }

    // The frozen v6 error enums predate script variants, so those errors get no public code.
    private fun internalError(error: SiteCreateParseError) =
        BpdmValidationException("Unexpected site parse error (no v6 client error code): $error")

    private fun toStateDto(record: BusinessStateRecord): SiteStateVerboseV6Dto =
        SiteStateVerboseV6Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )
}
