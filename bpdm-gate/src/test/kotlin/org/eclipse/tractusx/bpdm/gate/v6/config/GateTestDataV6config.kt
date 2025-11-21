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

package org.eclipse.tractusx.bpdm.gate.v6.config

import org.eclipse.tractusx.bpdm.gate.v6.util.GateOperatorClientV6
import org.eclipse.tractusx.bpdm.gate.v6.util.GateTestDataClientV6
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.ExpectedGateResultV6Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.GateTestDataFactoryV6
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class GateTestDataV6config {

    @Bean
    fun testDataFactoryV6(): GateTestDataFactoryV6{
        return GateTestDataFactoryV6(
            GateInputFactory(
                testMetadata = TestMetadata(
                    identifierTypes = listOf("idType1", "idType2", "idType3"),
                    legalForms = listOf("legalform1", "legalform2", "legalform3"),
                    adminAreas = listOf("adminArea1", "adminArea2", "adminArea3")
                ),
                testRunData = null
            ),
            ExpectedGateResultV6Factory()
        )
    }

    @Bean
    fun gateTestDataClientV6(testDataFactory: GateTestDataFactoryV6, operatorClient: GateOperatorClientV6): GateTestDataClientV6{
        return GateTestDataClientV6(testDataFactory, operatorClient)
    }
}