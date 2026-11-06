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
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.ErrorCodeV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityCreateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.exception.LegalEntityUpdateErrorV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.error.ErrorInfoV7
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerUpdateV7ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.*
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityCreateResult
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.LegalEntityGetError.LegalEntityNotFound
import org.springframework.stereotype.Component

/**
 * Maps legal entity records and rejections to their API v7 form.
 */
@Component
class ApiV7LegalEntityMapper(
    private val apiV7AddressMapper: ApiV7AddressMapper,
    private val apiV7BusinessPartnerMapper: ApiV7BusinessPartnerMapper,
    private val apiV7MetadataMapper: ApiV7MetadataMapper
) {

    /**
     * Returns the API DTO for [record], together with its legal address.
     */
    fun toDto(record: LegalEntityRecord): LegalEntityWithLegalAddressVerboseV7Dto =
        LegalEntityWithLegalAddressVerboseV7Dto(
            legalAddress = apiV7AddressMapper.toInvariantDto(record.legalAddress),
            header = toHeaderDto(record),
            scriptVariants = record.scriptVariants.map { toDto(it) }
        )

    /**
     * Returns the exception the API reports a rejected legal entity fetch with.
     */
    fun toGetException(errors: List<LegalEntityGetError>): BpdmNotFoundException =
        when (val error = errors.first()) {
            is LegalEntityNotFound -> BpdmNotFoundException("Legal Entity", error.identifierValue)
        }

    /**
     * Returns the response for a create batch, each result reported under the index of the request at its position.
     */
    fun toCreateResponse(
        requests: List<LegalEntityPartnerCreateV7Request>,
        results: List<LegalEntityCreateResult>
    ): LegalEntityPartnerCreateV7ResponseWrapper {
        val entities = mutableListOf<LegalEntityPartnerCreateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<LegalEntityCreateErrorV7>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is LegalEntityCreateResult.Created -> entities.add(LegalEntityPartnerCreateVerboseV7Dto(toDto(result.legalEntity), request.index))
                is LegalEntityCreateResult.Rejected -> errors.addAll(result.errors.map { toCreateErrorInfo(it, request.index) })
            }
        }
        return LegalEntityPartnerCreateV7ResponseWrapper(entities, errors)
    }

    /**
     * Returns the response for an update batch, each result reported under the BPNL of the request at its position.
     */
    fun toUpdateResponse(
        requests: List<LegalEntityPartnerUpdateV7Request>,
        results: List<LegalEntityUpdateResult>
    ): LegalEntityPartnerUpdateV7ResponseWrapper {
        val entities = mutableListOf<LegalEntityPartnerCreateVerboseV7Dto>()
        val errors = mutableListOf<ErrorInfoV7<LegalEntityUpdateErrorV7>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is LegalEntityUpdateResult.Updated -> entities.add(LegalEntityPartnerCreateVerboseV7Dto(toDto(result.legalEntity), request.bpnl))
                is LegalEntityUpdateResult.Rejected -> errors.addAll(result.errors.map { toUpdateErrorInfo(it, request.bpnl) })
            }
        }
        return LegalEntityPartnerUpdateV7ResponseWrapper(entities, errors)
    }

    private fun toHeaderDto(record: LegalEntityRecord): LegalEntityHeaderVerboseV7Dto =
        LegalEntityHeaderVerboseV7Dto(
            bpnl = record.bpn,
            legalName = record.legalName,
            legalShortName = record.legalShortName,
            legalFormVerbose = record.legalForm?.let { apiV7MetadataMapper.toDto(it) },
            identifiers = record.identifiers.map { toDto(it) },
            states = record.states.map { toStateDto(it) },
            relations = record.relations.map { toDto(it) },
            currentness = record.currentness,
            confidenceCriteria = apiV7BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isParticipantData = record.isDataSpaceParticipant,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            ownershipUltimate = record.ownershipUltimate,
            ultimateOwnerBpnl = record.ultimateOwnerBpnl
        )

    private fun toDto(record: LegalEntityIdentifierRecord): LegalEntityIdentifierVerboseV7Dto =
        LegalEntityIdentifierVerboseV7Dto(record.value, TypeKeyNameVerboseDto(record.typeTechnicalKey, record.typeName), record.issuingBody)

    private fun toStateDto(record: BusinessStateRecord): LegalEntityStateVerboseV7Dto =
        LegalEntityStateVerboseV7Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )

    private fun toDto(record: LegalEntityRelationRecord): RelationVerboseV7Dto =
        RelationVerboseV7Dto(
            type = record.type,
            businessPartnerSourceBpnl = record.sourceBpnl,
            businessPartnerTargetBpnl = record.targetBpnl,
            validityPeriods = record.validityPeriods.map { apiV7BusinessPartnerMapper.toDto(it) },
            reasonCode = record.reasonCode
        )

    private fun toDto(record: LegalEntityScriptVariantRecord): LegalEntityScriptVariantV7Dto =
        LegalEntityScriptVariantV7Dto(
            record.scriptCode,
            record.legalName,
            record.shortName,
            apiV7AddressMapper.toPostalAddressDto(record.legalAddress)
        )

    private fun toCreateErrorInfo(error: LegalEntityCreateError, entityKey: String?): ErrorInfoV7<LegalEntityCreateErrorV7> =
        when (error) {
            is AddressContentError -> apiV7AddressMapper.toLegalAddressCreateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is LegalEntityContentError -> toContentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityCreateErrorV7.LegalFormNotFound,
                identifierNotFound = LegalEntityCreateErrorV7.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityCreateErrorV7.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityCreateErrorV7.LegalEntityIdentifiersTooMany,
                scriptVariantLegalNameMissing = LegalEntityCreateErrorV7.ScriptVariantLegalNameMissing,
                scriptVariantDuplicateScriptCode = LegalEntityCreateErrorV7.ScriptVariantDuplicateScriptCode
            )
        }

    private fun toUpdateErrorInfo(error: LegalEntityUpdateError, entityKey: String?): ErrorInfoV7<LegalEntityUpdateErrorV7> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV7(LegalEntityUpdateErrorV7.LegalEntityNotFound, "Legal entity '${error.bpn}' can't be updated as it doesn't exist", entityKey)
            is MultipleUltimateOwnersInHierarchy -> ErrorInfoV7(
                LegalEntityUpdateErrorV7.MultipleUltimateOwnersInHierarchy,
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}",
                entityKey
            )
            is AlternativeHeadquarterCannotOwnUltimately -> ErrorInfoV7(
                LegalEntityUpdateErrorV7.AlternativeHeadquarterCannotOwnUltimately,
                "Legal entity '${error.bpnl}' cannot carry the ultimate-owner flag because it is an alternative headquarter",
                entityKey
            )
            is AddressContentError -> apiV7AddressMapper.toLegalAddressUpdateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is LegalEntityContentError -> toContentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityUpdateErrorV7.LegalFormNotFound,
                identifierNotFound = LegalEntityUpdateErrorV7.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityUpdateErrorV7.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityUpdateErrorV7.LegalEntityIdentifiersTooMany,
                scriptVariantLegalNameMissing = LegalEntityUpdateErrorV7.ScriptVariantLegalNameMissing,
                scriptVariantDuplicateScriptCode = LegalEntityUpdateErrorV7.ScriptVariantDuplicateScriptCode
            )
        }

    private fun <E : ErrorCodeV7> toContentErrorInfo(
        error: LegalEntityContentError,
        entityKey: String?,
        legalFormNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E,
        scriptVariantLegalNameMissing: E,
        scriptVariantDuplicateScriptCode: E
    ): ErrorInfoV7<E> =
        when (error) {
            is LegalEntityContentError.LegalFormNotFound ->
                ErrorInfoV7(legalFormNotFound, "Legal form '${error.legalForm}' does not exist", entityKey)
            is LegalEntityContentError.IdentifierTypeNotFound ->
                ErrorInfoV7(identifierNotFound, "Legal Entity Identifier Type '${error.type}' does not exist", entityKey)
            is LegalEntityContentError.DuplicateIdentifier ->
                ErrorInfoV7(duplicateIdentifier, "Duplicate Legal Entity Identifier: Value '${error.value}' of type '${error.type}'", entityKey)
            is LegalEntityContentError.IdentifiersTooMany ->
                ErrorInfoV7(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
            is LegalEntityContentError.ScriptVariantLegalNameMissing ->
                ErrorInfoV7(scriptVariantLegalNameMissing, "Script variant ${error.index} has no legal name", entityKey)
            is LegalEntityContentError.ScriptVariantDuplicateScriptCode ->
                ErrorInfoV7(scriptVariantDuplicateScriptCode, "Duplicate legal entity script variant for script code '${error.scriptCode}'", entityKey)
            is LegalEntityContentError.NameMissing,
            is LegalEntityContentError.ConfidenceCriteriaMissing,
            is LegalEntityContentError.IdentifierValueMissing,
            is LegalEntityContentError.IdentifierTypeMissing,
            is LegalEntityContentError.ScriptCodeNotFound -> throw internalError(error)
        }

    // An error the bounded DTO already rules out, or that this use case cannot reach, gets no public code.
    private fun internalError(error: LegalEntityCreateError) =
        BpdmValidationException("Unexpected legal entity parse error (no public error code): $error")
}
