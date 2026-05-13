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

import org.eclipse.tractusx.bpdm.gate.v7.util.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.GateInputFactory
import org.eclipse.tractusx.bpdm.test.testdata.gate.TestMetadata
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputDtoV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputRequestV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerOutputDtoV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.GateTestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.PageChangeLogV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.RelationInputRequestV7Factory
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GateTestDataV7Config {

    @Bean
    fun testMetadata(): TestMetadata {
        return TestMetadata(
            identifierTypes = listOf("EU_VAT_ID_DE", "DUNS_ID"),
            legalForms = listOf("SCE1", "SGST"),
            adminAreas = listOf("DE-BW", "DE-BY")
        )
    }

    @Bean
    fun gateTestDataFactory(testMetadata: TestMetadata): GateInputFactory {
        return GateInputFactory(testMetadata, null)
    }

    @Bean
    fun gateTestMetadataV7(): GateTestMetadataV7 {
        return GateTestMetadataV7(
            identifierTypes = listOf("EU_VAT_ID_DE", "DUNS_ID"),
            legalForms = listOf("SCE1", "SGST"),
            adminAreas = listOf("DE-BW", "DE-BY"),
            scriptVariants = listOf("Latn", "Arab", "Hans", "Cyrl")
        )
    }

    @Bean
    fun businessPartnerInputRequestV7Factory(gateTestMetadataV7: GateTestMetadataV7): BusinessPartnerInputRequestV7Factory {
        return BusinessPartnerInputRequestV7Factory(gateTestMetadataV7)
    }

    @Bean
    fun businessPartnerInputDtoV7Factory(): BusinessPartnerInputDtoV7Factory {
        return BusinessPartnerInputDtoV7Factory()
    }

    @Bean
    fun gateAssertRepositoryV7(): GateAssertRepositoryV7{
        val instantSecondsComparator = InstantSecondsComparator()
        return GateAssertRepositoryV7(
            instantSecondsComparator,
            LocalDatetimeSecondsComparator(instantSecondsComparator)
        )
    }

    @Bean
    fun pageChangeLogV7Factory(): PageChangeLogV7Factory{
        return PageChangeLogV7Factory()
    }

    @Bean
    fun businessPartnerOutputDtoV7Factory(): BusinessPartnerOutputDtoV7Factory{
        return BusinessPartnerOutputDtoV7Factory()
    }

    @Bean
    fun relationInputRequestV7Factory(): RelationInputRequestV7Factory {
        return RelationInputRequestV7Factory()
    }

}