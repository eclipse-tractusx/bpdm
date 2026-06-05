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

class ConfidenceCriteriaStepDefs(
    private val shareActions: BusinessPartnerShareActions,
    private val confidenceAssertHelper: ConfidenceAssertHelper
) : SpringTestRunConfiguration() {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val context: ScenarioContext get() = ScenarioContext.current()!!
    private val scenarioName: String get() = context.scenarioName

    @When("a sharing member uploads a business partner record {string}")
    fun `when upload business partner record`(recordId: String) {
        logger.info { "[$scenarioName] When: sharing member uploads business partner record '$recordId'" }
        shareActions.upload(recordId, isOwnCompanyData = false)
    }

    @When("the cleaning service provider refines {string} as a legal entity without external verification")
    fun `when refines as legal entity without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as a legal entity without external verification" }
        shareActions.refineAsLegalEntity(recordId, verified = false)
    }

    @When("the cleaning service provider refines {string} as a legal entity with external verification")
    fun `when refines as legal entity with verification`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as a legal entity with external verification" }
        shareActions.refineAsLegalEntity(recordId, verified = true)
    }

    @When("the cleaning service provider refines {string} as a site-based legal entity without external verification")
    fun `when refines as site-based legal entity without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as a site-based legal entity without external verification" }
        shareActions.refineAsSiteBasedLegalEntity(recordId, verified = false)
    }

    @When("the cleaning service provider refines {string} as an additional address of a legal entity without external verification")
    fun `when refines as additional address of legal entity without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as an additional address of a legal entity without external verification" }
        shareActions.refineAsAdditionalAddressOfLegalEntity(recordId, verified = false)
    }

    @When("the cleaning service provider refines {string} as an additional address of a legal entity with external verification")
    fun `when refines as additional address of legal entity with verification`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as an additional address of a legal entity with external verification" }
        shareActions.refineAsAdditionalAddressOfLegalEntity(recordId, verified = true)
    }

    @When("the cleaning service provider refines {string} as an additional address of a site without external verification")
    fun `when refines as additional address of site without verification`(recordId: String) {
        logger.info { "[$scenarioName] When: cleaning service provider refines '$recordId' as an additional address of a site without external verification" }
        shareActions.refineAsAdditionalAddressOfSite(recordId, verified = false)
    }

    // Single step definition covering all component/level combinations from confidence_criteria.feature.
    // Regex captures: component (legal entity|legal address|additional address|site), recordId, confidence expression.
    @Then("^the (legal entity|legal address|additional address|site) of \"([^\"]+)\" has (NoConfidence|OwnerShared confidence|Verified confidence|VerifiedOwnerShared confidence)$")
    fun `then component confidence`(componentExpr: String, recordId: String, confidenceExpr: String) {
        val component = when (componentExpr) {
            "legal entity"      -> OutputComponent.LEGAL_ENTITY
            "legal address"     -> OutputComponent.LEGAL_ADDRESS
            "additional address" -> OutputComponent.ADDITIONAL_ADDRESS
            "site"              -> OutputComponent.SITE
            else -> error("Unknown output component: '$componentExpr'")
        }
        val level = when (confidenceExpr) {
            "NoConfidence"              -> ConfidenceLevel.NO_CONFIDENCE
            "OwnerShared confidence"    -> ConfidenceLevel.OWNER_SHARED
            "Verified confidence"       -> ConfidenceLevel.VERIFIED
            "VerifiedOwnerShared confidence" -> ConfidenceLevel.VERIFIED_OWNER_SHARED
            else -> error("Unknown confidence level: '$confidenceExpr'")
        }
        logger.info { "[$scenarioName] Then: $componentExpr of '$recordId' has $confidenceExpr" }
        confidenceAssertHelper.assertConfidence(recordId, component, level)
    }
}
