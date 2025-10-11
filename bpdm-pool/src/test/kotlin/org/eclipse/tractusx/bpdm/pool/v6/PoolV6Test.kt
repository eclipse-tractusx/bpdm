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

package org.eclipse.tractusx.bpdm.pool.v6

import org.eclipse.tractusx.bpdm.pool.v6.util.AssertRepositoryV6
import org.eclipse.tractusx.bpdm.pool.v6.util.TestDataClientV6
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestDataV6Factory
import org.eclipse.tractusx.bpdm.test.util.AuthAssertionHelper
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired

abstract class PoolV6Test: IsPoolV6Test {
    @Autowired
    override lateinit var databaseHelpers: DbTestHelpers
    @Autowired
    override lateinit var testDataClient: TestDataClientV6
    @Autowired
    override lateinit var testDataFactory: TestDataV6Factory
    @Autowired
    override lateinit var assertRepository: AssertRepositoryV6

    override val authAssertionHelper: AuthAssertionHelper = AuthAssertionHelper()

    lateinit var testName: String

    open fun beforeEach(testInfo: TestInfo){
        testName = testInfo.displayName
        databaseHelpers.truncateDbTables()
    }

}