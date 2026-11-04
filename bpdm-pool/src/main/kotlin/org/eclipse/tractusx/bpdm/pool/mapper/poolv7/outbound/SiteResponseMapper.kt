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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteScriptVariantV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteStateVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import org.springframework.stereotype.Component

/**
 * Maps stored sites to the v7 API site DTOs.
 */
@Component
class SiteResponseMapper(
    private val addressResponseMapper: AddressResponseMapper,
    private val confidenceCriteriaResponseMapper: ConfidenceCriteriaResponseMapper,
    private val relationResponseMapper: RelationResponseMapper
) {

    /**
     * Returns the given site as the API reports it.
     */
    fun toSite(site: SiteDb): SiteVerboseV7Dto =
        with(site) {
            SiteVerboseV7Dto(
                bpn,
                name,
                states = states.map { toState(it) },
                bpnLegalEntity = legalEntity.bpn,
                confidenceCriteria = confidenceCriteriaResponseMapper.toConfidenceCriteria(confidenceCriteria),
                isParticipantData = legalEntity.isDataSpaceParticipant,
                scriptVariants = toScriptVariants(site),
                createdAt = createdAt.instant,
                updatedAt = updatedAt.instant,
                relations = startSiteRelations.plus(endSiteRelations).map { relationResponseMapper.toSiteRelation(it) }
            )
        }

    /**
     * Returns the given site together with its main address as the API reports them.
     */
    fun toSiteWithMainAddress(site: SiteDb): SiteWithMainAddressVerboseV7Dto =
        SiteWithMainAddressVerboseV7Dto(
            site = toSite(site),
            mainAddress = addressResponseMapper.toInvariantAddress(site.mainAddress)
        )

    private fun toState(state: SiteStateDb): SiteStateVerboseV7Dto =
        SiteStateVerboseV7Dto(
            BusinessStateDates.reportValidFrom(state.validFrom),
            BusinessStateDates.reportValidTo(state.validTo),
            state.type.toDto()
        )

    // The main address covers every script its site is named in: the parsers reject a variant it does not cover and
    // ScriptVariantCoverageService prunes any the main address stops covering.
    private fun toScriptVariants(site: SiteDb): List<SiteScriptVariantV7Dto> {
        val mainAddressVariantsByCode = site.mainAddress.scriptVariants.associateBy { it.scriptCode.technicalKey }

        return site.scriptVariants.map { variant ->
            val scriptCode = variant.scriptCode.technicalKey
            val mainAddressVariant = mainAddressVariantsByCode[scriptCode]
                ?: throw IllegalStateException("Site script variant of script code '$scriptCode' is not covered by the main address.")
            SiteScriptVariantV7Dto(scriptCode, variant.name, addressResponseMapper.toPostalAddressScriptVariant(mainAddressVariant))
        }
    }
}
