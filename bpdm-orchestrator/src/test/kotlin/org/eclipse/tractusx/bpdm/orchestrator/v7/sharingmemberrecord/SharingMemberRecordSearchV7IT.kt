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

package org.eclipse.tractusx.bpdm.orchestrator.v7.sharingmemberrecord

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordQueryRequest
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.junit.jupiter.api.Test
import java.time.Instant

class SharingMemberRecordSearchV7IT: UnscheduledOrchestratorTestBaseV7() {

    private val beforeTestingTime = Instant.now()

    /**
     * GIVEN sharing member record with undecided whether it is golden record counted or not
     * WHEN user searches for records
     * THEN user sees is record with NULL for is golden record counted
     */
    @Test
    fun `search sharing member record with undecided golden record counted`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val searchResult = orchestratorClient.sharingMemberRecords.queryRecords(SharingMemberRecordQueryRequest(beforeTestingTime), PaginationRequest())

        //THEN
        //Get sharing member record ID for golden record process services
        val sharingMemberRecordId = testDataClient.reserveBusinessPartnerTask(createdTask).recordId
        val expectedResult = PageDto(1, 1, 0, 1, listOf(SharingMemberRecord(sharingMemberRecordId, null, Instant.now(), Instant.now())))

        assertRepo.assertSharingMemberRecordEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN sharing member record which is golden record counted
     * WHEN user searches for records
     * THEN user sees is record is golden record counted
     */
    @Test
    fun `search sharing member record with is golden record counted`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(createdTask.recordId, true))

        //WHEN
        val searchResult = orchestratorClient.sharingMemberRecords.queryRecords(SharingMemberRecordQueryRequest(beforeTestingTime), PaginationRequest())

        //THEN
        //Get sharing member record ID for golden record process services
        val sharingMemberRecordId = testDataClient.reserveBusinessPartnerTask(createdTask).recordId
        val expectedResult = PageDto(1, 1, 0, 1, listOf(SharingMemberRecord(sharingMemberRecordId, true, Instant.now(), Instant.now())))

        assertRepo.assertSharingMemberRecordEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN sharing member record which is NOT golden record counted
     * WHEN user searches for records
     * THEN user sees is record is NOT golden record counted
     */
    @Test
    fun `search sharing member record with is NOT golden record counted`(){
        //GIVEN
        val createdTask = testDataClient.createBusinessPartnerTask(testName)
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(createdTask.recordId, false))

        //WHEN
        val searchResult = orchestratorClient.sharingMemberRecords.queryRecords(SharingMemberRecordQueryRequest(beforeTestingTime), PaginationRequest())

        //THEN
        //Get sharing member record ID for golden record process services
        val sharingMemberRecordId = testDataClient.reserveBusinessPartnerTask(createdTask).recordId
        val expectedResult = PageDto(1, 1, 0, 1, listOf(SharingMemberRecord(sharingMemberRecordId, false, Instant.now(), Instant.now())))

        assertRepo.assertSharingMemberRecordEqual(searchResult, expectedResult)
    }

    /**
     * GIVEN sharing member records with one record updated after time X
     * WHEN user searches for only records updated after time X
     * THEN user sees is only records updated after time X
     */
    @Test
    fun `search sharing member records updated after time X`(){
        //GIVEN
        val createdTask1 = testDataClient.createBusinessPartnerTask(testName)
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(createdTask1.recordId, true))

        val createdTask2 = testDataClient.createBusinessPartnerTask(testName)
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(createdTask2.recordId, true))

        val timeX = Instant.now()

        val createdTask3 = testDataClient.createBusinessPartnerTask(testName)
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(createdTask3.recordId, true))

        //WHEN
        val searchResult = orchestratorClient.sharingMemberRecords.queryRecords(SharingMemberRecordQueryRequest(timeX), PaginationRequest())

        //THEN
        //Get sharing member record ID for golden record process services
        val sharingMemberRecordId = testDataClient.reserveBusinessPartnerTasks(createdTask3.processingState.step).reservedTasks.find { it.taskId == createdTask3.taskId }!!.recordId
        val expectedResult = PageDto(1, 1, 0, 1, listOf(SharingMemberRecord(sharingMemberRecordId, true, Instant.now(), Instant.now())))

        assertRepo.assertSharingMemberRecordEqual(searchResult, expectedResult)
    }
}