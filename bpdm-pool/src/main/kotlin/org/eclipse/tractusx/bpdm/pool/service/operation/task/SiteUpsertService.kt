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


package org.eclipse.tractusx.bpdm.pool.service.operation.task

import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteCreateWithReferencedAddressAsMainParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateOnLegalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SiteCreateWithReferencedAddressAsMainService
import org.eclipse.tractusx.bpdm.pool.service.operation.site.SitePayloadUpdateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Carries out the site a parsed golden record upsert states.
 */
@Service
class SiteUpsertService(
    private val siteCreateService: SiteCreateService,
    private val siteCreateWithReferencedAddressAsMainService: SiteCreateWithReferencedAddressAsMainService,
    private val sitePayloadUpdateService: SitePayloadUpdateService
) {

    /**
     * Writes what [site] states under [legalEntity] and reports the site it leaves behind, and whether that write changed it.
     */
    @Transactional
    fun upsert(site: SiteUpsertParsed, legalEntity: LegalEntityDb): UpsertResult<SiteDb> =
        when (site) {
            is SiteUpsertParsed.Unchanged -> UpsertResult(site.existingSite, UpsertType.NoChange)
            is SiteUpsertParsed.CreateWithOwnMainAddress ->
                UpsertResult(siteCreateService.create(listOf(SiteCreateParsed(legalEntity, site.content))).single(), UpsertType.Created)
            is SiteUpsertParsed.CreateOnLegalAddress ->
                UpsertResult(
                    siteCreateWithReferencedAddressAsMainService.create(
                        listOf(SiteCreateWithReferencedAddressAsMainParsed(legalEntity.legalAddress, site.header, mainAddressContent = null))
                    ).single(),
                    UpsertType.Created
                )
            is SiteUpsertParsed.CreateOnExistingAddress ->
                UpsertResult(
                    siteCreateWithReferencedAddressAsMainService.create(
                        listOf(SiteCreateWithReferencedAddressAsMainParsed(site.existingMainAddress, site.header, site.mainAddressContent))
                    ).single(),
                    UpsertType.Created
                )
            is SiteUpsertParsed.UpdateWithOwnMainAddress ->
                sitePayloadUpdateService.updateWithOwnMainAddress(listOf(SiteUpdateParsed(site.existingSite, site.content))).single()
            is SiteUpsertParsed.UpdateOnLegalAddress ->
                sitePayloadUpdateService.updateOnLegalAddress(
                    listOf(SiteUpdateOnLegalAddressParsed(site.existingSite, site.header))
                ).single()
        }
}
