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
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityHeaderVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityIdentifierVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityIdentifierRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityGetError.LegalEntityNotFound
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
}
