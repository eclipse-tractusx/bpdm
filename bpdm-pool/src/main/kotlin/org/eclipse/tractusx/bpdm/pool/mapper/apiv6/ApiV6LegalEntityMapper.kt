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
import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.ErrorCodeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityIdentifierVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityRelationTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.relation.RelationVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.AddressParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityCreateResult
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityUpdateResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityGetError.LegalEntityNotFound
import org.springframework.stereotype.Component

/**
 * Maps legal entity records and rejections to their API v6 form.
 */
@Component
class ApiV6LegalEntityMapper(
    private val apiV6AddressMapper: ApiV6AddressMapper,
    private val apiV6BusinessPartnerMapper: ApiV6BusinessPartnerMapper,
    private val apiV6MetadataMapper: ApiV6MetadataMapper,
    private val addressParseErrorMapperV6: AddressParseErrorMapperV6
) {

    /**
     * Returns the API DTO for [record], together with its legal address.
     */
    fun toDto(record: LegalEntityRecord): LegalEntityWithLegalAddressVerboseV6Dto =
        LegalEntityWithLegalAddressVerboseV6Dto(
            legalEntity = toLegalEntityDto(record),
            legalAddress = apiV6AddressMapper.toDto(record.legalAddress)
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
        requests: List<LegalEntityPartnerCreateV6Request>,
        results: List<LegalEntityCreateResult>
    ): LegalEntityPartnerCreateV6ResponseWrapper {
        val entities = mutableListOf<LegalEntityPartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<LegalEntityCreateErrorV6>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is LegalEntityCreateResult.Created -> entities.add(toCreateDto(result.legalEntity, request.index))
                is LegalEntityCreateResult.Rejected -> errors.addAll(result.errors.map { toCreateErrorInfo(it, request.index) })
            }
        }
        return LegalEntityPartnerCreateV6ResponseWrapper(entities, errors)
    }

    /**
     * Returns the response for an update batch, each result reported under the BPNL of the request at its position.
     */
    fun toUpdateResponse(
        requests: List<LegalEntityPartnerUpdateV6Request>,
        results: List<LegalEntityUpdateResult>
    ): LegalEntityPartnerUpdateV6ResponseWrapper {
        val entities = mutableListOf<LegalEntityPartnerCreateVerboseV6Dto>()
        val errors = mutableListOf<ErrorInfoV6<LegalEntityUpdateErrorV6>>()
        requests.zip(results).forEach { (request, result) ->
            when (result) {
                is LegalEntityUpdateResult.Updated -> entities.add(toCreateDto(result.legalEntity, request.bpnl))
                is LegalEntityUpdateResult.Rejected -> errors.addAll(result.errors.map { toUpdateErrorInfo(it, request.bpnl) })
            }
        }
        return LegalEntityPartnerUpdateV6ResponseWrapper(entities, errors)
    }

    private fun toCreateDto(record: LegalEntityRecord, index: String?): LegalEntityPartnerCreateVerboseV6Dto =
        LegalEntityPartnerCreateVerboseV6Dto(
            legalEntity = toLegalEntityDto(record),
            legalAddress = apiV6AddressMapper.toDto(record.legalAddress),
            index = index
        )

    private fun toLegalEntityDto(record: LegalEntityRecord): LegalEntityVerboseV6Dto =
        LegalEntityVerboseV6Dto(
            bpnl = record.bpn,
            legalName = record.legalName,
            legalShortName = record.legalShortName,
            legalFormVerbose = record.legalForm?.let { apiV6MetadataMapper.toDto(it) },
            identifiers = record.identifiers.map { toDto(it) },
            states = record.states.map { toStateDto(it) },
            relations = record.relations.mapNotNull { toDto(it) },
            currentness = record.currentness,
            confidenceCriteria = apiV6BusinessPartnerMapper.toDto(record.confidenceCriteria),
            isCatenaXMemberData = record.isDataSpaceParticipant,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )

    private fun toDto(record: LegalEntityIdentifierRecord): LegalEntityIdentifierVerboseV6Dto =
        LegalEntityIdentifierVerboseV6Dto(record.value, TypeKeyNameVerboseDto(record.typeTechnicalKey, record.typeName), record.issuingBody)

    private fun toStateDto(record: BusinessStateRecord): LegalEntityStateVerboseV6Dto =
        LegalEntityStateVerboseV6Dto(
            BusinessStateDates.reportValidFrom(record.validFrom),
            BusinessStateDates.reportValidTo(record.validTo),
            record.type.toDto()
        )

    // Relation types introduced after v6 was frozen are omitted from v6 responses rather than added to its enum,
    // which would change a deprecated contract. Mapping them exhaustively keeps a later type from silently reaching here.
    private fun toDto(record: LegalEntityRelationRecord): RelationVerboseV6Dto? =
        toRelationType(record.type)?.let { RelationVerboseV6Dto(it, record.sourceBpnl, record.targetBpnl) }

    private fun toRelationType(relationType: LegalEntityRelationTypeV7): LegalEntityRelationTypeV6? =
        when (relationType) {
            LegalEntityRelationTypeV7.IsAlternativeHeadquarterFor -> LegalEntityRelationTypeV6.IsAlternativeHeadquarterFor
            LegalEntityRelationTypeV7.IsManagedBy -> LegalEntityRelationTypeV6.IsManagedBy
            LegalEntityRelationTypeV7.IsOwnedBy -> LegalEntityRelationTypeV6.IsOwnedBy
            LegalEntityRelationTypeV7.IsReplacedBy -> null
        }

    private fun toCreateErrorInfo(error: LegalEntityCreateParseError, entityKey: String?): ErrorInfoV6<LegalEntityCreateErrorV6> =
        when (error) {
            is AddressContentParseError -> addressParseErrorMapperV6.toLegalEntityCreateErrorInfo(error, entityKey)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is LegalEntityContentParseError -> toContentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityCreateErrorV6.LegalFormNotFound,
                identifierNotFound = LegalEntityCreateErrorV6.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityCreateErrorV6.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityCreateErrorV6.LegalEntityIdentifiersTooMany
            )
        }

    private fun toUpdateErrorInfo(error: LegalEntityUpdateParseError, entityKey: String?): ErrorInfoV6<LegalEntityUpdateErrorV6> =
        when (error) {
            is UnresolvableLegalEntity ->
                ErrorInfoV6(
                    LegalEntityUpdateErrorV6.LegalEntityNotFound,
                    "Legal entity '${error.bpn}' can't be updated as it doesn't exist",
                    entityKey
                )
            is AddressContentParseError -> addressParseErrorMapperV6.toLegalEntityUpdateErrorInfo(error, entityKey)
            // A v6 write never sets the ownership flag, so the uniqueness rule it guards cannot be broken from v6.
            is MultipleUltimateOwnersInHierarchy -> throw internalError(error)
            // A v6 write never sets the ownership flag on an alternative headquarter, so this cannot be broken from v6.
            is AlternativeHeadquarterCannotOwnUltimately -> throw internalError(error)
            is ScriptVariantNotCoveredByAddress -> throw internalError(error)
            is LegalEntityContentParseError -> toContentErrorInfo(
                error,
                entityKey,
                legalFormNotFound = LegalEntityUpdateErrorV6.LegalFormNotFound,
                identifierNotFound = LegalEntityUpdateErrorV6.LegalEntityIdentifierNotFound,
                duplicateIdentifier = LegalEntityUpdateErrorV6.LegalEntityDuplicateIdentifier,
                identifiersTooMany = LegalEntityUpdateErrorV6.LegalEntityIdentifiersTooMany
            )
        }

    private fun <E : ErrorCodeV6> toContentErrorInfo(
        error: LegalEntityContentParseError,
        entityKey: String?,
        legalFormNotFound: E,
        identifierNotFound: E,
        duplicateIdentifier: E,
        identifiersTooMany: E
    ): ErrorInfoV6<E> =
        when (error) {
            is LegalEntityContentParseError.LegalFormNotFound ->
                ErrorInfoV6(legalFormNotFound, "Legal form '${error.legalForm}' does not exist", entityKey)
            is LegalEntityContentParseError.IdentifierTypeNotFound ->
                ErrorInfoV6(identifierNotFound, "Legal Entity Identifier Type '${error.type}' does not exist", entityKey)
            is LegalEntityContentParseError.DuplicateIdentifier ->
                ErrorInfoV6(
                    duplicateIdentifier,
                    "Duplicate Legal Entity Identifier: Value '${error.value}' of type '${error.type}'",
                    entityKey
                )
            is LegalEntityContentParseError.IdentifiersTooMany ->
                ErrorInfoV6(identifiersTooMany, "Amount of identifiers (${error.count}) exceeds the allowed limit", entityKey)
            is LegalEntityContentParseError.ScriptVariantLegalNameMissing,
            is LegalEntityContentParseError.ScriptVariantDuplicateScriptCode,
            is LegalEntityContentParseError.NameMissing,
            is LegalEntityContentParseError.ConfidenceCriteriaMissing,
            is LegalEntityContentParseError.IdentifierValueMissing,
            is LegalEntityContentParseError.IdentifierTypeMissing,
            is LegalEntityContentParseError.ScriptCodeNotFound -> throw internalError(error)
        }

    // The frozen v6 error enums predate script variants and ultimate ownership, so those errors get no public code.
    private fun internalError(error: Any) =
        BpdmValidationException("Unexpected legal entity parse error (no v6 client error code): $error")
}
