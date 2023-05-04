/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.entity.Site
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SitePersistenceService(
    private val siteRepository: SiteRepository,
    //private val gateLegalEntityRepository: GateLegalEntityRepository
) {

    @Transactional
    fun persistSitesBP(sites: Collection<SiteGateInputRequest>) {

        //finds Legal Entity by External ID
        //val legalEntities = gateLegalEntityRepository.findDistinctByBpnIn(sites.map { it.legalEntityExternalId })

        //Finds Site in DB
        val externalIdColl: MutableCollection<String> = mutableListOf()
        sites.forEach { externalIdColl.add(it.externalId) }
        val siteRecord = siteRepository.findByExternalIdIn(externalIdColl)

        // for (legalEntity in legalEntities) {
        sites.forEach { site ->
            //if (legalEntity.bpn == site.legalEntityExternalId) {
            val fullSite = site.toSiteGate() //TODO (Needs to receive an Legal Entity)
            siteRecord.find { it.externalId == site.externalId }?.let { existingSite ->
                updateSite(existingSite, fullSite)
                siteRepository.save(existingSite)
            } ?: run {
                siteRepository.save(fullSite)
            }
            //}
        }
        //}
    }


    private fun updateSite(site: Site, updatedSite: Site) {

        site.bpn = updatedSite.bpn
        site.name = updatedSite.name
        site.externalId = updatedSite.externalId
        site.legalEntityExternalId = updatedSite.legalEntityExternalId
        //TODO (Needs LegalEntity Logic)
        site.states.replace(updatedSite.states)

    }

}