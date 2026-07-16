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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.request.SiteContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithLegalAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteCreateWithReferencedAddressAsMainRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.SiteState
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpdateRequest
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.pool.model.request.SiteScriptVariant as SiteScriptVariantRequest
import org.eclipse.tractusx.orchestrator.api.model.BusinessState as TaskBusinessState
import org.eclipse.tractusx.orchestrator.api.model.PostalAddress as TaskPostalAddress
import org.eclipse.tractusx.orchestrator.api.model.PostalAddressScriptVariantWithScriptCode as TaskScriptVariant
import org.eclipse.tractusx.orchestrator.api.model.Site as TaskSite

/**
 * Maps a cleaning task's site into the loose [SiteCreateRequest] / [SiteUpdateRequest]; the main address is delegated to
 * [GoldenRecordTaskAddressRequestMapper] and resolved by the caller (the site's own address, or the legal address when
 * the site main is the legal address). Unlike the pass-through elsewhere, a missing [SiteState] type throws here — the
 * loose request's type is non-null by contract.
 */
@Component
class GoldenRecordTaskSiteRequestMapper(
    private val addressRequestMapper: GoldenRecordTaskAddressRequestMapper
) {

    fun toCreateRequest(legalEntityBpn: String, site: TaskSite, mainAddress: TaskPostalAddress): SiteCreateRequest =
        SiteCreateRequest(legalEntityBpn = legalEntityBpn, content = toContentRequest(site, mainAddress))

    fun toUpdateRequest(siteBpn: String, site: TaskSite, mainAddress: TaskPostalAddress): SiteUpdateRequest =
        SiteUpdateRequest(siteBpn = siteBpn, content = toContentRequest(site, mainAddress))

    fun toCreateWithLegalAddressAsMainRequest(legalEntityBpn: String, site: TaskSite): SiteCreateWithLegalAddressAsMainRequest =
        SiteCreateWithLegalAddressAsMainRequest(legalEntityBpn = legalEntityBpn, header = toHeaderRequest(site))

    fun toCreateWithReferencedAddressAsMainRequest(mainAddressBpn: String, site: TaskSite, mainAddress: TaskPostalAddress): SiteCreateWithReferencedAddressAsMainRequest =
        SiteCreateWithReferencedAddressAsMainRequest(mainAddressBpn = mainAddressBpn, content = toContentRequest(site, mainAddress))

    private fun toContentRequest(site: TaskSite, mainAddress: TaskPostalAddress): SiteContentRequest =
        SiteContentRequest(
            header = toHeaderRequest(site),
            mainAddress = addressRequestMapper.toContentRequest(
                mainAddress,
                site.scriptVariants.map { TaskScriptVariant(it.scriptCode, it.mainAddress) }
            )
        )

    private fun toHeaderRequest(site: TaskSite): SiteHeaderRequest =
        SiteHeaderRequest(
            name = site.siteName,
            states = site.states.map { toState(it) },
            confidenceCriteria = addressRequestMapper.toConfidenceRequest(site.confidenceCriteria),
            scriptVariants = site.scriptVariants.map { SiteScriptVariantRequest(it.scriptCode, it.siteName) }
        )

    private fun toState(state: TaskBusinessState): SiteState =
        SiteState(
            validFrom = state.validFrom,
            validTo = state.validTo,
            type = state.type ?: throw BpdmValidationException("Business Partner state type is null")
        )
}
