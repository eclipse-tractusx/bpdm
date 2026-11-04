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
package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.SiteUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteCreateWithLegalAddressAsMainV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound.AddressParseErrorMapper
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteCreateResult
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.LegalEntitySiteSearchError.LegalEntityNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError.SiteNotFound
import org.springframework.stereotype.Component

/**
 * Maps site records and rejections to their API v7 form.
 */
@Component
class ApiV7SiteMapper(
    private val apiV7AddressMapper: ApiV7AddressMapper,
    private val apiV7BusinessPartnerMapper: ApiV7BusinessPartnerMapper,
    private val addressParseErrorMapper: AddressParseErrorMapper
) {

    /**
     * Returns the API DTO for [record], together with its main address.
     */
    fun toDto(record: SiteRecord): SiteWithMainAddressVerboseV7Dto =
        SiteWithMainAddressVerboseV7Dto(
            site = toSiteDto(record),
            mainAddress = apiV7AddressMapper.toInvariantDto(record.mainAddress)
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
    fun toCreateResponse(requests: List<SitePartnerCreateV7Request>, results: List<SiteCreateResult>): SitePartnerCreateV7ResponseWrapper =
        toKeyedCreateResponse(requests.map { it.index }, results)

    /**
     * Returns the response for a batch of sites created on their legal addresses, each result reported under its
     * position in the batch.
     */
    fun toLegalAddressAsMainCreateResponse(
        requests: List<SiteCreateWithLegalAddressAsMainV7Request>,
        results: List<SiteCreateResult>
    ): SitePartnerCreateV7ResponseWrapper =
        toKeyedCreateResponse(requests.indices.map { it.toString() }, results)

    /**
     * Returns the response for an update batch, each result reported under the BPN of the request at its position.
     */
    fun toUpdateResponse(requests: List<SitePartnerUpdateV7Request>, results: List<SiteUpdateResult>): SitePartnerUpdateV7ResponseWrapper {
        val entities = mutableListOf<SitePartnerCreateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<SiteUpdateErrorV7>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is SiteUpdateResult.Updated -> entities.add(toCreateDto(result.site, request.bpns))
                is SiteUpdateResult.Rejected -> errors.addAll(result.errors.map { toUpdateErrorInfo(it, request.bpns) })
            }
        }
        return SitePartnerUpdateV7ResponseWrapper(entities, errors)
    }

    /**
     * Returns the API DTO for [record], without its main address.
     */
    fun toSiteDto(record: SiteRecord): SiteVerboseV7Dto =
        SiteVerboseV7Dto(
            bpns = record.bpn,
            name = record.name,
            states = record.states.map { toStateDto(it) },
            bpnLegalEntity = record.legalEntityBpn,
            confidenceCriteria = apiV7BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isParticipantData = record.isDataSpaceParticipant,
            scriptVariants = record.scriptVariants.map { toDto(it) },
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            relations = record.relations.map { toDto(it) }
        )

    private fun toKeyedCreateResponse(entityKeys: List<String?>, results: List<SiteCreateResult>): SitePartnerCreateV7ResponseWrapper {
        val entities = mutableListOf<SitePartnerCreateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<SiteCreateErrorV7>>()
        entityKeys.zip(results).forEach { (entityKey, result) ->
            when (result) {
                is SiteCreateResult.Created -> entities.add(toCreateDto(result.site, entityKey))
                is SiteCreateResult.Rejected -> errors.addAll(result.errors.map { toCreateErrorInfo(it, entityKey) })
            }
        }
        return SitePartnerCreateV7ResponseWrapper(entities, errors)
    }

    private fun toCreateDto(record: SiteRecord, entityKey: String?): SitePartnerCreateVerboseV7Dto =
        SitePartnerCreateVerboseV7Dto(
            site = toSiteDto(record),
            mainAddress = apiV7AddressMapper.toInvariantDto(record.mainAddress),
            index = entityKey
        )

    private fun toCreateErrorInfo(error: SiteCreateParseError, entityKey: String?): ErrorInfoV7<SiteCreateErrorV7> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV7(SiteCreateErrorV7.LegalEntityNotFound, "Parent legal entity '${error.bpn}' not found", entityKey)
            is LegalAddressAlreadyMainAddress ->
                ErrorInfoV7(SiteCreateErrorV7.MainAddressDuplicateIdentifier, "Legal address already belongs to site '${error.bpnSite}'", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toSiteCreateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress ->
                ErrorInfoV7(
                    SiteCreateErrorV7.ScriptVariantNotCoveredByMainAddress,
                    "Script code '${error.scriptCode}' is not covered by the site's main address",
                    entityKey
                )
            is SiteContentParseError.ScriptVariantNameMissing ->
                ErrorInfoV7(SiteCreateErrorV7.ScriptVariantNameMissing, "Script variant ${error.index} has no site name", entityKey)
            is SiteContentParseError.ScriptVariantDuplicateScriptCode ->
                ErrorInfoV7(
                    SiteCreateErrorV7.ScriptVariantDuplicateScriptCode,
                    "Duplicate site script variant for script code '${error.scriptCode}'",
                    entityKey
                )
            is SiteContentParseError.NameMissing,
            is SiteContentParseError.ConfidenceCriteriaMissing,
            is SiteContentParseError.ScriptCodeNotFound,
            is UnresolvableAddress,
            is ScriptVariantCoverageStillNeeded -> throw internalError(error)
        }

    private fun toUpdateErrorInfo(error: SiteUpdateParseError, entityKey: String?): ErrorInfoV7<SiteUpdateErrorV7> =
        when (error) {
            is UnresolvableSite ->
                ErrorInfoV7(SiteUpdateErrorV7.SiteNotFound, "Site '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is AddressContentParseError -> addressParseErrorMapper.toSiteUpdateErrorInfo(error, entityKey)
            is ScriptVariantCoverageStillNeeded ->
                ErrorInfoV7(
                    SiteUpdateErrorV7.ScriptVariantCoverageStillNeeded,
                    "Script code '${error.scriptCode}' must stay covered by the main address: business partner " +
                            "'${error.requiredByBpn}' is named in that script",
                    entityKey
                )
            is SiteContentParseError.ScriptVariantNameMissing ->
                ErrorInfoV7(SiteUpdateErrorV7.ScriptVariantNameMissing, "Script variant ${error.index} has no site name", entityKey)
            is SiteContentParseError.ScriptVariantDuplicateScriptCode ->
                ErrorInfoV7(
                    SiteUpdateErrorV7.ScriptVariantDuplicateScriptCode,
                    "Duplicate site script variant for script code '${error.scriptCode}'",
                    entityKey
                )
            is SiteContentParseError.NameMissing,
            is SiteContentParseError.ConfidenceCriteriaMissing,
            is SiteContentParseError.ScriptCodeNotFound,
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
        }

    // An error the bounded DTO already rules out, or that this use case cannot reach, gets no public code.
    private fun internalError(error: SiteCreateParseError) =
        BpdmValidationException("Unexpected site parse error (no public error code): $error")

    private fun toStateDto(record: BusinessStateRecord): SiteStateVerboseV7Dto =
        SiteStateVerboseV7Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )

    private fun toDto(record: SiteRelationRecord): SiteRelationVerboseV7Dto =
        SiteRelationVerboseV7Dto(
            type = record.type,
            businessPartnerSourceBpns = record.sourceBpns,
            businessPartnerTargetBpns = record.targetBpns,
            validityPeriods = record.validityPeriods.map { apiV7BusinessPartnerMapper.toDto(it) },
            reasonCode = record.reasonCode
        )

    private fun toDto(record: SiteScriptVariantRecord): SiteScriptVariantV7Dto =
        SiteScriptVariantV7Dto(record.scriptCode, record.name, apiV7AddressMapper.toPostalAddressDto(record.mainAddress))
}
