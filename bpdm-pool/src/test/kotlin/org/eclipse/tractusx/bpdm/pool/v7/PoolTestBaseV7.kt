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

package org.eclipse.tractusx.bpdm.pool.v7

import org.eclipse.tractusx.bpdm.pool.PoolTestBase
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.v7.util.AssertRepositoryV7
import org.eclipse.tractusx.bpdm.pool.v7.util.TestDataClientV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.springframework.beans.factory.annotation.Autowired

abstract class PoolTestBaseV7: PoolTestBase() {
    @Autowired
    lateinit var testDataClient: TestDataClientV7
    @Autowired
    lateinit var requestFactory: PoolRequestFactoryV7
    @Autowired
    lateinit var resultFactory: PoolResponseFactoryV7
    @Autowired
    lateinit var assertRepository: AssertRepositoryV7
    @Autowired
    lateinit var poolClient: PoolApiClient
}