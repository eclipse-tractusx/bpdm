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

package org.eclipse.tractusx.bpdm.test.system.stepdefinations

import io.cucumber.java.en.Then
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.test.system.config.PoolClientConfigurationProperties
import org.eclipse.tractusx.bpdm.test.system.config.SharingMemberCredentialSet
import org.eclipse.tractusx.bpdm.test.system.config.edc.EdcAccessNegotiators
import org.eclipse.tractusx.bpdm.test.system.utils.ApiCallEvidence
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMemberGates
import org.opentest4j.TestAbortedException
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * Reads one page over each data offer this run consumes, to tell whether the EDC access works at all.
 *
 * What the API answers with is not asserted: an empty page is a pass.
 */
class EdcAccessStepDefs(
    private val poolClient: PoolApiClient,
    private val sharingMemberGates: SharingMemberGates,
    private val credentials: SharingMemberCredentialSet,
    private val negotiators: EdcAccessNegotiators,
    private val apiCallEvidence: ApiCallEvidence
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }

        private val ONE_PAGE = PaginationRequest(page = 0, size = 1)

        // The query parameter has to travel, not match.
        private const val NO_SUCH_EXTERNAL_ID = "edc-access-probe"
    }

    @Then("the Pool answers a read over the EDC")
    fun poolAnswersReadOverEdc() {
        readOverOffer(PoolClientConfigurationProperties.PREFIX, "GET", "/v7/legal-entities") {
            poolClient.legalEntities.getLegalEntities(LegalEntitySearchRequest(), ONE_PAGE)
        }
    }

    @Then("the Gate input of the {sharingMember} sharing member answers a read over the EDC")
    fun gateInputAnswersReadOverEdc(member: SharingMember) {
        readOverOffer(credentialsOf(member).input.getId(), "POST", "/v7/input/business-partners/search") {
            sharingMemberGates.of(member).businessParters.getBusinessPartnersInput(null, ONE_PAGE)
        }
    }

    @Then("the Gate input of the {sharingMember} sharing member answers a read with query parameters over the EDC")
    fun gateInputAnswersQueryReadOverEdc(member: SharingMember) {
        readOverOffer(credentialsOf(member).input.getId(), "GET", "/v7/business-partners/sharing-state") {
            sharingMemberGates.of(member).sharingState.getSharingStates(ONE_PAGE, listOf(NO_SUCH_EXTERNAL_ID))
        }
    }

    @Then("the Gate output of the {sharingMember} sharing member answers a read over the EDC")
    fun gateOutputAnswersReadOverEdc(member: SharingMember) {
        readOverOffer(credentialsOf(member).output.getId(), "POST", "/v7/output/business-partners/search") {
            sharingMemberGates.of(member).businessParters.getBusinessPartnersOutput(null, ONE_PAGE)
        }
    }

    private fun credentialsOf(member: SharingMember) =
        credentials.singleOrNull { it.member == member }
            ?: error("this run has no Gate credentials for the ${member.name.lowercase()} sharing member")

    // A client this run reaches directly is skipped rather than failed: a profile may put one API on the
    // direct route and the rest over offers, and the scenario would then report on the run's configuration.
    private fun readOverOffer(registrationId: String, method: String, path: String, read: () -> PageDto<*>) {
        val negotiator = negotiators.of(registrationId)
            ?: skip(
                "'$registrationId' does not reach its API over an EDC in this run." +
                        " Set '$registrationId.edc.enabled' and name the connector to run it."
            )

        assertThat(negotiator.isAvailable)
            .withFailMessage { "No access was negotiated for the offer '$registrationId' consumes: ${negotiator.failure?.message}" }
            .isTrue()

        val page = runCatching { read() }
            .onFailure { apiCallEvidence.attach(method, path, response = diagnosisOf(it)) }
            .getOrThrow()

        apiCallEvidence.attach(method, path, response = mapOf("totalElements" to page.totalElements))
    }

    private fun skip(reason: String): Nothing {
        val context = ScenarioContext.current()
        val message = "Skipping scenario '${context?.scenarioName ?: "unnamed"}': $reason"
        logger.warn { message }
        context?.scenario?.log(message)
        throw TestAbortedException(message)
    }

    private fun diagnosisOf(failure: Throwable): String {
        val status = (failure as? WebClientResponseException)?.statusCode?.value()
        val hint = when (status) {
            400 -> "the data plane refused to proxy the call - the asset may not carry the proxy setting the call" +
                    " needs (proxyPath, proxyQueryParams, proxyMethod or proxyBody)"
            403 -> "the data plane refused the call - the access policy of the offer may not name this consumer's BPNL," +
                    " or the technical user behind the asset may lack the permission the endpoint requires"
            404 -> "the data plane found nothing at this path - the asset's 'dataAddress.baseUrl' and the path sent" +
                    " may not line up"
            else -> "the data plane answered ${status ?: "no status"}"
        }
        val body = (failure as? WebClientResponseException)?.responseBodyAsString?.takeIf { it.isNotBlank() }
        return "$hint (${failure.message}${body?.let { ": $it" } ?: ""})"
    }
}
