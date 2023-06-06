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

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SitePersistenceService(
    private val siteRepository: SiteRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val addressRepository: GateAddressRepository
) {

    @Transactional
    fun persistSitesBP(sites: Collection<SiteGateInputRequest>) {

        //Finds Site in DB
        val externalIdColl: MutableCollection<String> = mutableListOf()
        sites.forEach { externalIdColl.add(it.externalId) }
        val siteRecord = siteRepository.findByExternalIdIn(externalIdColl)

        sites.forEach { site ->

            val legalEntityRecord =
                site.legalEntityExternalId.let {
                    legalEntityRepository.findByExternalId(site.legalEntityExternalId) ?: throw BpdmNotFoundException("Business Partner", it)
                }

            val fullSite = site.toSiteGate(legalEntityRecord)

            siteRecord.find { it.externalId == site.externalId }?.let { existingSite ->

                val logisticAddressRecord =
                    addressRepository.findByExternalId(getMainAddressForSiteExternalId(site.externalId)) ?: throw BpdmNotFoundException(
                        "Business Partner",
                        "Error"
                    )

                updateAddress(logisticAddressRecord, fullSite.mainAddress)

                updateSite(existingSite, site, legalEntityRecord)

                siteRepository.save(existingSite)
            } ?: run {
                siteRepository.save(fullSite)
            }
        }
    }

    private fun updateSite(site: Site, updatedSite: SiteGateInputRequest, legalEntityRecord: LegalEntity) {

        site.name = updatedSite.site.name
        site.externalId = updatedSite.externalId
        site.legalEntity = legalEntityRecord
        site.states.replace(updatedSite.site.states.map { toEntityAddress(it, site) })

    }

    private fun updateAddress(address: LogisticAddress, changeAddress: LogisticAddress) {

        address.name = changeAddress.name
        address.externalId = changeAddress.externalId
        address.legalEntity = changeAddress.legalEntity
        address.siteExternalId = changeAddress.siteExternalId
        address.physicalPostalAddress = changeAddress.physicalPostalAddress
        address.alternativePostalAddress = changeAddress.alternativePostalAddress

        address.identifiers.replace(changeAddress.identifiers.map { toEntityIdentifier(it, address) })
        address.states.replace(changeAddress.states.map { toEntityAddress(it, address) })

    }

    fun toEntityAddress(dto: AddressState, address: LogisticAddress): AddressState {
        return AddressState(dto.description, dto.validFrom, dto.validTo, dto.type, address)
    }

    fun toEntityIdentifier(dto: AddressIdentifier, address: LogisticAddress): AddressIdentifier {
        return AddressIdentifier(dto.value, dto.type, address)
    }

}