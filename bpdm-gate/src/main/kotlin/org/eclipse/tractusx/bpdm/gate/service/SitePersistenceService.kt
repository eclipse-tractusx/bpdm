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
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.SiteGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.gate.repository.SiteRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class SitePersistenceService(
    private val siteRepository: SiteRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val addressRepository: GateAddressRepository,
    private val changelogRepository: ChangelogRepository
) {

    @Transactional
    fun persistSitesBP(sites: Collection<SiteGateInputRequest>, datatype: OutputInputEnum) {

        //Finds Site in DB
        val siteRecord = siteRepository.findByExternalIdIn(sites.map { it.externalId })

        sites.forEach { site ->

            val legalEntityRecord = getLegalEntityRecord(site.legalEntityExternalId, datatype)

            val fullSite = site.toSiteGate(legalEntityRecord, datatype)

            siteRecord.find { it.externalId == site.externalId && it.dataType == datatype }?.let { existingSite ->

                val logisticAddressRecord = getAddressRecord(getMainAddressForSiteExternalId(site.externalId), datatype)

                updateAddress(logisticAddressRecord, fullSite.mainAddress)
                updateSite(existingSite, site, legalEntityRecord)
                siteRepository.save(existingSite)
                saveChangelog(site)
            } ?: run {
                siteRepository.save(fullSite)
                saveChangelog(site)
            }
        }
    }

    //Creates Changelog For both Site and Logistic Address when they are created or updated
    private fun saveChangelog(site: SiteGateInputRequest) {
        changelogRepository.save(ChangelogEntry(getMainAddressForSiteExternalId(site.externalId), LsaType.ADDRESS))
        changelogRepository.save(ChangelogEntry(site.externalId, LsaType.SITE))
    }

    private fun getAddressRecord(externalId: String, datatype: OutputInputEnum): LogisticAddress {
        return addressRepository.findByExternalIdAndDataType(externalId, datatype)
            ?: throw BpdmNotFoundException("Business Partner", "Error")
    }

    private fun getLegalEntityRecord(externalId: String, datatype: OutputInputEnum): LegalEntity {
        return legalEntityRepository.findByExternalIdAndDataType(externalId, datatype)
            ?: throw BpdmNotFoundException("Business Partner", externalId)
    }

    private fun updateSite(site: Site, updatedSite: SiteGateInputRequest, legalEntityRecord: LegalEntity) {

        site.name = updatedSite.site.nameParts.firstOrNull() ?: ""
        site.externalId = updatedSite.externalId
        site.legalEntity = legalEntityRecord
        site.states.replace(updatedSite.site.states.map { toEntityAddress(it, site) })

    }

    private fun updateAddress(address: LogisticAddress, changeAddress: LogisticAddress) {

        address.name = changeAddress.name
        address.externalId = changeAddress.externalId
        address.legalEntity = changeAddress.legalEntity
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

    @Transactional
    fun persistSitesOutputBP(sites: Collection<SiteGateOutputRequest>, datatype: OutputInputEnum) {

        //Finds Site in DB
        val siteRecord = siteRepository.findByExternalIdIn(sites.map { it.externalId })

        sites.forEach { site ->

            val legalEntityRecord = getLegalEntityRecord(site.legalEntityExternalId, datatype)

            val fullSite = site.toSiteGate(legalEntityRecord, datatype)

            siteRecord.find { it.externalId == site.externalId && it.dataType == datatype }?.let { existingSite ->

                val logisticAddressRecord = getAddressRecord(getMainAddressForSiteExternalId(site.externalId), datatype)

                updateAddress(logisticAddressRecord, fullSite.mainAddress)
                updateSiteOutput(existingSite, site, legalEntityRecord)
                siteRepository.save(existingSite)
            } ?: run {
                if (siteRecord.find { it.externalId == fullSite.externalId && it.dataType == OutputInputEnum.Input } == null) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Input Site doesn't exist")
                } else {
                    siteRepository.save(fullSite)
                }
            }
        }
    }

    private fun updateSiteOutput(site: Site, updatedSite: SiteGateOutputRequest, legalEntityRecord: LegalEntity) {

        site.bpn = updatedSite.bpn
        site.name = updatedSite.site.nameParts.firstOrNull() ?: ""
        site.externalId = updatedSite.externalId
        site.legalEntity = legalEntityRecord
        site.states.replace(updatedSite.site.states.map { toEntityAddress(it, site) })

    }
}