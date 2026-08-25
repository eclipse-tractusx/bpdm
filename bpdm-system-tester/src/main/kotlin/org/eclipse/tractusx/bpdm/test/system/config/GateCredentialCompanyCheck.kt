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
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

/** The two Gate credentials this run acts as one sharing member with. */
data class SharingMemberCredentials(
    val member: SharingMember,
    val input: BpdmClientProperties,
    val output: BpdmClientProperties
)

/**
 * Refuses a run whose Gate credentials do not name one company per sharing member.
 *
 * The Gate answers every read with the data of the company in the token, so a mismatch does not fail: the
 * output stays empty and the run dies much later waiting for a golden record that it cannot see. Two sharing
 * members naming the same company fail just as quietly - the second member's Gate rejects its tokens, or, on
 * one shared Gate, its records are folded into the first member's data. The check is skipped wherever it
 * cannot decide - unsecured clients, one credential serving both roles, or a token that carries no company -
 * and reports the credentials it could not compare.
 */
class GateCredentialCompanyCheck(
    private val credentials: List<SharingMemberCredentials>,
    private val clientRegistrations: ClientRegistrationRepository?
) {

    companion object {
        private val logger = KotlinLogging.logger { }

        private const val COMPANY_CLAIM = "bpn"
    }

    private val tokenResponseClient = RestClientClientCredentialsTokenResponseClient()

    /** Fetches a token per Gate credential and fails unless each sharing member acts for a company of its own. */
    fun verify() {
        val companies = credentials.mapNotNull { member -> companyOf(member)?.let { member.member to it } }
        verifyMembersDiffer(companies)
    }

    private fun companyOf(credentials: SharingMemberCredentials): String? {
        val memberName = credentials.member.name.lowercase()
        val inputClientId = credentials.input.registration.clientId
        val outputClientId = credentials.output.registration.clientId

        if (clientRegistrations == null || !credentials.input.securityEnabled || !credentials.output.securityEnabled) {
            logger.info { "Not verifying the company of the $memberName sharing member: its Gate clients are not both secured." }
            return null
        }

        if (inputClientId == outputClientId && credentials.input.provider.issuerUri == credentials.output.provider.issuerUri) {
            logger.info { "Gate client '$inputClientId' serves both the input and the output role of the $memberName sharing member." }
            return companyInTokenOf(credentials.input)
        }

        val inputCompany = companyInTokenOf(credentials.input)
        val outputCompany = companyInTokenOf(credentials.output)
        if (inputCompany == null || outputCompany == null) {
            logger.warn {
                "Cannot verify that the Gate clients '$inputClientId' (input) and '$outputClientId' (output) of the" +
                        " $memberName sharing member act for the same company: no '$COMPANY_CLAIM' claim in the token" +
                        " of at least one of them."
            }
            return null
        }

        check(inputCompany == outputCompany) {
            "The Gate clients of the $memberName sharing member act for different companies: '$inputClientId' (input)" +
                    " for $inputCompany, '$outputClientId' (output) for $outputCompany. The Gate answers a read with the" +
                    " data of the company in the token, so the output would stay empty for everything this run shares." +
                    " Configure both clients from one company."
        }

        logger.info { "Gate clients '$inputClientId' (input) and '$outputClientId' (output) act as $inputCompany, the $memberName sharing member." }
        return inputCompany
    }

    private fun verifyMembersDiffer(companies: List<Pair<SharingMember, String>>) {
        val membersByCompany = companies.groupBy({ it.second }, { it.first })
        val shared = membersByCompany.filterValues { it.size > 1 }
        check(shared.isEmpty()) {
            shared.entries.joinToString(prefix = "Sharing members that act for the same company: ") { (company, members) ->
                "${members.joinToString(" and ") { it.name.lowercase() }} act as $company"
            } + ". A golden record shared by them would still count as one sharing member, and a Gate owned by one" +
                    " company rejects the tokens of another. Configure each sharing member from a company of its own."
        }
    }

    private fun companyInTokenOf(credential: BpdmClientProperties): String? {
        val registration = clientRegistrations!!.findByRegistrationId(credential.getId()) ?: return null
        val tokenResponse = tokenResponseClient.getTokenResponse(OAuth2ClientCredentialsGrantRequest(registration))
        return runCatching { JWTParser.parse(tokenResponse.accessToken.tokenValue).jwtClaimsSet.getStringClaim(COMPANY_CLAIM) }.getOrNull()
    }
}
