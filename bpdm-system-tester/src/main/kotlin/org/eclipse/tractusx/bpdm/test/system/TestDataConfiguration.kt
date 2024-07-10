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

package org.eclipse.tractusx.bpdm.test.system

import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
class TestDataConfiguration {

    @Bean
    fun testMetadata(poolClient: PoolApiClient): TestMetadata{
        val testMetadata = TestMetadata(
            listOf("id1", "id2"),
            listOf("legalform1", "legalform2"),
            listOf("DE-BW", "DE-BY")
        )

        testMetadata.identifierTypes.forEach { technicalKey ->
            try{
                poolClient.metadata.createIdentifierType(
                    IdentifierTypeDto(
                        technicalKey,
                        IdentifierBusinessPartnerType.LEGAL_ENTITY,
                        technicalKey,
                        null,
                        null,
                        null
                    )
                )
            }catch (_: Throwable){}

            try{
                poolClient.metadata.createIdentifierType(
                    IdentifierTypeDto(
                        technicalKey,
                        IdentifierBusinessPartnerType.ADDRESS,
                        technicalKey,
                        null,
                        null,
                        null
                    )
                )
            }catch (_: Throwable) {}
        }

        testMetadata.legalForms.forEach { technicalKey ->
            try{
                poolClient.metadata.createLegalForm(LegalFormRequest(technicalKey, technicalKey, null))
            }catch (_: Throwable){}
        }

        return testMetadata
    }

    @Bean
    fun gateTestDataFactory(testMetadata: TestMetadata, testRunData: TestRunData): GateInputFactory{
        return GateInputFactory(testMetadata, testRunData)
    }

    @Bean
    fun gateOutputFactory(gateInputDataFactory: GateInputFactory): GateOutputFactory{
        return GateOutputFactory(gateInputDataFactory)
    }

    @Bean
    fun testRunData(): TestRunData{
        return TestRunData(Instant.now())
    }
}