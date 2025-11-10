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

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.SyncType
import org.eclipse.tractusx.bpdm.pool.config.SharingMemberRecordSyncConfigProperties
import org.eclipse.tractusx.bpdm.pool.repository.SyncRecordRepository
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordQueryRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class SharingMemberRecordSyncService(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val syncRecordService: SyncRecordService,
    private val transactionTemplate: TransactionTemplate,
    private val sharingMemberConfidenceService: SharingMemberConfidenceService,
    private val syncRecordRepository: SyncRecordRepository
){
    @Scheduled(cron = "#{${SharingMemberRecordSyncConfigProperties.GET_CRON}}", zone = "UTC")
    fun synchronize(){
        var hasMore = false
        do{
            transactionTemplate.execute {
                hasMore = synchronizeBatch()
            }
        }while(hasMore)
    }

    private fun synchronizeBatch(): Boolean{
        val syncRecord = syncRecordService.getOrCreateRecord(SyncType.SHARING_MEMBER_RECORDS)

        val queryRequest = SharingMemberRecordQueryRequest(syncRecord.fromTime)
        val updatedSharingMemberRecords = orchestrationApiClient.sharingMemberRecords.queryRecords(queryRequest, PaginationRequest())

        updatedSharingMemberRecords.content
            .forEach { sharingMemberConfidenceService.updateGoldenRecordCounted(it.recordId, it.isGoldenRecordCounted) }

        syncRecord.fromTime =  updatedSharingMemberRecords.content.lastOrNull()?.updatedAt ?: syncRecord.fromTime
        syncRecordRepository.save(syncRecord)

        return updatedSharingMemberRecords.totalElements > updatedSharingMemberRecords.contentSize
    }

}