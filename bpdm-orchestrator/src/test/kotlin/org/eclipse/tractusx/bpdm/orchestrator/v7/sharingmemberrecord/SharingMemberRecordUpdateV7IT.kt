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

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.orchestrator.v7.UnscheduledOrchestratorTestBaseV7
import org.eclipse.tractusx.orchestrator.api.SharingMemberRecord
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class SharingMemberRecordUpdateV7IT: UnscheduledOrchestratorTestBaseV7() {

    /**
     * GIVEN existing sharing member record
     * WHEN user requests record to be golden record counted
     * THEN user sees sharing member record is now golden record counted
     */
    @Test
    fun `set sharing member record to is golden record counted`(){
        //GIVEN
        val sharingMemberRecordId = testDataClient.createBusinessPartnerTask(testName).recordId

        //WHEN
        val updatedSharingMemberRecord = orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(sharingMemberRecordId, true))

        //THEN
        val expectedSharingMemberRecord = SharingMemberRecord(sharingMemberRecordId, true, Instant.now(), Instant.now())

        assertRepo.assertSharingMemberRecordEqual(updatedSharingMemberRecord, expectedSharingMemberRecord)
    }

    /**
     * GIVEN existing sharing member record
     * WHEN user requests record to be NOT golden record counted
     * THEN user sees sharing member record is now NOT golden record counted
     */
    @Test
    fun `set sharing member record to is NOT golden record counted`(){
        //GIVEN
        val sharingMemberRecordId = testDataClient.createBusinessPartnerTask(testName).recordId

        //WHEN
        val updatedSharingMemberRecord = orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(sharingMemberRecordId, false))

        //THEN
        val expectedSharingMemberRecord = SharingMemberRecord(sharingMemberRecordId, false, Instant.now(), Instant.now())

        assertRepo.assertSharingMemberRecordEqual(updatedSharingMemberRecord, expectedSharingMemberRecord)
    }

    /**
     * GIVEN existing sharing member record that is golden record counted
     * WHEN user requests record to be NOT golden record counted
     * THEN user sees sharing member record is now NOT golden record counted
     */
    @Test
    fun `set sharing member record from is golden record counted to not counted`(){
        //GIVEN
        val sharingMemberRecordId = testDataClient.createBusinessPartnerTask(testName).recordId
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(sharingMemberRecordId, true))

        //WHEN
        val updatedSharingMemberRecord = orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(sharingMemberRecordId, false))

        //THEN
        val expectedSharingMemberRecord = SharingMemberRecord(sharingMemberRecordId, false, Instant.now(), Instant.now())

        assertRepo.assertSharingMemberRecordEqual(updatedSharingMemberRecord, expectedSharingMemberRecord)
    }

    /**
     * GIVEN existing sharing member record that is not golden record counted
     * WHEN user requests record to be golden record counted
     * THEN user sees sharing member record is now golden record counted
     */
    @Test
    fun `set sharing member record from not golden record counted to is counted`(){
        //GIVEN
        val sharingMemberRecordId = testDataClient.createBusinessPartnerTask(testName).recordId
        orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(sharingMemberRecordId, false))

        //WHEN
        val updatedSharingMemberRecord = orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest(sharingMemberRecordId, true))

        //THEN
        val expectedSharingMemberRecord = SharingMemberRecord(sharingMemberRecordId, true, Instant.now(), Instant.now())

        assertRepo.assertSharingMemberRecordEqual(updatedSharingMemberRecord, expectedSharingMemberRecord)
    }

    /**
     * WHEN user requests not existing record to be updated
     * THEN user sees 400 BAD REQUEST error response
     */
    @Test
    fun `try updating not existing sharing member record`(){
        //GIVEN
        testDataClient.createBusinessPartnerTask(testName)

        //WHEN
        val updateRequest: () -> Unit =  { orchestratorClient.sharingMemberRecords.update(SharingMemberRecordUpdateRequest("NOT EXISTING", true)) }

        //THEN
        Assertions.assertThatThrownBy(updateRequest).isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }
}