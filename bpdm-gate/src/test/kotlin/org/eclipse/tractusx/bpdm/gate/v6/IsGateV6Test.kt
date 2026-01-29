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

package org.eclipse.tractusx.bpdm.gate.v6

import org.eclipse.tractusx.bpdm.gate.api.v6.client.GateClientV6
import org.eclipse.tractusx.bpdm.gate.v6.util.GateAssertRepositoryV6
import org.eclipse.tractusx.bpdm.gate.v6.util.GateTestDataClientV6

interface IsGateV6Test {
    val gateClient: GateClientV6
    val assertRepo: GateAssertRepositoryV6
    val testDataClient: GateTestDataClientV6

    val requestFactory get() = testDataClient.testDataFactory.request
    val expectedResultFactory get() = testDataClient.testDataFactory.result
}