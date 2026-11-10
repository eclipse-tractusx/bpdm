/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.application

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.v7.client.PoolV7Client
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockConfiguration
import org.eclipse.tractusx.bpdm.test.containers.OrchestratorMockContextInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.sharingmember.SharingMemberRecord
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReference
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BpnReferenceType
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartner
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.ConfidenceCriteria
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepReservationEntryDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class],
    properties = ["bpdm.tasks.cron=-", "bpdm.sharingMemberRecords.cron=-"]
)
@ActiveProfiles("test-no-auth")
@Import(OrchestratorMockConfiguration::class)
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class, OrchestratorMockContextInitializer::class])
class SharingMemberRecordSyncApplicationServiceTest @Autowired constructor(
    val syncService: SharingMemberRecordSyncApplicationService,
    val taskResolutionService: GoldenRecordTaskResolutionApplicationService,
    val orchestratorMockDataFactory: OrchestratorMockDataFactory,
    val poolClient: PoolV7Client,
    val dbTestHelpers: DbTestHelpers
) {

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        orchestratorMockDataFactory.resetRecordedRequests()
    }

    @Test
    fun `record counted by the Orchestrator counts as sharing member of its legal entity and legal address`() {
        val bpnL = createLegalEntityShared(RECORD_ID)

        orchestratorMockDataFactory.mockSharingMemberRecords(listOf(sharingMemberRecord(RECORD_ID, isGoldenRecordCounted = true)))
        syncService.synchronize()

        val legalEntity = poolClient.legalEntities.getLegalEntity(bpnL)
        assertThat(legalEntity.header.confidenceCriteria.numberOfSharingMembers).isEqualTo(1)
        assertThat(legalEntity.legalAddress.confidenceCriteria.numberOfSharingMembers).isEqualTo(1)
    }

    @Test
    fun `record no longer counted by the Orchestrator stops counting as sharing member`() {
        val bpnL = createLegalEntityShared(RECORD_ID)
        orchestratorMockDataFactory.mockSharingMemberRecords(listOf(sharingMemberRecord(RECORD_ID, isGoldenRecordCounted = true)))
        syncService.synchronize()

        orchestratorMockDataFactory.mockSharingMemberRecords(listOf(sharingMemberRecord(RECORD_ID, isGoldenRecordCounted = false)))
        syncService.synchronize()

        val legalEntity = poolClient.legalEntities.getLegalEntity(bpnL)
        assertThat(legalEntity.header.confidenceCriteria.numberOfSharingMembers).isEqualTo(0)
        assertThat(legalEntity.legalAddress.confidenceCriteria.numberOfSharingMembers).isEqualTo(0)
    }

    @Test
    fun `record unknown to the Pool does not prevent the other records from being applied`() {
        val bpnL = createLegalEntityShared(RECORD_ID)

        orchestratorMockDataFactory.mockSharingMemberRecords(listOf(
            sharingMemberRecord("UNKNOWN_RECORD", isGoldenRecordCounted = true),
            sharingMemberRecord(RECORD_ID, isGoldenRecordCounted = true)
        ))
        syncService.synchronize()

        assertThat(poolClient.legalEntities.getLegalEntity(bpnL).header.confidenceCriteria.numberOfSharingMembers).isEqualTo(1)
    }

    @Test
    fun `next synchronisation resumes after the last record received`() {
        val lastUpdatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        orchestratorMockDataFactory.mockSharingMemberRecords(listOf(
            sharingMemberRecord("RECORD_1", isGoldenRecordCounted = true, updatedAt = lastUpdatedAt.minusSeconds(10)),
            sharingMemberRecord("RECORD_2", isGoldenRecordCounted = true, updatedAt = lastUpdatedAt)
        ))
        syncService.synchronize()

        orchestratorMockDataFactory.resetRecordedRequests()
        syncService.synchronize()

        assertThat(orchestratorMockDataFactory.getSharingMemberRecordQueryTimestamps()).containsExactly(lastUpdatedAt)
    }

    private fun createLegalEntityShared(recordId: String): String {
        orchestratorMockDataFactory.mockReservedTasks(listOf(TaskStepReservationEntryDto("TASK_1", recordId, minValidLegalEntity())))
        taskResolutionService.resolveTasks()
        return orchestratorMockDataFactory.getStepResults().single().businessPartner.legalEntity.bpnReference.referenceValue!!
    }

    private fun sharingMemberRecord(recordId: String, isGoldenRecordCounted: Boolean, updatedAt: Instant = Instant.now()) =
        SharingMemberRecord(recordId, isGoldenRecordCounted, updatedAt, updatedAt)

    private fun minValidLegalEntity(): BusinessPartner =
        with(BusinessPartner.empty) {
            copy(
                legalEntity = legalEntity.copy(
                    bpnReference = BpnReference(referenceValue = "BPNL REQUEST ID", null, referenceType = BpnReferenceType.BpnRequestIdentifier),
                    legalName = "Legal Name",
                    confidenceCriteria = confidenceCriteria(),
                    legalAddress = legalEntity.legalAddress.copy(
                        bpnReference = BpnReference(referenceValue = "BPNA REQUEST ID", null, referenceType = BpnReferenceType.BpnRequestIdentifier),
                        confidenceCriteria = confidenceCriteria(),
                        physicalAddress = legalEntity.legalAddress.physicalAddress.copy(country = "DE", city = "Stuttgart")
                    )
                )
            )
        }

    private fun confidenceCriteria() =
        ConfidenceCriteria(
            sharedByOwner = true,
            numberOfSharingMembers = 1,
            checkedByExternalDataSource = true,
            lastConfidenceCheckAt = Instant.now(),
            nextConfidenceCheckAt = Instant.now().plus(1, ChronoUnit.DAYS),
            confidenceLevel = 10
        )

    companion object {
        private const val RECORD_ID = "RECORD_1"
    }
}
