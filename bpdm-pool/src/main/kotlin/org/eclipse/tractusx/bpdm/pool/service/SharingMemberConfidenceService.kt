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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.repository.SharingMemberRecordRepository
import org.springframework.stereotype.Service

@Service
class SharingMemberConfidenceService(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository,
    private val logisticAddressRepository: LogisticAddressRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService,
) {

    fun updateGoldenRecordCounted(recordId: String, isGoldenRecordCounted: Boolean?): SharingMemberRecordDb?{
        val foundSharingMemberRecord = sharingMemberRecordRepository.findByRecordId(recordId)
        if(foundSharingMemberRecord != null){
            if(foundSharingMemberRecord.isGoldenRecordCounted != isGoldenRecordCounted){
                foundSharingMemberRecord.isGoldenRecordCounted = isGoldenRecordCounted
                sharingMemberRecordRepository.save(foundSharingMemberRecord)
                updateNumberOfSharingMembers(foundSharingMemberRecord.address)
            }
        }
        return foundSharingMemberRecord
    }

    fun updateAddress(recordId: String, addressBpn: String): Result{
        val address = logisticAddressRepository.findByBpn(addressBpn)!!
        val existingSharingMemberRecord = sharingMemberRecordRepository.findByRecordId(recordId)
        val previousAddress = existingSharingMemberRecord?.address
        val hasChanges = previousAddress != address

        val sharingMemberRecord = existingSharingMemberRecord?.apply {
            this.address = address
        } ?: SharingMemberRecordDb(recordId, null, address)

        sharingMemberRecordRepository.saveAndFlush(sharingMemberRecord)

        val businessPartnerUpdateResults = if(hasChanges){
            val addressResults = updateNumberOfSharingMembers(address)
            val previousAddressResults = previousAddress?.let { updateNumberOfSharingMembers(previousAddress) }
                ?: Result(emptyList(), emptyList())

            Result(
                addressResults.updatedAddresses + previousAddressResults.updatedAddresses,
                addressResults.updatedLegalEntities + previousAddressResults.updatedLegalEntities
            )
        }else{
            Result(emptyList(), emptyList())
        }

        return businessPartnerUpdateResults
    }

    private fun updateNumberOfSharingMembers(address: LogisticAddressDb): Result{
        val newNumberOfSharingMembers = countSharingMembers(address)
        val addressHasChanges = address.confidenceCriteria.numberOfBusinessPartners != newNumberOfSharingMembers

        if(addressHasChanges){
            address.confidenceCriteria = address.confidenceCriteria.copy(numberOfBusinessPartners = newNumberOfSharingMembers)
            logisticAddressRepository.save(address)
            changelogService.createChangelogEntry(ChangelogEntryCreateRequest(address.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        }

        val legalEntity = address.legalEntity!!
        if(legalEntity.legalAddress == address){
            val legalEntityHasChanges = legalEntity.confidenceCriteria.numberOfBusinessPartners != newNumberOfSharingMembers
            if(legalEntityHasChanges){
                legalEntity.confidenceCriteria = legalEntity.confidenceCriteria.copy(numberOfBusinessPartners = newNumberOfSharingMembers)
                legalEntityRepository.save(legalEntity)
                changelogService.createChangelogEntry(ChangelogEntryCreateRequest(legalEntity.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))
            }
        }

        return Result(listOf(address), if(legalEntity.legalAddress == address) listOf(legalEntity) else emptyList())
    }

    fun countSharingMembers(address: LogisticAddressDb): Int{
        val sharingMemberRecords = sharingMemberRecordRepository.findByAddress(address)
        return sharingMemberRecords.count{ it.isGoldenRecordCounted ?: false }
    }

    data class Result(
        val updatedAddresses: List<LogisticAddressDb>,
        val updatedLegalEntities: List<LegalEntityDb>
    )
}