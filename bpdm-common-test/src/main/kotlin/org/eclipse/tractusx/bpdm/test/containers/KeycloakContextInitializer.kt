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

package org.eclipse.tractusx.bpdm.test.containers

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.eclipse.tractusx.bpdm.test.config.SelfClientConfigProperties
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.keycloakContainer
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class KeyCloakInitializer: ApplicationContextInitializer<ConfigurableApplicationContext>{
    companion object{
        val keycloakContainer: KeycloakContainer = KeycloakContainer("quay.io/keycloak/keycloak:23.0")
            .withRealmImportFile("keycloak/CX-Central.json")

        const val REALM =  "CX-Central"
        const val TENANT_BPNL = "BPNL000000000010"
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        keycloakContainer.start()

        TestPropertyValues.of(
            "bpdm.security.auth-server-url=${keycloakContainer.authServerUrl.trimEnd('/')}",
            "bpdm.security.realm=${REALM}"
        ).applyTo(applicationContext)
    }
}

abstract class SelfClientInitializer:  ApplicationContextInitializer<ConfigurableApplicationContext>{

    abstract val clientId: String

    override fun initialize(applicationContext: ConfigurableApplicationContext){
        val realm = applicationContext.environment.getProperty("bpdm.security.realm")
        val authServerUrl = applicationContext.environment.getProperty("bpdm.security.auth-server-url")

        val client = keycloakContainer.keycloakAdminClient
        val clientSecret = client.realm(realm).clients().findByClientId(clientId).first().secret

        TestPropertyValues.of(
            "${SelfClientConfigProperties.PREFIX}.security-enabled=true",
            "${SelfClientConfigProperties.PREFIX}.provider.issuer-uri=${authServerUrl}/realms/$realm",
            "${SelfClientConfigProperties.PREFIX}.registration.authorization-grant-type=client_credentials",
            "${SelfClientConfigProperties.PREFIX}.registration.client-id=$clientId",
            "${SelfClientConfigProperties.PREFIX}.registration.client-secret=$clientSecret"
        ).applyTo(applicationContext)
    }
}