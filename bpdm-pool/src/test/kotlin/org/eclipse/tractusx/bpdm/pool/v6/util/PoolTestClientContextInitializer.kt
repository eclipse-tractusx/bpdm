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

package org.eclipse.tractusx.bpdm.pool.v6.util

import org.eclipse.tractusx.bpdm.pool.v6.config.OperatorClientConfigProperties
import org.eclipse.tractusx.bpdm.pool.v6.config.ParticipantClientConfigProperties
import org.eclipse.tractusx.bpdm.pool.v6.config.SharingMemberClientConfigProperties
import org.eclipse.tractusx.bpdm.pool.v6.config.UnauthorizedClientConfigProperties
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.CLIENT_ID_OPERATOR
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.CLIENT_ID_PARTICIPANT
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.CLIENT_ID_SHARING_MEMBER
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.CLIENT_ID_UNAUTHORIZED
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.REALM
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.keycloakContainer
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.operatorClientSecret
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.participantClientSecret
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.sharingMemberClientSecret
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.unauthorizedClientSecret
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class PoolTestClientContextInitializer: ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val authServerUrl = keycloakContainer.authServerUrl.trimEnd('/')
        val issuerUri = "$authServerUrl/realms/$REALM"

        applyClientConnectionProperties(CLIENT_ID_OPERATOR, operatorClientSecret, OperatorClientConfigProperties.PREFIX, issuerUri, applicationContext)
        applyClientConnectionProperties(CLIENT_ID_SHARING_MEMBER, sharingMemberClientSecret, SharingMemberClientConfigProperties.PREFIX, issuerUri, applicationContext)
        applyClientConnectionProperties(CLIENT_ID_PARTICIPANT, participantClientSecret, ParticipantClientConfigProperties.PREFIX, issuerUri, applicationContext)
        applyClientConnectionProperties(CLIENT_ID_UNAUTHORIZED, unauthorizedClientSecret, UnauthorizedClientConfigProperties.PREFIX, issuerUri, applicationContext)
    }

    private fun applyClientConnectionProperties(
        clientId: String,
        clientSecret: String,
        propertyPrefix: String,
        issuerUri: String,
        applicationContext: ConfigurableApplicationContext
    ){
        TestPropertyValues.of(
            "${propertyPrefix}.base-url=http://localhost:8080/",
            "${propertyPrefix}.security-enabled=true",
            "${propertyPrefix}.provider.issuer-uri=$issuerUri",
            "${propertyPrefix}.registration.authorization-grant-type=client_credentials",
            "${propertyPrefix}.registration.client-id=$clientId",
            "${propertyPrefix}.registration.client-secret=$clientSecret"
        ).applyTo(applicationContext)
    }
}