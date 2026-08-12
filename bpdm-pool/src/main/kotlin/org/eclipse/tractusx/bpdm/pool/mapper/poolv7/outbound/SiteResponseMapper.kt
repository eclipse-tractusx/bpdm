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

import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteStateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
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
    fun toSite(site: SiteDb): SiteVerboseDto =
        with(site) {
            SiteVerboseDto(
                bpn,
                name,
                states = states.map { toState(it) },
                bpnLegalEntity = legalEntity.bpn,
                confidenceCriteria = confidenceCriteriaResponseMapper.toConfidenceCriteria(confidenceCriteria),
                isParticipantData = legalEntity.isDataSpaceParticipant,
                scriptVariants = toScriptVariants(site),
                createdAt = createdAt,
                updatedAt = updatedAt,
                relations = startSiteRelations.plus(endSiteRelations).map { relationResponseMapper.toSiteRelation(it) }
            )
        }

    /**
     * Returns the given site together with its main address as the API reports them.
     */
    fun toSiteWithMainAddress(site: SiteDb): SiteWithMainAddressVerboseDto =
        SiteWithMainAddressVerboseDto(
            site = toSite(site),
            mainAddress = addressResponseMapper.toInvariantAddress(site.mainAddress)
        )

    /**
     * Returns the given created or updated site as the API reports it, tagged with the key of the request that wrote it.
     */
    fun toUpsertResponse(site: SiteDb, entryId: String?): SitePartnerCreateVerboseDto =
        SitePartnerCreateVerboseDto(
            site = toSite(site),
            mainAddress = addressResponseMapper.toInvariantAddress(site.mainAddress),
            index = entryId
        )

    private fun toState(state: SiteStateDb): SiteStateVerboseDto =
        SiteStateVerboseDto(state.validFrom, state.validTo, state.type.toDto())

    // The main address covers every script its site is named in: the parsers reject a variant it does not cover and
    // ScriptVariantCoverageService prunes any the main address stops covering.
    private fun toScriptVariants(site: SiteDb): List<SiteScriptVariantDto> {
        val mainAddressVariantsByCode = site.mainAddress.scriptVariants.associateBy { it.scriptCode.technicalKey }

        return site.scriptVariants.map { variant ->
            val scriptCode = variant.scriptCode.technicalKey
            val mainAddressVariant = mainAddressVariantsByCode[scriptCode]
                ?: throw IllegalStateException("Site script variant of script code '$scriptCode' is not covered by the main address.")
            SiteScriptVariantDto(scriptCode, variant.name, addressResponseMapper.toPostalAddressScriptVariant(mainAddressVariant))
        }
    }
}
