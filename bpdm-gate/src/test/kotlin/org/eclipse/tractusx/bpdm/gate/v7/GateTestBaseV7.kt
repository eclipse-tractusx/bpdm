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

package org.eclipse.tractusx.bpdm.gate.v7

import jakarta.annotation.PostConstruct
import org.eclipse.tractusx.bpdm.gate.GateTestBase
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.v7.util.GateAssertRepositoryV7
import org.eclipse.tractusx.bpdm.gate.v7.util.GateTestClientProviderV7
import org.eclipse.tractusx.bpdm.gate.v7.util.GateTestDataClientV7
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputDtoV7Factory
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.BusinessPartnerInputRequestV7Factory
import org.springframework.beans.factory.annotation.Autowired

abstract class GateTestBaseV7 : GateTestBase(){
    @Autowired
    lateinit var assertRepo: GateAssertRepositoryV7
    @Autowired
    lateinit var testDataClient: GateTestDataClientV7
    @Autowired
    lateinit var businessPartnerInputRequestFactory: BusinessPartnerInputRequestV7Factory
    @Autowired
    lateinit var businessPartnerInputFactory: BusinessPartnerInputDtoV7Factory
    @Autowired
    lateinit var testClientProvider: GateTestClientProviderV7

    lateinit var gateClient: GateClient

    @PostConstruct
    fun init() {
        gateClient = testClientProvider.createClient(KeyCloakInitializer.CLIENT_ID_OPERATOR)
    }
}