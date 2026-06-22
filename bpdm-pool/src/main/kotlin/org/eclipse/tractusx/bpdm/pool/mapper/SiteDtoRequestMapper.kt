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

package org.eclipse.tractusx.bpdm.pool.mapper

import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.SiteContentRequest
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.SiteScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.SiteState
import org.eclipse.tractusx.bpdm.pool.model.SiteUpdateRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Pure translation of a Pool API site request into the loose domain [SiteCreateRequest] / [SiteUpdateRequest] consumed by
 * the site services. No validation happens here — that is the site service's `parse`. The main address (and its localized
 * script variants, carried per-variant on the site) is delegated to [LogisticAddressDtoRequestMapper]; the Pool-computed
 * confidence values are dropped there. Boundary time fields are converted from the API's `LocalDateTime` (UTC) to the
 * domain's `Instant`.
 */
@Component
class SiteDtoRequestMapper(
    private val logisticAddressDtoRequestMapper: LogisticAddressDtoRequestMapper
) {

    fun toCreateRequest(request: SitePartnerCreateRequest): SiteCreateRequest =
        SiteCreateRequest(legalEntityBpn = request.bpnlParent, content = toContentRequest(request.site))

    fun toUpdateRequest(request: SitePartnerUpdateRequest): SiteUpdateRequest =
        SiteUpdateRequest(siteBpn = request.bpns, content = toContentRequest(request.site))

    private fun toContentRequest(site: SiteDto): SiteContentRequest =
        SiteContentRequest(
            header = toHeaderRequest(site),
            mainAddress = logisticAddressDtoRequestMapper.toContentRequest(
                site.mainAddress,
                site.scriptVariants.map { LogisticAddressScriptVariantDto(it.scriptCode, it.mainAddress) }
            )
        )

    private fun toHeaderRequest(site: SiteDto): SiteHeaderRequest =
        SiteHeaderRequest(
            name = site.name,
            states = site.states.map { SiteState(it.validFrom?.toUtcInstant(), it.validTo?.toUtcInstant(), it.type) },
            confidenceCriteria = toConfidenceRequest(site.confidenceCriteria),
            scriptVariants = site.scriptVariants.map { SiteScriptVariant(it.scriptCode, it.name) }
        )

    private fun toConfidenceRequest(confidence: ConfidenceCriteriaDto): ConfidenceCriteriaRequest =
        ConfidenceCriteriaRequest(
            sharedByOwner = confidence.sharedByOwner,
            checkedByExternalDataSource = confidence.checkedByExternalDataSource,
            lastConfidenceCheckAt = confidence.lastConfidenceCheckAt.toUtcInstant(),
            nextConfidenceCheckAt = confidence.nextConfidenceCheckAt.toUtcInstant()
        )

    private fun LocalDateTime.toUtcInstant() = toInstant(ZoneOffset.UTC)
}
