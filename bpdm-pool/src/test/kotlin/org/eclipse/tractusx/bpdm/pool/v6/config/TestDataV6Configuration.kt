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

package org.eclipse.tractusx.bpdm.pool.v6.config

import org.eclipse.tractusx.bpdm.pool.v6.util.PoolOperatorClientV6
import org.eclipse.tractusx.bpdm.pool.v6.util.TestDataClientV6
import org.eclipse.tractusx.bpdm.pool.v6.util.metadata.AdminAreaLevel1ImporterV6
import org.eclipse.tractusx.bpdm.pool.v6.util.metadata.IdentifierTypeImporterV6
import org.eclipse.tractusx.bpdm.pool.v6.util.metadata.LegalFormImporterV6
import org.eclipse.tractusx.bpdm.pool.v6.util.metadata.TestMetadataV6FromResourceProvider
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class TestDataV6Configuration {

    @Bean
    fun testDataV6Factory(resourceLoader: ResourceLoader): TestDataV6Factory{
        val legalFormImporterV6 = LegalFormImporterV6(resourceLoader.getResource("classpath:legalforms/2023-09-28-elf-code-list-v1.5.csv"))
        val adminAreaImporter = AdminAreaLevel1ImporterV6(resourceLoader.getResource("classpath:regions/IP2LOCATION-ISO3166-2.CSV"))
        val identifierTypeImporter = IdentifierTypeImporterV6(resourceLoader.getResource("classpath:identifiertypes/identifier-types.csv"))

        val testMetadataV6Provider = TestMetadataV6FromResourceProvider(legalFormImporterV6, adminAreaImporter, identifierTypeImporter)

        return TestDataV6Factory(testMetadataV6Provider.createMetadata())
    }

    @Bean
    fun testDataClientV6(poolApiClient: PoolOperatorClientV6, testDataV6Factory: TestDataV6Factory): TestDataClientV6{
        return TestDataClientV6(testDataV6Factory.request, poolApiClient)
    }


}