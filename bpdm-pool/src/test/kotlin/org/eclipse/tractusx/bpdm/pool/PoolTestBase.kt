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

package org.eclipse.tractusx.bpdm.pool

import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.springframework.beans.factory.annotation.Autowired

/**
 * Base class for all Pool tests
 *
 * Comes with generic helpful util classes and functions for testing
 */
abstract class PoolTestBase {
    @Autowired
    lateinit var databaseHelpers: DbTestHelpers

    lateinit var testName: String

    /**
     * Needs to be overwritten and annotated with @BeforeEach as Junit does not pick the annotation up from the base class
     */
    @BeforeEach
    open fun beforeEach(testInfo: TestInfo){
        testName = testInfo.displayName
        databaseHelpers.truncateDbTables()
    }
}