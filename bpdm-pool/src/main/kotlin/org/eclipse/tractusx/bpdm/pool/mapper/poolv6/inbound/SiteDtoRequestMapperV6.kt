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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.inbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.ConfidenceCriteriaDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.SiteStateDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SiteCreateRequestWithLegalAddressAsMainV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerCreateRequestV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.SitePartnerUpdateRequestV6
import org.eclipse.tractusx.bpdm.pool.model.SiteState
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Maps a **v6** site request into the shared loose site request models. v6 sites carry no script variants, so a site
 * written over v6 always sends none.
 */
@Component
class SiteDtoRequestMapperV6(
    private val addressDtoRequestMapperV6: AddressDtoRequestMapperV6
) {

    /**
     * Returns the create request for the site a client sent, under the parent legal entity it named.
     */
    fun toCreateRequest(request: SitePartnerCreateRequestV6): SiteCreateRequest =
        SiteCreateRequest(legalEntityBpn = request.bpnlParent, content = toContentRequest(request.site))

    /**
     * Returns the update request for the site a client sent, addressed by its BPN.
     */
    fun toUpdateRequest(request: SitePartnerUpdateRequestV6): SiteUpdateRequest =
        SiteUpdateRequest(siteBpn = request.bpns, content = toContentRequest(request.site))

    /**
     * Returns the create request for the site a client sent that takes its parent's legal address as main address.
     */
    fun toCreateWithLegalAddressAsMainRequest(request: SiteCreateRequestWithLegalAddressAsMainV6): SiteCreateWithLegalAddressAsMainRequest =
        SiteCreateWithLegalAddressAsMainRequest(
            legalEntityBpn = request.bpnLParent,
            header = SiteHeaderRequest(
                name = request.name,
                states = request.states.map { toStateRequest(it) },
                confidenceCriteria = toConfidenceRequest(request.confidenceCriteria),
                scriptVariants = emptyList()
            )
        )

    private fun toContentRequest(site: SiteDtoV6): SiteContentRequest =
        SiteContentRequest(
            header = toHeaderRequest(site),
            mainAddress = addressDtoRequestMapperV6.toContentRequest(site.mainAddress)
        )

    private fun toHeaderRequest(site: SiteDtoV6): SiteHeaderRequest =
        SiteHeaderRequest(
            name = site.name,
            states = site.states.map { toStateRequest(it) },
            confidenceCriteria = toConfidenceRequest(site.confidenceCriteria),
            scriptVariants = emptyList()
        )

    private fun toStateRequest(state: SiteStateDtoV6): SiteState =
        SiteState(state.validFrom?.toUtcInstant(), state.validTo?.toUtcInstant(), state.type)

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDtoV6): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
