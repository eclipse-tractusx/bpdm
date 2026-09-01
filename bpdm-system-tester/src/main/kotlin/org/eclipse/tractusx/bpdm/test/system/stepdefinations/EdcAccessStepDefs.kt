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
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.test.system.config.PoolClientConfigurationProperties
import org.eclipse.tractusx.bpdm.test.system.config.SharingMemberCredentialSet
import org.eclipse.tractusx.bpdm.test.system.config.edc.EdcAccessNegotiators
import org.eclipse.tractusx.bpdm.test.system.utils.ApiCallEvidence
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMemberGates
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * Reads one page over each data offer this run consumes, to tell whether the EDC access works at all.
 *
 * Each step reports on one offer and reads only, so a run says which offers are reachable without changing
 * anything the rest of the suite would then see. What the API answers with is not asserted: an offer that
 * grants access to an empty Gate answers an empty page, and that is a pass.
 */
class EdcAccessStepDefs(
    private val poolClient: PoolApiClient,
    private val sharingMemberGates: SharingMemberGates,
    private val credentials: SharingMemberCredentialSet,
    private val negotiators: EdcAccessNegotiators,
    private val apiCallEvidence: ApiCallEvidence
) : SpringTestRunConfiguration() {

    companion object {
        private val ONE_PAGE = PaginationRequest(page = 0, size = 1)
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

    @Then("the Gate output of the {sharingMember} sharing member answers a read over the EDC")
    fun gateOutputAnswersReadOverEdc(member: SharingMember) {
        readOverOffer(credentialsOf(member).output.getId(), "POST", "/v7/output/business-partners/search") {
            sharingMemberGates.of(member).businessParters.getBusinessPartnersOutput(null, ONE_PAGE)
        }
    }

    private fun credentialsOf(member: SharingMember) =
        credentials.singleOrNull { it.member == member }
            ?: error("this run has no Gate credentials for the ${member.name.lowercase()} sharing member")

    /**
     * Reads one page over the offer the client consumes, failing with what went wrong where it cannot.
     *
     * A negotiation that never succeeded is reported as itself rather than as the read that could not be
     * made, because the two fail for entirely different reasons.
     */
    private fun readOverOffer(registrationId: String, method: String, path: String, read: () -> PageDto<*>) {
        val negotiator = negotiators.of(registrationId)
            ?: error(
                "'$registrationId' does not reach its API over an EDC in this run." +
                        " Set '$registrationId.edc.enabled' to run this scenario against a data offer."
            )

        assertThat(negotiator.isAvailable)
            .withFailMessage { "No access was negotiated for the offer '$registrationId' consumes: ${negotiator.failure?.message}" }
            .isTrue()

        val page = runCatching { read() }
            .onFailure { apiCallEvidence.attach(method, path, response = diagnosisOf(it)) }
            .getOrThrow()

        apiCallEvidence.attach(method, path, response = mapOf("totalElements" to page.totalElements))
    }

    /**
     * Names what a refused call most likely says about the offer.
     *
     * The two failures an EDC setup produces are told apart by their status alone, and both of them look like
     * an ordinary API error unless the data plane in front of the API is named.
     */
    private fun diagnosisOf(failure: Throwable): String {
        val status = (failure as? WebClientResponseException)?.statusCode?.value()
        val hint = when (status) {
            403 -> "the data plane refused the call - the access policy of the offer may not name this consumer's BPNL," +
                    " or the technical user behind the asset may lack the permission the endpoint requires"
            404 -> "the data plane found nothing at this path - the asset's 'dataAddress.baseUrl' and the path sent" +
                    " may not line up"
            else -> "the data plane answered ${status ?: "no status"}"
        }
        return "$hint (${failure.message})"
    }
}
