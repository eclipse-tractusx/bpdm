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

package org.eclipse.tractusx.bpdm.pool.auth

import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class])
@ContextConfiguration(initializers = [
    PostgreSQLContextInitializer::class,
    KeyCloakInitializer::class
])
class NoAuthIT @Autowired constructor(
    poolApiClient: PoolApiClient,
): AuthTestBase(
    poolApiClient,
    AddressAuthExpectations(
        getAddresses = AuthExpectationType.Unauthorized,
        getAddress = AuthExpectationType.Unauthorized,
        postAddressSearch = AuthExpectationType.Unauthorized,
        postAddresses = AuthExpectationType.Unauthorized,
        putAddresses = AuthExpectationType.Unauthorized
    ),
    SiteAuthExpectations(
        getSites = AuthExpectationType.Unauthorized,
        getSite = AuthExpectationType.Unauthorized,
        postSiteSearch = AuthExpectationType.Unauthorized,
        postSites = AuthExpectationType.Unauthorized,
        putSites = AuthExpectationType.Unauthorized
    ),
    LegalEntityAuthExpectations(
        getLegalEntities = AuthExpectationType.Unauthorized,
        getLegalEntity = AuthExpectationType.Unauthorized,
        postLegalEntitySearch = AuthExpectationType.Unauthorized,
        postLegalEntities = AuthExpectationType.Unauthorized,
        putLegalEntities = AuthExpectationType.Unauthorized,
        getLegalEntityAddresses = AuthExpectationType.Unauthorized,
        getLegalEntitySites = AuthExpectationType.Unauthorized
    ),
    MetadataAuthExpectations(
        getLegalForm = AuthExpectationType.Unauthorized,
        postLegalForm = AuthExpectationType.Unauthorized,
        getIdentifierType = AuthExpectationType.Unauthorized,
        postIdentifierType = AuthExpectationType.Unauthorized,
        getAdminArea = AuthExpectationType.Unauthorized,
        getFieldQualityRules = AuthExpectationType.Unauthorized
    ),
    MembersAuthExpectations(
        postAddressSearch = AuthExpectationType.Unauthorized,
        postSiteSearch = AuthExpectationType.Unauthorized,
        postLegalEntitySearch = AuthExpectationType.Unauthorized,
        postChangelogSearch = AuthExpectationType.Unauthorized
    ),
    changelogAuthExpectation = AuthExpectationType.Unauthorized,
    bpnAuthExpectation = AuthExpectationType.Unauthorized
)