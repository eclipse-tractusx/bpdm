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

package org.eclipse.tractusx.bpdm.test.system.config

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.BpdmClientProperties
import org.eclipse.tractusx.bpdm.common.util.BpdmWebClientProvider
import org.eclipse.tractusx.bpdm.common.util.ClientConfigurationProperties
import org.eclipse.tractusx.bpdm.gate.api.client.GateClientImpl
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMemberGate
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMemberGates
import org.eclipse.tractusx.bpdm.test.system.utils.SharingStateWatcher
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

@ConfigurationProperties(prefix = GateInputClientConfigProperties.PREFIX)
data class GateInputClientConfigProperties(
    override val baseUrl: String = "http://localhost:8081",
    val searchChangelogPageSize: Int = 100,
    override val securityEnabled: Boolean = false,
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : BpdmClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.gate-input"
    }

    override fun getId() = PREFIX
}

@ConfigurationProperties(prefix = GateOutputClientConfigProperties.PREFIX)
data class GateOutputClientConfigProperties(
    override val baseUrl: String = "http://localhost:8081",
    override val securityEnabled: Boolean = false,
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : BpdmClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.gate-output"
    }

    override fun getId() = PREFIX
}

/**
 * A Gate client of a sharing member the run may not act for at all.
 *
 * The first sharing member is always there, so its clients take their settings as given. A further member is
 * named by its base-url or not named at all - and an unconfigured client that still reported itself as secured
 * would be handed to the OAuth2 client registrations, which cannot be built without credentials.
 */
interface FurtherGateClientProperties : BpdmClientProperties {

    /** Reports whether the run is given this sharing member at all. */
    val isConfigured get() = baseUrl.isNotBlank()

    override val securityEnabled get() = isConfigured
}

@ConfigurationProperties(prefix = SecondGateInputClientConfigProperties.PREFIX)
data class SecondGateInputClientConfigProperties(
    override val baseUrl: String = "",
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : FurtherGateClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.gate-2-input"
    }

    override fun getId() = PREFIX
}

@ConfigurationProperties(prefix = SecondGateOutputClientConfigProperties.PREFIX)
data class SecondGateOutputClientConfigProperties(
    override val baseUrl: String = "",
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : FurtherGateClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.gate-2-output"
    }

    override fun getId() = PREFIX
}

@ConfigurationProperties(prefix = ThirdGateInputClientConfigProperties.PREFIX)
data class ThirdGateInputClientConfigProperties(
    override val baseUrl: String = "",
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : FurtherGateClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.gate-3-input"
    }

    override fun getId() = PREFIX
}

@ConfigurationProperties(prefix = ThirdGateOutputClientConfigProperties.PREFIX)
data class ThirdGateOutputClientConfigProperties(
    override val baseUrl: String = "",
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
) : FurtherGateClientProperties {
    companion object {
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.gate-3-output"
    }

    override fun getId() = PREFIX
}

@Configuration
class GateClientConfig{

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    @Bean
    fun gateCredentialCompanyCheck(
        inputProperties: GateInputClientConfigProperties,
        outputProperties: GateOutputClientConfigProperties,
        secondInputProperties: SecondGateInputClientConfigProperties,
        secondOutputProperties: SecondGateOutputClientConfigProperties,
        thirdInputProperties: ThirdGateInputClientConfigProperties,
        thirdOutputProperties: ThirdGateOutputClientConfigProperties,
        clientRegistrations: ClientRegistrationRepository?
    ): GateCredentialCompanyCheck {
        val credentials = sharingMemberCredentials(
            inputProperties, outputProperties,
            secondInputProperties, secondOutputProperties,
            thirdInputProperties, thirdOutputProperties
        )
        return GateCredentialCompanyCheck(credentials, clientRegistrations)
    }

    /** Returns the Gate of every sharing member this run holds credentials for. */
    @Bean
    fun sharingMemberGates(
        webClientProvider: BpdmWebClientProvider,
        inputProperties: GateInputClientConfigProperties,
        outputProperties: GateOutputClientConfigProperties,
        secondInputProperties: SecondGateInputClientConfigProperties,
        secondOutputProperties: SecondGateOutputClientConfigProperties,
        thirdInputProperties: ThirdGateInputClientConfigProperties,
        thirdOutputProperties: ThirdGateOutputClientConfigProperties,
        clientConnector: ClientHttpConnector,
        credentialCompanyCheck: GateCredentialCompanyCheck
    ): SharingMemberGates {
        credentialCompanyCheck.verify()

        val credentials = sharingMemberCredentials(
            inputProperties, outputProperties,
            secondInputProperties, secondOutputProperties,
            thirdInputProperties, thirdOutputProperties
        )

        logger.info {
            "Sharing as " + credentials.joinToString(", ") {
                "the ${it.member.name.lowercase()} sharing member at Gate '${it.input.baseUrl}'"
            }
        }

        return SharingMemberGates(credentials.map { gateOf(it, webClientProvider, clientConnector) })
    }

    /**
     * The sharing members this run can act as: the first one always, a further one where its Gate is named.
     * Both the credential check and the Gates are built from this one list, so they cannot disagree on who
     * this run is.
     */
    private fun sharingMemberCredentials(
        inputProperties: GateInputClientConfigProperties,
        outputProperties: GateOutputClientConfigProperties,
        secondInputProperties: SecondGateInputClientConfigProperties,
        secondOutputProperties: SecondGateOutputClientConfigProperties,
        thirdInputProperties: ThirdGateInputClientConfigProperties,
        thirdOutputProperties: ThirdGateOutputClientConfigProperties
    ): List<SharingMemberCredentials> = buildList {
        add(SharingMemberCredentials(SharingMember.FIRST, inputProperties, outputProperties))
        if (secondInputProperties.isConfigured)
            add(SharingMemberCredentials(SharingMember.SECOND, secondInputProperties, secondOutputProperties))
        if (thirdInputProperties.isConfigured)
            add(SharingMemberCredentials(SharingMember.THIRD, thirdInputProperties, thirdOutputProperties))
    }

    private fun gateOf(
        credentials: SharingMemberCredentials,
        webClientProvider: BpdmWebClientProvider,
        clientConnector: ClientHttpConnector
    ): SharingMemberGate {
        val inputCredential = GateClientImpl { webClientProvider.builder(credentials.input).clientConnector(clientConnector).build() }
        val outputCredential = GateClientImpl { webClientProvider.builder(credentials.output).clientConnector(clientConnector).build() }
        val client = RoleSplitGateClient(inputCredential, outputCredential)
        return SharingMemberGate(credentials.member, client, SharingStateWatcher(client, credentials.member))
    }
}