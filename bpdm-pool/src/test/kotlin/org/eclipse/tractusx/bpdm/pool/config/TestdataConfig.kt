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

package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolDataHelper
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataKeys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestdataConfig {

    /**
     * We create [PoolDataHelper] here with standard metadata to create test data environments from
     * For more specialized environments you could also not autowire it and  instead create the [PoolDataHelper] in the test class itself
     */
    @Bean
    fun poolDataHelper(poolClient: PoolApiClient): PoolDataHelper {
        return PoolDataHelper(
            metadataToCreate = TestMetadataKeys(
                legalFormKeys = listOf("LF1", "LF2", "LF3"),
                legalEntityIdentifierTypeKeys = listOf("LID1", "LID2", "LID3", "LID4", "LID5", "LID6", "LID7", "LID8"),
                addressIdentifierTypeKeys = listOf("AID1", "AID2", "AID3", "AID4", "AID5", "AID6", "AID7", "AID8")
            ),
            poolClient
        )
    }

}