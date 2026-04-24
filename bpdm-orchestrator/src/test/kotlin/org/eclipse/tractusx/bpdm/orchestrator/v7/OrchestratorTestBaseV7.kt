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

package org.eclipse.tractusx.bpdm.orchestrator.v7

import org.eclipse.tractusx.bpdm.orchestrator.OrchestratorTestBase
import org.eclipse.tractusx.bpdm.orchestrator.v7.util.OrchestratorAssertRepositoryV7
import org.eclipse.tractusx.bpdm.orchestrator.v7.util.OrchestratorTestDataClientV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorExpectedResultFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV7
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.springframework.beans.factory.annotation.Autowired

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
/**
 * Base for all classes which want to use the V7 Orchestrator API for testing
 */
abstract class OrchestratorTestBaseV7: OrchestratorTestBase() {
    @Autowired
    lateinit var orchestratorClient: OrchestrationApiClient
    @Autowired
    lateinit var requestFactory: OrchestratorRequestFactoryV7
    @Autowired
    lateinit var resultFactory: OrchestratorExpectedResultFactoryV7
    @Autowired
    lateinit var assertRepo: OrchestratorAssertRepositoryV7
    @Autowired
    lateinit var testDataClient: OrchestratorTestDataClientV7
}