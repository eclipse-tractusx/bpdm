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

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class GoldenRecordCountedService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val orchestrationApiClient: OrchestrationApiClient,
    private val sharingStateRepository: SharingStateRepository,
    private val transactionTemplate: TransactionTemplate,
    private  val entityManager: EntityManager,
    private val goldenRecordTaskConfigProperties: GoldenRecordTaskConfigProperties
) {

    private val logger = KotlinLogging.logger { }

    fun synchronizeGoldenRecordCounted(){
        var hasMore = false
        do{
            transactionTemplate.execute {
                val unsyncedSharingStatePage = sharingStateRepository.findByIsGoldenRecordCountedUnsynced(PageRequest.ofSize(goldenRecordTaskConfigProperties.recordSync.batchSize))
                val unsyncedSharingStates = unsyncedSharingStatePage.content
                unsyncedSharingStates.forEach { sharingState ->
                    val isGoldenRecordCounted = sharingState.isGoldenRecordCounted
                    if(isGoldenRecordCounted != null){
                        val updateRequest = SharingMemberRecordUpdateRequest(sharingState.orchestratorRecordId.toString(), isGoldenRecordCounted)
                        orchestrationApiClient.sharingMemberRecords.update(updateRequest)
                    }else{
                        logger.warn("Sharing member record '${sharingState.externalId}' isGoldenRecordCounted is NULL but does not match the previously synced state. Skipping synchronization for that record.")
                    }

                    sharingState.syncedIsGoldenRecordCounted = isGoldenRecordCounted
                    sharingStateRepository.save(sharingState)
                }

                hasMore = unsyncedSharingStatePage.hasNext()
            }
            entityManager.clear()
        }while(hasMore)

    }


    @Transactional
    fun setIsGoldenRecordCounted(businessPartners: List<BusinessPartnerDb>){
        val bpnAs = businessPartners.map { it.bpnA!! }
        val allCandidates = businessPartnerRepository.findByStageAndBpnAIn(StageType.Output, bpnAs)

        val candidateBpnGroups = allCandidates.groupBy { it.bpnA!! }

        businessPartners.forEach { businessPartner ->
            val duplicates = candidateBpnGroups[businessPartner.bpnA!!] ?: emptyList()
            setIsGoldenRecordCounted(businessPartner, duplicates)
        }
    }

    private fun setIsGoldenRecordCounted(businessPartner: BusinessPartnerDb, duplicates: List<BusinessPartnerDb>){

        val duplicateSet = duplicates.plus(businessPartner).toSet()
        val sortedDuplicates = duplicateSet.sortedBy { it.createdAt }
        val oldestDuplicate =  sortedDuplicates.first().sharingState
        val otherDuplicates = sortedDuplicates.drop(1).map { it.sharingState }

        setIsGoldenRecordCounted(oldestDuplicate, true)
        otherDuplicates.forEach{ setIsGoldenRecordCounted(it, false) }
    }

    private fun setIsGoldenRecordCounted(sharingState: SharingStateDb, isGoldenRecordCounted: Boolean){
        sharingState.isGoldenRecordCounted = isGoldenRecordCounted
        sharingStateRepository.save(sharingState)
    }

}