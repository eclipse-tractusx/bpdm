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

package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateTestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.BusinessPartnerTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorExpectedResultFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryCommon
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.TestMetadataReferences
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Configuration
class OrchestratorMockConfig {


}