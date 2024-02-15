/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.SiteGateOutputRequest
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
    private val changelogRepository: ChangelogRepository,
    private val sharingStateService: SharingStateService
) {

    @Transactional
    fun persistSitesBP(sites: Collection<SiteGateInputRequest>, datatype: StageType) {

        //Finds Site in DB
        val siteRecord = siteRepository.findByExternalIdIn(sites.map { it.externalId })

        sites.forEach { site ->

            val legalEntityRecord = getLegalEntityRecord(site.legalEntityExternalId, datatype)

            val fullSite = site.toSiteGate(legalEntityRecord, datatype)

            siteRecord.find { it.externalId == site.externalId && it.stage == datatype }
                ?.let { existingSite ->
                    val logisticAddressRecord = getAddressRecord(getMainAddressExternalIdForSiteExternalId(site.externalId), datatype)

                    updateAddress(logisticAddressRecord, fullSite.mainAddress)
                    updateSite(existingSite, site, legalEntityRecord)
                    siteRepository.save(existingSite)
                    saveChangelog(site.externalId, ChangelogType.UPDATE, datatype)
                }
                ?: run {
                    siteRepository.save(fullSite)
                    saveChangelog(site.externalId, ChangelogType.CREATE, datatype)
                    sharingStateService.upsertSharingState(site.toSharingStateDTO())
                }
        }

        val initRequests = sites.map { SharingStateService.SharingStateIdentifierDto(it.externalId, BusinessPartnerType.SITE ) }
        sharingStateService.setInitial(initRequests)
    }

    //Creates Changelog For both Site and Logistic Address when they are created or updated
    private fun saveChangelog(externalId: String, changelogType: ChangelogType, stage: StageType) {
        val mainAddressExternalId = getMainAddressExternalIdForSiteExternalId(externalId)
        changelogRepository.save(ChangelogEntry(mainAddressExternalId, BusinessPartnerType.ADDRESS, changelogType, stage))
        changelogRepository.save(ChangelogEntry(externalId, BusinessPartnerType.SITE, changelogType, stage))
    }

    private fun getAddressRecord(externalId: String, datatype: StageType): LogisticAddress {
        return addressRepository.findByExternalIdAndStage(externalId, datatype)
            ?: throw BpdmNotFoundException("Business Partner", "Error")
    }

    private fun getLegalEntityRecord(externalId: String, datatype: StageType): LegalEntity {
        return legalEntityRepository.findByExternalIdAndStage(externalId, datatype)
            ?: throw BpdmNotFoundException("Business Partner", externalId)
    }

    private fun updateSite(site: Site, updatedSite: SiteGateInputRequest, legalEntityRecord: LegalEntity) {

        site.externalId = updatedSite.externalId
        site.legalEntity = legalEntityRecord

        site.states.replace(updatedSite.site.states.map { toEntityAddress(it, site) })
        site.nameParts.replace(updatedSite.site.nameParts.map { toNameParts(it, null, site, null) })
        site.roles.replace(updatedSite.site.roles.distinct().map { toRoles(it, null, site, null) })

    }

    private fun updateAddress(address: LogisticAddress, changeAddress: LogisticAddress) {

        address.externalId = changeAddress.externalId
        address.legalEntity = changeAddress.legalEntity
        address.physicalPostalAddress = changeAddress.physicalPostalAddress
        address.alternativePostalAddress = changeAddress.alternativePostalAddress
        address.identifiers.replace(changeAddress.identifiers.map { toEntityAddressIdentifiers(it.mapToAddressIdentifiersDto(), address) })
        address.states.replace(changeAddress.states.map { toEntityAddress(it, address) })
        address.nameParts.replace(changeAddress.nameParts.map { toNameParts(it.namePart, address, null, null) })
        address.roles.replace(changeAddress.roles.distinct().map { toRoles(it.roleName, null, null, address) })

    }

    fun toEntityAddress(dto: AddressState, address: LogisticAddress): AddressState {
        return AddressState(dto.description, dto.validFrom, dto.validTo, dto.type, address)
    }

    @Transactional
    fun persistSitesOutputBP(sites: Collection<SiteGateOutputRequest>, datatype: StageType) {

        //Finds Site in DB
        val siteRecord = siteRepository.findByExternalIdIn(sites.map { it.externalId })

        sites.forEach { site ->

            val legalEntityRecord = getLegalEntityRecord(site.legalEntityExternalId, datatype)

            val fullSite = site.toSiteGate(legalEntityRecord, datatype)

            siteRecord.find { it.externalId == site.externalId && it.stage == datatype }
                ?.let { existingSite ->
                    val logisticAddressRecord = getAddressRecord(getMainAddressExternalIdForSiteExternalId(site.externalId), datatype)

                    updateAddress(logisticAddressRecord, fullSite.mainAddress)
                    updateSiteOutput(existingSite, site, legalEntityRecord)
                    siteRepository.save(existingSite)
                    saveChangelog(site.externalId, ChangelogType.UPDATE, datatype)
                }
                ?: run {
                    if (siteRecord.find { it.externalId == fullSite.externalId && it.stage == StageType.Input } == null) {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Input Site doesn't exist")
                    } else {
                        siteRepository.save(fullSite)
                        saveChangelog(site.externalId, ChangelogType.CREATE, datatype)
                    }
                }
        }

        val successRequests = sites.map {
            SharingStateService.SuccessRequest(
                SharingStateService.SharingStateIdentifierDto(it.externalId, BusinessPartnerType.SITE),
                it.bpn
            )
        }
        sharingStateService.setSuccess(successRequests)
    }

    private fun updateSiteOutput(site: Site, updatedSite: SiteGateOutputRequest, legalEntityRecord: LegalEntity) {

        site.bpn = updatedSite.bpn
        site.externalId = updatedSite.externalId
        site.legalEntity = legalEntityRecord

        site.states.replace(updatedSite.site.states.map { toEntityAddress(it, site) })
        site.nameParts.replace(updatedSite.site.nameParts.map { toNameParts(it, null, site, null) })
        site.roles.replace(updatedSite.site.roles.distinct().map { toRoles(it, null, site, null) })

    }
}