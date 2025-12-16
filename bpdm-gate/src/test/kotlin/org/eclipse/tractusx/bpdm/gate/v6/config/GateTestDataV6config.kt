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

import com.fasterxml.jackson.databind.ObjectMapper
import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.gate.service.TaskCreationBatchService
import org.eclipse.tractusx.bpdm.gate.service.TaskResolutionBatchService
import org.eclipse.tractusx.bpdm.gate.v6.util.GateOperatorClientV6
import org.eclipse.tractusx.bpdm.gate.v6.util.GateTestDataClientV6
import org.eclipse.tractusx.bpdm.pool.api.model.CountrySubdivisionDto
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.ExpectedGateResultV6Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v6.GateTestDataFactoryV6
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.OrchestratorMockDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerRequestFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.ExpectedBusinessPartnerResultFactory
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class GateTestDataV6config {

    private val poolTestMetadata = org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadata(
        legalForms = listOf(
            BusinessPartnerVerboseValues.legalForm1,
            BusinessPartnerVerboseValues.legalForm2
        ),
        legalEntityIdentifierTypes = listOf(
            IdentifierTypeDto("idType1", IdentifierBusinessPartnerType.LEGAL_ENTITY, "idType1", null, null, null, null, sortedSetOf(), emptyList()),
            IdentifierTypeDto("idType2", IdentifierBusinessPartnerType.LEGAL_ENTITY, "idType2", null, null, null, null, sortedSetOf(), emptyList()),
            IdentifierTypeDto("idType3", IdentifierBusinessPartnerType.LEGAL_ENTITY, "idType3", null, null, null, null, sortedSetOf(), emptyList())
        ),
        addressIdentifierTypes = listOf(
            IdentifierTypeDto("addressIdType1", IdentifierBusinessPartnerType.ADDRESS, "addressIdType1", null, null, null, null, sortedSetOf(), emptyList()),
            IdentifierTypeDto("addressIdType2", IdentifierBusinessPartnerType.ADDRESS, "addressIdType2", null, null, null, null, sortedSetOf(), emptyList()),
            IdentifierTypeDto("addressIdType3", IdentifierBusinessPartnerType.ADDRESS, "addressIdType3", null, null, null, null, sortedSetOf(), emptyList())
        ),
        adminAreas = listOf(
            CountrySubdivisionDto(CountryCode.DE, "adminArea1", "adminArea1"),
            CountrySubdivisionDto(CountryCode.US, "adminArea2", "adminArea2"),
            CountrySubdivisionDto(CountryCode.CN, "adminArea3", "adminArea3"),
        )
    )


    @Bean
    fun testDataFactoryV6(): GateTestDataFactoryV6{
        return GateTestDataFactoryV6(
            GateInputFactory(
                testMetadata = TestMetadata(
                    identifierTypes = poolTestMetadata.legalEntityIdentifierTypes.map { it.technicalKey },
                    legalForms = poolTestMetadata.legalForms.map { it.technicalKey },
                    adminAreas = poolTestMetadata.adminAreas.map { it.code }
                ),
                testRunData = null
            ),
            ExpectedGateResultV6Factory()
        )
    }

    @Bean
    fun refinementTestDataFactory(): RefinementTestDataFactory{
        return RefinementTestDataFactory()
    }

    @Bean
    fun orchestratorMockDataFactory(
        refinementTestDataFactory: RefinementTestDataFactory,
        objectMapper: ObjectMapper
    ): OrchestratorMockDataFactory{
        return OrchestratorMockDataFactory(refinementTestDataFactory, objectMapper)
    }

    @Bean
    fun poolRequestFactory(): BusinessPartnerRequestFactory{
        return BusinessPartnerRequestFactory(poolTestMetadata)
    }

    @Bean
    fun poolMockDataFactory(
        objectMapper: ObjectMapper
    ): PoolMockDataFactory{
        return PoolMockDataFactory(
            BusinessPartnerRequestFactory(poolTestMetadata),
            ExpectedBusinessPartnerResultFactory(poolTestMetadata),
            objectMapper
        )
    }

    @Bean
    fun gateTestDataClientV6(
        testDataFactory: GateTestDataFactoryV6,
        operatorClient: GateOperatorClientV6,
        orchestratorMockDataFactory: OrchestratorMockDataFactory,
        taskCreationBatchService: TaskCreationBatchService,
        taskResolutionBatchService: TaskResolutionBatchService,
        poolMockDataFactory: PoolMockDataFactory
    ): GateTestDataClientV6{
        return GateTestDataClientV6(
            testDataFactory,
            operatorClient,
            orchestratorMockDataFactory,
            taskCreationBatchService,
            taskResolutionBatchService,
            "BPNL00000003CRHK",
            poolMockDataFactory
        )
    }
}