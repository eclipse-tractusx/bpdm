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

import com.nimbusds.jwt.JWTParser
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.BpdmClientProperties
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

/**
 * Refuses a run whose two Gate credentials act for different companies.
 *
 * The Gate answers every read with the data of the company in the token, so credentials from two companies do
 * not fail: the output stays empty and the run dies much later waiting for a golden record that it cannot see.
 * The check is skipped wherever it cannot decide - unsecured clients, one credential serving both roles, or a
 * token that carries no company - and reports the credentials it could not compare.
 */
class GateCredentialCompanyCheck(
    private val inputCredential: BpdmClientProperties,
    private val outputCredential: BpdmClientProperties,
    private val clientRegistrations: ClientRegistrationRepository?
) {

    companion object {
        private val logger = KotlinLogging.logger { }

        private const val COMPANY_CLAIM = "bpn"
    }

    private val tokenResponseClient = RestClientClientCredentialsTokenResponseClient()

    /** Fetches a token for each Gate credential and fails unless both name the same company. */
    fun verify() {
        val inputClientId = inputCredential.registration.clientId
        val outputClientId = outputCredential.registration.clientId

        if (clientRegistrations == null || !inputCredential.securityEnabled || !outputCredential.securityEnabled) {
            logger.info { "Not verifying the company of the Gate credentials: the Gate clients are not both secured." }
            return
        }

        if (inputClientId == outputClientId && inputCredential.provider.issuerUri == outputCredential.provider.issuerUri) {
            logger.info { "Gate client '$inputClientId' serves both the input and the output role." }
            return
        }

        val inputCompany = companyOf(inputCredential)
        val outputCompany = companyOf(outputCredential)
        if (inputCompany == null || outputCompany == null) {
            logger.warn {
                "Cannot verify that the Gate clients '$inputClientId' (input) and '$outputClientId' (output) act for the" +
                        " same company: no '$COMPANY_CLAIM' claim in the token of at least one of them."
            }
            return
        }

        check(inputCompany == outputCompany) {
            "The Gate clients act for different companies: '$inputClientId' (input) for $inputCompany, '$outputClientId'" +
                    " (output) for $outputCompany. The Gate answers a read with the data of the company in the token, so" +
                    " the output would stay empty for everything this run shares. Configure both clients from one company."
        }

        logger.info { "Gate clients '$inputClientId' (input) and '$outputClientId' (output) both act for $inputCompany." }
    }

    private fun companyOf(credential: BpdmClientProperties): String? {
        val registration = clientRegistrations!!.findByRegistrationId(credential.getId()) ?: return null
        val tokenResponse = tokenResponseClient.getTokenResponse(OAuth2ClientCredentialsGrantRequest(registration))
        return runCatching { JWTParser.parse(tokenResponse.accessToken.tokenValue).jwtClaimsSet.getStringClaim(COMPANY_CLAIM) }.getOrNull()
    }
}
