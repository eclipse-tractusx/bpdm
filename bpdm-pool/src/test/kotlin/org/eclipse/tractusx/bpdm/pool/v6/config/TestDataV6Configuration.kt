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

package org.eclipse.tractusx.bpdm.pool.v6.config

import jakarta.persistence.EntityManager
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.v6.util.TestDataClientV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestMetadataV6Provider
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient as PoolApiClientV6

@Configuration
@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
class TestDataV6Configuration {

    /**
     * Include entity manager in the arguments here to make sure that the test data is gathered after the database connection is fully initialized
     */
    @Bean
    fun testMetadataV6Provider(flyway: Flyway, entityManager: EntityManager, poolClientV6: PoolApiClientV6, poolClient: PoolApiClient): TestMetadataV6Provider{
        flyway.clean()
        flyway.migrate()
        return TestMetadataV6Provider(poolClientV6, poolClient)
    }

    @Bean
    fun testDataV6Factory(testMetadataV6Provider: TestMetadataV6Provider): TestDataV6Factory{
        return TestDataV6Factory(testMetadataV6Provider.createTestMetadata())
    }

    @Bean
    fun testDataClientV6(poolApiClient: PoolApiClientV6, testDataV6Factory: TestDataV6Factory): TestDataClientV6{
        return TestDataClientV6(testDataV6Factory.request, poolApiClient)
    }


}