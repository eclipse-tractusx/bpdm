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

package org.eclipse.tractusx.bpdm.gate.v7.util

import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.service.RelationTaskCreationService
import org.eclipse.tractusx.bpdm.gate.service.RelationTaskResolutionService
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationBatchService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionBatchService
import org.eclipse.tractusx.bpdm.test.testdata.GoldenRecordMockFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory

class GateTestDataClientV7(
    gateClient: GateClient,
    testDataFactory: TestDataFactoryGateV7,
    orchestratorMockDataFactory: OrchestratorMockDataFactory,
    taskCreationBatchService: TaskCreationBatchService,
    taskResolutionBatchService: TaskResolutionBatchService,
    relationTaskResolutionService: RelationTaskResolutionService,
    relationTaskCreationService: RelationTaskCreationService,
    goldenRecordMockFactory: GoldenRecordMockFactory,
    tenantBpnL: String,
) {
    val businessPartner = BusinessPartnerTestDataClientV7(
        gateClient,
        testDataFactory,
        orchestratorMockDataFactory,
        taskCreationBatchService,
        taskResolutionBatchService,
        goldenRecordMockFactory,
        tenantBpnL,
    )

    val relation = RelationTestDataClientV7(
        gateClient,
        testDataFactory,
        orchestratorMockDataFactory,
        relationTaskCreationService,
        relationTaskResolutionService,
        businessPartner,
    )
}
