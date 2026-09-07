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
import io.cucumber.java.en.When
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.test.system.utils.BusinessPartnerShareActions
import org.eclipse.tractusx.bpdm.test.system.utils.ConfidenceAssertHelper
import org.eclipse.tractusx.bpdm.test.system.utils.ConfidenceLevel
import org.eclipse.tractusx.bpdm.test.system.utils.OutputComponent
import org.eclipse.tractusx.bpdm.test.system.utils.ScenarioContext
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember

/**
 * Steps for the "Output Reflects Golden Record Confidence Criteria" feature.
 *
 * The owner signal (whether the data is owner-shared) is only *requested* by the share step:
 * "the sharing member shares own company record {string}" requests it, while
 * "the sharing member shares third-party record {string}" does not. Spelling both out keeps the confidence
 * scenarios explicit about the owner signal instead of relying on the implicit default of the general share
 * step. It is the refine step, however, that *finally assigns* the owner signal: the golden record process
 * decides what the record actually is and assigns the signal only to the entity the record is refined to. A
 * parent entity the process determines (e.g. the legal entity above a refined-to additional address) never
 * inherits the owner signal. The verification signal is set by the refine step variants below. Each refine
 * step waits for the golden record process to complete so the output is available for the Then assertions.
 */
class ConfidenceCriteriaStepDefs(
    private val shareActions: BusinessPartnerShareActions,
    private val confidenceAssertHelper: ConfidenceAssertHelper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    // -------------------------------------------------------------------------
    // When - share (owner signal)
    // -------------------------------------------------------------------------

    @When("the sharing member shares own company record {string}")
    fun `when shares own company record`(recordId: String) {
        logger.info { "[$scenarioName] When: the sharing member shares own company record '$recordId'" }
        // Own company data requests the OwnerShared signal; the refine step finally assigns it, and only to
        // the entity the record is refined to. Same behaviour as the general "the sharing member shares record"
        // step; spelled out here to make the owner signal explicit in the confidence scenarios.
        shareActions.upload(recordId, isOwnCompanyData = true)
    }

    @When("the {sharingMember} sharing member shares own company record {string}")
    fun `when member shares own company record`(member: SharingMember, recordId: String) {
        logger.info { "[$scenarioName] When: the ${member.name.lowercase()} sharing member shares own company record '$recordId'" }
        shareActions.upload(recordId, isOwnCompanyData = true, member = member)
    }

    @When("the sharing member shares third-party record {string}")
    fun `when shares third-party record`(recordId: String) {
        logger.info { "[$scenarioName] When: the sharing member shares third-party record '$recordId'" }
        // Not own company data, so the OwnerShared signal is off (sharedByOwner = false).
        shareActions.upload(recordId, isOwnCompanyData = false)
    }

    @When("the {sharingMember} sharing member shares third-party record {string}")
    fun `when member shares third-party record`(member: SharingMember, recordId: String) {
        logger.info { "[$scenarioName] When: the ${member.name.lowercase()} sharing member shares third-party record '$recordId'" }
        shareActions.upload(recordId, isOwnCompanyData = false, member = member)
    }

    // -------------------------------------------------------------------------
    // When - refine (verification signal + matched entity)
    //
    // Each step routes through the seed-and-label refine overloads, which build the golden record with stable
    // request identifiers and wait for the completed sharing state internally, so the output golden record is
    // ready for the Then assertions. Every record in this feature owns its golden records, so the master data
    // seed and entity labels are derived from the record id. Parent entities (legal entity, site) the matched
    // entity hangs off of get their own suffixed labels so their golden record identifiers stay distinct. A
    // determined parent entity never inherits the owner signal: refining a record shared as own company data to
    // an additional address makes only that address OwnerShared, leaving the determined parent at NoConfidence.
    // -------------------------------------------------------------------------

    @When("the golden record process refines record {string} to a legal entity without external verification")
    fun `when refines to legal entity without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to a legal entity without external verification" }
        shareActions.refineAsLegalEntity(recordId, masterDataSeed = recordId, legalEntityLabel = recordId, verified = false)
    }

    @When("the golden record process refines record {string} to a legal entity with external verification")
    fun `when refines to legal entity with verification`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to a legal entity with external verification" }
        shareActions.refineAsLegalEntity(recordId, masterDataSeed = recordId, legalEntityLabel = recordId, verified = true)
    }

    @When("the golden record process refines record {string} to an additional address of a legal entity without external verification")
    fun `when refines to additional address of legal entity without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to an additional address of a legal entity without external verification" }
        shareActions.refineAsAdditionalAddressOfLegalEntity(
            recordId, masterDataSeed = recordId, additionalAddressLabel = recordId, legalEntityLabel = "$recordId-le", verified = false
        )
    }

    @When("the golden record process refines record {string} to an additional address of a legal entity with external verification")
    fun `when refines to additional address of legal entity with verification`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to an additional address of a legal entity with external verification" }
        shareActions.refineAsAdditionalAddressOfLegalEntity(
            recordId, masterDataSeed = recordId, additionalAddressLabel = recordId, legalEntityLabel = "$recordId-le", verified = true
        )
    }

    @When("the golden record process refines record {string} to a site")
    fun `when refines to site`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to a site" }
        // A site always carries OwnerShared confidence, so there is no verification variant for it.
        shareActions.refineAsSite(recordId, masterDataSeed = recordId, siteLabel = recordId, legalEntityLabel = "$recordId-le")
    }

    @When("the golden record process refines record {string} to a site-based legal entity without external verification")
    fun `when refines to site-based legal entity without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to a site-based legal entity without external verification" }
        shareActions.refineAsSiteBasedLegalEntity(recordId, masterDataSeed = recordId, siteLabel = "$recordId-site", legalEntityLabel = recordId, verified = false)
    }

    @When("the golden record process refines record {string} to an additional address of a site without external verification")
    fun `when refines to additional address of site without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: refines '$recordId' to an additional address of a site without external verification" }
        // The seed-and-label overload builds the additional address with OwnerShared confidence, which is what
        // this feature's site additional-address scenario expects.
        shareActions.refineAsAdditionalAddressOfSite(
            recordId, masterDataSeed = recordId, additionalAddressLabel = recordId, siteLabel = "$recordId-site", legalEntityLabel = "$recordId-le"
        )
    }

    // -------------------------------------------------------------------------
    // Then - the output reflects the golden record's confidence criteria
    // -------------------------------------------------------------------------

    // Single step definition covering all level/component combinations.
    // Regex captures: recordId, confidence expression, component.
    @Then("^\"([^\"]+)\" output reflects (NoConfidence|OwnerShared confidence|Verified confidence|VerifiedOwnerShared confidence) for its (legal entity|legal address|additional address|site)$")
    fun `then output reflects confidence`(recordId: String, confidenceExpr: String, componentExpr: String) {
        val level = when (confidenceExpr) {
            "NoConfidence"                   -> ConfidenceLevel.NO_CONFIDENCE
            "OwnerShared confidence"         -> ConfidenceLevel.OWNER_SHARED
            "Verified confidence"            -> ConfidenceLevel.VERIFIED
            "VerifiedOwnerShared confidence" -> ConfidenceLevel.VERIFIED_OWNER_SHARED
            else -> error("Unknown confidence level: '$confidenceExpr'")
        }
        logger.info { "[$scenarioName] Then: '$recordId' output reflects $confidenceExpr for its $componentExpr" }
        confidenceAssertHelper.assertConfidence(recordId, componentOf(componentExpr), level)
    }

    // The sharing member count is the one confidence criterion the refinement does not state, so its own step
    // asserts it: the Pool counts the sharing members that share a golden record, independent of any signal a
    // single record carries.
    @Then("^\"([^\"]+)\" output reflects a sharing member count of (\\d+) for its (legal entity|legal address|additional address|site)$")
    fun `then output reflects sharing member count`(recordId: String, expectedCount: Int, componentExpr: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects a sharing member count of $expectedCount for its $componentExpr" }
        confidenceAssertHelper.assertSharingMemberCount(recordId, componentOf(componentExpr), expectedCount)
    }

    // The confidence level is derived from the criteria, and the number of sharing members only counts towards
    // it from a threshold on, so it takes a scenario with enough sharing members to move it at all.
    @Then("^\"([^\"]+)\" output reflects a confidence level of (\\d+) for its (legal entity|legal address|additional address|site)$")
    fun `then output reflects confidence level`(recordId: String, expectedLevel: Int, componentExpr: String) {
        logger.info { "[$scenarioName] Then: '$recordId' output reflects a confidence level of $expectedLevel for its $componentExpr" }
        confidenceAssertHelper.assertConfidenceLevel(recordId, componentOf(componentExpr), expectedLevel)
    }

    private fun componentOf(componentExpr: String) = when (componentExpr) {
        "legal entity"       -> OutputComponent.LEGAL_ENTITY
        "legal address"      -> OutputComponent.LEGAL_ADDRESS
        "additional address" -> OutputComponent.ADDITIONAL_ADDRESS
        "site"               -> OutputComponent.SITE
        else -> error("Unknown output component: '$componentExpr'")
    }
}
