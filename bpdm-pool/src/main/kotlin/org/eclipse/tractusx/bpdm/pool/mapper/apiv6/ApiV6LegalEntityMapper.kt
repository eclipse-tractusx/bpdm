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
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityCreateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityIdentifierVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6Request
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityRelationTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityStateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.relation.RelationVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.LegalEntityParseErrorMapperV6
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityCreateResult
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRelationRecord
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
    private val legalEntityParseErrorMapperV6: LegalEntityParseErrorMapperV6
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
                is LegalEntityCreateResult.Rejected -> errors.addAll(result.errors.map { legalEntityParseErrorMapperV6.toCreateErrorInfo(it, request.index) })
            }
        }
        return LegalEntityPartnerCreateV6ResponseWrapper(entities, errors)
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
}
