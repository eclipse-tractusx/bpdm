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
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class GoldenRecordConsistencyService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val poolApiClient: PoolApiClient,
    private val sharingStateService: SharingStateService,
    private val configProperties: GoldenRecordTaskConfigProperties,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
    private val sharingStateRepository: SharingStateRepository
) {

    private val logger = KotlinLogging.logger { }

    fun check(){
        logger.info { "Start checking business partner BPNs for consistency with the golden record pool..." }

        var currentPageNumber = 0
        var totalCheckBusinessPartners = 0
        lateinit var checkBatchResult: CheckResult

        do{
            checkBatchResult = transactionTemplate.execute { checkBatch(currentPageNumber, configProperties.consistencyCheck.batchSize) } ?: CheckResult(0, false)
            currentPageNumber++
            totalCheckBusinessPartners += checkBatchResult.checkedCount

            entityManager.clear()
        }while (checkBatchResult.hasMore)

        logger.debug { "Finished checking $totalCheckBusinessPartners for consistency with the golden record pool." }
    }


    private fun checkBatch(pageNumber: Int, pageSize: Int): CheckResult{
        val sharingStatePage = sharingStateRepository.findBySharingStateType(SharingStateType.Success, PageRequest.of(pageNumber, pageSize))
        val sharingStates = sharingStatePage.content
        val outputs = businessPartnerRepository.findBySharingStateInAndStage(sharingStates, StageType.Output)

        val outputsByBpnL = outputs.filter { it.bpnL != null }.associateBy { it.bpnL }
        val outputsByBpnS = outputs.filter { it.bpnS != null }.associateBy { it.bpnS }
        val outputsByBpnA = outputs.filter { it.bpnA != null }.associateBy { it.bpnA }

        val legalEntitiesToCheck = outputsByBpnL.keys.filterNotNull()
        val sitesToCheck = outputsByBpnS.keys.filterNotNull()
        val addressesToCheck = outputsByBpnA.keys.filterNotNull()

        val foundLegalEntities = if(legalEntitiesToCheck.isNotEmpty()) poolApiClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(bpnLs = legalEntitiesToCheck), PaginationRequest(0, legalEntitiesToCheck.size)).content else emptyList()
        val foundSites = if(sitesToCheck.isNotEmpty()) poolApiClient.sites.getSites(SiteSearchRequest(siteBpns = sitesToCheck), PaginationRequest(0, sitesToCheck.size)).content else emptyList()
        val foundAddresses = if(addressesToCheck.isNotEmpty()) poolApiClient.addresses.getAddresses(AddressSearchRequest(addressBpns = addressesToCheck), PaginationRequest(0, addressesToCheck.size)).content else emptyList()

        val foundBpnLs = foundLegalEntities.map { it.legalEntity.bpnl }.toSet()
        val foundBpnSs = foundSites.map { it.site.bpns }.toSet()
        val foundBpnAs = foundAddresses.map { it.bpna }.toSet()

        val missingLegalEntities = legalEntitiesToCheck.minus(foundBpnLs).toSet()
        val missingSites = sitesToCheck.minus(foundBpnSs).toSet()
        val missingAddresses = addressesToCheck.minus(foundBpnAs).toSet()

        outputs.forEach { output ->
            val missingBpnL = if(output.bpnL in missingLegalEntities) output.bpnL else null
            val missingBpnS = if(output.bpnS in missingSites) output.bpnS else null
            val missingBpnA = if(output.bpnA in missingAddresses) output.bpnA else null

            val missingBpns = listOfNotNull(missingBpnL, missingBpnS, missingBpnA)

            if(missingBpns.isNotEmpty())
                setConsistencyError(output, missingBpns)
        }

        return CheckResult(sharingStatePage.content.size, sharingStatePage.hasNext())
    }

    private fun setConsistencyError(businessPartner: BusinessPartnerDb, missingBpns: List<String>){
        val sharingState = businessPartner.sharingState
        val errorMessage = "Business partner with external-ID ${sharingState.externalId} references not existing golden record '${missingBpns.joinToString()}'"
        sharingStateService.setError(sharingState, sharingErrorCode = BusinessPartnerSharingError.BpnNotInPool, errorMessage)
    }

    private data class CheckResult(
        val checkedCount: Int,
        val hasMore: Boolean
    )
}