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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteStateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SitePartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import org.springframework.stereotype.Component

/**
 * Maps stored sites to the v6 API site DTOs.
 */
@Component
class SiteResponseMapperV6(
    private val addressResponseMapperV6: AddressResponseMapperV6,
    private val confidenceCriteriaResponseMapperV6: ConfidenceCriteriaResponseMapperV6
) {

    /**
     * Returns the given site as the v6 API reports it.
     */
    fun toSite(site: SiteDb): SiteVerboseDtoV6 =
        with(site) {
            SiteVerboseDtoV6(
                bpn,
                name,
                states = states.map { toState(it) },
                bpnLegalEntity = legalEntity.bpn,
                confidenceCriteria = confidenceCriteriaResponseMapperV6.toConfidenceCriteria(confidenceCriteria),
                isCatenaXMemberData = legalEntity.isDataSpaceParticipant,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

    /**
     * Returns the given site together with its main address as the v6 API reports them.
     */
    fun toSiteWithMainAddress(site: SiteDb): SiteWithMainAddressVerboseDtoV6 =
        SiteWithMainAddressVerboseDtoV6(
            site = toSite(site),
            mainAddress = addressResponseMapperV6.toAddress(site.mainAddress)
        )

    /**
     * Returns the given created or updated site as the v6 API reports it, tagged with the key of the request that
     * wrote it.
     */
    fun toUpsertResponse(site: SiteDb, entryId: String?): SitePartnerCreateVerboseDtoV6 =
        SitePartnerCreateVerboseDtoV6(
            site = toSite(site),
            mainAddress = addressResponseMapperV6.toAddress(site.mainAddress),
            index = entryId
        )

    private fun toState(state: SiteStateDb): SiteStateVerboseDtoV6 =
        SiteStateVerboseDtoV6(state.validFrom, state.validTo, state.type.toDto())
}
