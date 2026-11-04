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
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteRelationVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteGetError.SiteNotFound
import org.springframework.stereotype.Component

/**
 * Maps site records and rejections to their API v7 form.
 */
@Component
class ApiV7SiteMapper(
    private val apiV7AddressMapper: ApiV7AddressMapper,
    private val apiV7BusinessPartnerMapper: ApiV7BusinessPartnerMapper
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

    private fun toSiteDto(record: SiteRecord): SiteVerboseV7Dto =
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
