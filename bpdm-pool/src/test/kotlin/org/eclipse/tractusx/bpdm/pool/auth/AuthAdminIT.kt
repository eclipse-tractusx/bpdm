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

package org.eclipse.tractusx.bpdm.pool.auth

import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.test.containers.CreateNewSelfClientInitializer
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class])
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class,
    SelfClientAsAdminInitializer::class
])
class AuthAdminIT @Autowired constructor(
    poolApiClient: PoolClientImpl,
): AuthTestBase(
    poolApiClient,
    AddressAuthExpectations(
        getAddresses = AuthExpectationType.Authorized,
        getAddress = AuthExpectationType.Authorized,
        postAddressSearch = AuthExpectationType.Authorized,
        postAddresses = AuthExpectationType.Authorized,
        putAddresses = AuthExpectationType.Authorized
    ),
    SiteAuthExpectations(
        getSites = AuthExpectationType.Authorized,
        getSite = AuthExpectationType.Authorized,
        postSiteSearch = AuthExpectationType.Authorized,
        postSites = AuthExpectationType.Authorized,
        putSites = AuthExpectationType.Authorized
    ),
    LegalEntityAuthExpectations(
        getLegalEntities = AuthExpectationType.Authorized,
        getLegalEntity = AuthExpectationType.Authorized,
        postLegalEntitySearch = AuthExpectationType.Authorized,
        postLegalEntities = AuthExpectationType.Authorized,
        putLegalEntities = AuthExpectationType.Authorized,
        getLegalEntityAddresses = AuthExpectationType.Authorized,
        getLegalEntitySites = AuthExpectationType.Authorized
    ),
    MetadataAuthExpectations(
        getLegalForm = AuthExpectationType.Authorized,
        postLegalForm = AuthExpectationType.Authorized,
        getIdentifierType = AuthExpectationType.Authorized,
        postIdentifierType = AuthExpectationType.Authorized,
        getAdminArea = AuthExpectationType.Authorized,
        getFieldQualityRules = AuthExpectationType.Authorized
    ),
    MembersAuthExpectations(
        postAddressSearch = AuthExpectationType.Authorized,
        postSiteSearch = AuthExpectationType.Authorized,
        postLegalEntitySearch = AuthExpectationType.Authorized,
        postChangelogSearch = AuthExpectationType.Authorized
    ),
    CxMembershipsAuthExpectations(
        getMemberships = AuthExpectationType.Authorized,
        putMemberships = AuthExpectationType.Authorized
    ),
    changelogAuthExpectation = AuthExpectationType.Authorized,
    bpnAuthExpectation = AuthExpectationType.Authorized
)

class SelfClientAsAdminInitializer : CreateNewSelfClientInitializer() {
    override val clientId: String
        get() = "BPDM-POOL"

    override val roleName: String
        get() = "BPDM Pool Admin"
}