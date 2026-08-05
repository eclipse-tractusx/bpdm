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

package org.eclipse.tractusx.bpdm.pool.v6

import org.eclipse.tractusx.bpdm.pool.UnscheduledTestEnvironment
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.v7.util.AssertRepositoryV7
import org.eclipse.tractusx.bpdm.pool.v7.util.TestDataClientV7
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for v6 tests whose subject the v6 API cannot express on its own, so that setting it up or observing it
 * needs the v7 API - script variants, for instance, which v6 neither accepts nor returns.
 */
@UnscheduledTestEnvironment
abstract class UnscheduledPoolTestBaseV6AndV7: PoolTestBaseV6() {
    @Autowired
    lateinit var testDataClientV7: TestDataClientV7
    @Autowired
    lateinit var assertRepositoryV7: AssertRepositoryV7
    @Autowired
    lateinit var poolClientV7: PoolApiClient

    @BeforeEach
    override fun beforeEach(testInfo: TestInfo) {
        super.beforeEach(testInfo)
    }
}
