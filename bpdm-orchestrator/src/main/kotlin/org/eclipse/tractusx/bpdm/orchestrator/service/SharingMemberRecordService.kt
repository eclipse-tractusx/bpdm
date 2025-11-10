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

package org.eclipse.tractusx.bpdm.orchestrator.service

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageDto
import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordIdNotValid
import org.eclipse.tractusx.bpdm.orchestrator.exception.BpdmRecordNotFoundException
import org.eclipse.tractusx.bpdm.orchestrator.repository.SharingMemberRecordRepository
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordQueryRequest
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class SharingMemberRecordService(
    private val sharingMemberRecordRepository: SharingMemberRecordRepository
) {

    fun queryRecords(request: SharingMemberRecordQueryRequest, paginationRequest: PaginationRequest): PageDto<SharingMemberRecord>{
        val pageable = PageRequest.of(paginationRequest.page, paginationRequest.size)
        val recordPage = sharingMemberRecordRepository.findByUpdatedAtAfter(request.timestampAfter, pageable)

        return recordPage.toPageDto { toPublicDto(it) }
    }

    fun updateRecord(request: SharingMemberRecordUpdateRequest): SharingMemberRecord{
        val uuid = toUUID(request.recordId)
        val sharingMemberRecord = sharingMemberRecordRepository.findByPrivateIdIn(setOf(uuid))
            .firstOrNull() ?: throw BpdmRecordNotFoundException(listOf(uuid))

        val hasChanges = sharingMemberRecord.isGoldenRecordCounted != request.isGoldenRecordCounted
        if(hasChanges){
            sharingMemberRecord.isGoldenRecordCounted = request.isGoldenRecordCounted
            sharingMemberRecord.updatedAt = Instant.now()
            sharingMemberRecordRepository.save(sharingMemberRecord)
        }

        return toPrivateDto(sharingMemberRecord)
    }

     fun getOrCreateGateRecords(requests: List<TaskCreateRequestEntry>): List<SharingMemberRecordDb> {
         val privateIds = requests.map { request -> request.recordId?.let { toUUID(it) } }
        val notNullPrivateIds = privateIds.filterNotNull()

        val foundRecords = sharingMemberRecordRepository.findByPrivateIdIn(notNullPrivateIds.toSet())
        val foundRecordsByPrivateId = foundRecords.associateBy { it.privateId }
        val requestedNotFoundRecords = notNullPrivateIds.minus(foundRecordsByPrivateId.keys)

        if (requestedNotFoundRecords.isNotEmpty())
            throw BpdmRecordNotFoundException(requestedNotFoundRecords)

        return privateIds.map { privateId ->
            val gateRecord = privateId?.let { foundRecordsByPrivateId[it] } ?: SharingMemberRecordDb(publicId = UUID.randomUUID(), privateId = UUID.randomUUID(), isGoldenRecordCounted = null)
            sharingMemberRecordRepository.save(gateRecord)
        }
    }

    private fun toUUID(uuidString: String) =
        try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw BpdmRecordIdNotValid(uuidString)
        }


    private fun toPrivateDto(entity: SharingMemberRecordDb): SharingMemberRecord {
        return toDto(entity, entity.privateId.toString())
    }

    private fun toPublicDto(entity: SharingMemberRecordDb): SharingMemberRecord{
        return toDto(entity, entity.publicId.toString())
    }

    private fun toDto(entity: SharingMemberRecordDb, recordId: String): SharingMemberRecord{
        return SharingMemberRecord(
            recordId,
            entity.isGoldenRecordCounted,
            entity.createdAt,
            entity.updatedAt
        )
    }
}