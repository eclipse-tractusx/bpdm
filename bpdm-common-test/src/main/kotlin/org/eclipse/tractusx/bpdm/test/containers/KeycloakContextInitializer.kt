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

package org.eclipse.tractusx.bpdm.test.containers

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.eclipse.tractusx.bpdm.test.config.SelfClientConfigProperties
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.TENANT_BPNL
import org.eclipse.tractusx.bpdm.test.containers.KeyCloakInitializer.Companion.keycloakContainer
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.ProtocolMapperRepresentation
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * Setup and configuration of Keycloak to be used by the system under test
 */
class KeyCloakInitializer: ApplicationContextInitializer<ConfigurableApplicationContext>{
    companion object{
        val keycloakContainer: KeycloakContainer = KeycloakContainer("docker.io/keycloak/keycloak:26.5.6@sha256:8d44614c74798322c4e07fbe0ecb15cfbb5879d69b484628555f58ade06f0d8c")
            .withRealmImportFile("keycloak/BPDM-realm.json")

        const val REALM =  "BPDM"
        const val TENANT_BPNL = "BPNL000000000001"
        const val ROLE_MANAGEMENT_CLIENT = "technical_roles_management"

        const val CLIENT_ID_OPERATOR = "admin"
        const val CLIENT_ID_SHARING_MEMBER = "sharing-member"
        const val CLIENT_ID_PARTICIPANT = "participant"
        const val CLIENT_ID_UNAUTHORIZED = "unauthorized"

        const val CLIENT_ID_GATE_INPUT_MANAGER = "gate-input-manager"
        const val CLIENT_ID_GATE_INPUT_CONSUMER = "gate-input-consumer"
        const val CLIENT_ID_GATE_OUTPUT_CONSUMER = "gate-output-consumer"

        const val CLIENT_ID_ORCHESTRATOR_PROCESSOR_POOL_SYNC = "refiner-pool-sync"
        const val CLIENT_ID_ORCHESTRATOR_PROCESSOR_CLEAN_AND_SYNC = "refiner-clean-and-sync"
        const val CLIENT_ID_ORCHESTRATOR_PROCESSOR_CLEAN = "refiner-clean"
        const val CLIENT_ID_ORCHESTRATOR_TASK_CREATOR = "task-creator"

        private const val OWN_PROVIDER_ID = "test-keycloak"

        private var isContainerInitialized = false
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        if(!isContainerInitialized){
            initializeContainer()
        }

        val authServerUrl = keycloakContainer.authServerUrl.trimEnd('/')
        val issuerUri = "$authServerUrl/realms/$REALM"

        TestPropertyValues.of(
            "bpdm.security.auth-server-url=$authServerUrl",
            "bpdm.security.realm=$REALM",
            "spring.security.oauth2.client.provider.${OWN_PROVIDER_ID}.issuer-uri=${issuerUri}"
        ).applyTo(applicationContext)
    }

    private fun initializeContainer(){
        keycloakContainer.start()

        isContainerInitialized = true
    }
}

/**
 * Configures the tests to use the given Keycloak client for authentication with the API under test
 *
 * Requires a Keycloak configuration and the client to be existing already in Keycloak
 */
abstract class SelfClientInitializer:  ApplicationContextInitializer<ConfigurableApplicationContext>{

    abstract val clientId: String

    override fun initialize(applicationContext: ConfigurableApplicationContext){
        val realm = applicationContext.environment.getProperty("bpdm.security.realm")
        val authServerUrl = applicationContext.environment.getProperty("bpdm.security.auth-server-url")

        val adminClient = keycloakContainer.keycloakAdminClient
        val clientSecret = adminClient.realm(realm).clients().findByClientId(clientId).first().secret

        TestPropertyValues.of(
            "${SelfClientConfigProperties.PREFIX}.security-enabled=true",
            "${SelfClientConfigProperties.PREFIX}.provider.issuer-uri=${authServerUrl}/realms/$realm",
            "${SelfClientConfigProperties.PREFIX}.registration.authorization-grant-type=client_credentials",
            "${SelfClientConfigProperties.PREFIX}.registration.client-id=$clientId",
            "${SelfClientConfigProperties.PREFIX}.registration.client-secret=$clientSecret"
        ).applyTo(applicationContext)
    }
}