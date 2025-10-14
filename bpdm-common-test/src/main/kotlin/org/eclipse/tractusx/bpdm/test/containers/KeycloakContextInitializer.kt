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
        val keycloakContainer: KeycloakContainer = KeycloakContainer("quay.io/keycloak/keycloak:23.0")
            .withRealmImportFile("keycloak/CX-Central.json")

        const val REALM =  "CX-Central"
        const val TENANT_BPNL = "BPNL00000003CRHK"
        const val ROLE_MANAGEMENT_CLIENT = "technical_roles_management"

        const val CLIENT_ID_OPERATOR = "bpdm-operator"
        const val CLIENT_ID_SHARING_MEMBER = "bpdm-sharing-member"
        const val CLIENT_ID_PARTICIPANT = "bpdm-participant"
        const val CLIENT_ID_UNAUTHORIZED = "bpdm-unauthorized"

        private var isClientsInitialized = false

        lateinit var operatorClientSecret: String
        lateinit var sharingMemberClientSecret: String
        lateinit var participantClientSecret: String
        lateinit var unauthorizedClientSecret: String
    }

    lateinit var clientFactory: KeycloakClientFactory

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        keycloakContainer.start()

        if(!isClientsInitialized)
            initializeClients()

        val authServerUrl = keycloakContainer.authServerUrl.trimEnd('/')

        TestPropertyValues.of(
            "bpdm.security.auth-server-url=$authServerUrl",
            "bpdm.security.realm=$REALM"
        ).applyTo(applicationContext)
    }

    private fun initializeClients(){
        clientFactory = KeycloakClientFactory()

        operatorClientSecret = clientFactory.createClient(CLIENT_ID_OPERATOR, "BPDM Pool Admin")
        sharingMemberClientSecret = clientFactory.createClient(CLIENT_ID_SHARING_MEMBER, "BPDM Pool Sharing Consumer")
        participantClientSecret = clientFactory.createClient(CLIENT_ID_PARTICIPANT, "BPDM Pool Consumer")
        unauthorizedClientSecret = clientFactory.createClient(CLIENT_ID_UNAUTHORIZED, null)

        isClientsInitialized = true
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

/**
 * Creates a new Keycloak client with a given role available in the [KeyCloakInitializer.ROLE_MANAGEMENT_CLIENT]
 * and configures the tests to use that created client for authentication with the API under test.
 *
 * If the roleName is null just creates a user with no roles and permissions attached to it
 *
 * Requires a Keycloak configuration
 */
abstract class CreateNewSelfClientInitializer: SelfClientInitializer(){

    val clientFactory: KeycloakClientFactory = KeycloakClientFactory()

    abstract val roleName: String?

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        clientFactory.createClient(clientId, roleName)
        super.initialize(applicationContext)
    }
}

class KeycloakClientFactory{
    fun createClient(clientId: String, roleName: String?): String{
        val adminClient = keycloakContainer.keycloakAdminClient
        val realm = adminClient.realm(KeyCloakInitializer.REALM)
        val clients = realm.clients()

        val roleManagementClientUuid = clients.findByClientId(KeyCloakInitializer.ROLE_MANAGEMENT_CLIENT).first().id
        val roleManagementClient =  clients.get(roleManagementClientUuid)
        val role = roleName?.let { roleManagementClient.roles().list().find { it.name == roleName } }

        clientId.let { clientToCreate ->
            clients.create(ClientRepresentation().apply {
                this.clientId = clientToCreate
                this.isServiceAccountsEnabled = true
            })
        }

        val createdClientUuid = clients.findByClientId(clientId).first().id


        val newProtocolMapper = ProtocolMapperRepresentation().apply {
            name = "BPN"
            protocol = "openid-connect"
            protocolMapper = "oidc-usermodel-attribute-mapper"
            config = mapOf(
                "introspection.token.claim" to "true",
                "userinfo.token.claim" to "true",
                "user.attribute" to "bpn",
                "id.token.claim" to "true",
                "access.token.claim" to "true",
                "claim.name" to "bpn",
                "jsonType.label" to "String"
            )
        }

        val createdClient = clients.get(createdClientUuid)

        createdClient
            .protocolMappers
            .createMapper(newProtocolMapper)

        val newServiceAccount = clients
            .get(createdClientUuid)
            .serviceAccountUser

        newServiceAccount.attributes = mutableMapOf(Pair("bpn", listOf(TENANT_BPNL)))

        realm.users()
            .get(newServiceAccount.id)
            .update(newServiceAccount)

        if(role != null){
            realm.users()
                .get(newServiceAccount.id)
                .roles()
                .clientLevel(roleManagementClient.toRepresentation().id)
                .add(listOf(role))
        }

        return createdClient.secret.value
    }
}

/**
 * Creates a new client having no permissions attached with it
 */
class AuthenticatedSelfClient: CreateNewSelfClientInitializer(){
    override val roleName: String?
        get() = null
    override val clientId: String
        get() = "AuthenticatedClient"
}