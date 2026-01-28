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

package org.eclipse.tractusx.bpdm.test.testdata.orchestrator

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.model.NamePart
import org.eclipse.tractusx.orchestrator.api.model.NamePartType
import kotlin.random.Random


class BusinessPartnerTestDataFactory(
    private val orchestratorRequestFactory: OrchestratorRequestFactoryCommon
){

    val metadata = orchestratorRequestFactory.metadata

    //Business Partner with maximal fields filled
    fun createFullBusinessPartner(seed: String = "Test"): BusinessPartner {
        val random = orchestratorRequestFactory.createRandomFromSeed(seed)

        return BusinessPartner(
            nameParts = listOf(
                NamePart("Legal Name $seed", NamePartType.LegalName),
                NamePart("Site Name $seed", NamePartType.SiteName),
                NamePart("Address Name $seed", NamePartType.AddressName),
                NamePart("Legal Short Name $seed", NamePartType.ShortName),
                NamePart("Legal Form $seed", NamePartType.LegalForm)
            ),
            owningCompany = "Owner Company BPNL $seed",
            uncategorized = orchestratorRequestFactory.buildUncategorizedProperties(seed, random),
            legalEntity = createLegalEntity(seed, random),
            site = orchestratorRequestFactory.buildSite(seed, random),
            additionalAddress = orchestratorRequestFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random)
        )
    }

    fun createLegalEntityBusinessPartner(seed: String = "Test"): BusinessPartner {
        return BusinessPartner.empty.copy(legalEntity = createLegalEntity(seed))
    }

    fun createSiteBusinessPartner(seed: String = "Test"): BusinessPartner {
        return BusinessPartner.empty.copy(
            legalEntity = createLegalEntity(seed),
            site = orchestratorRequestFactory.buildSite(seed)
        )
    }

    fun createLegalEntity(seed: String = "Test", random: Random = orchestratorRequestFactory.createRandomFromSeed(seed)): LegalEntity {
        return LegalEntity(
            bpnReference = orchestratorRequestFactory.buildBpnLReference(seed),
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = orchestratorRequestFactory.metadata?.legalForms?.random(random) ?: "Legal Form $seed",
            identifiers = orchestratorRequestFactory.buildLegalIdentifiers(seed, random),
            states = orchestratorRequestFactory.buildStates(random),
            confidenceCriteria = orchestratorRequestFactory.buildConfidenceCriteria(random),
            isParticipantData = random.nextBoolean(),
            hasChanged = true,
            legalAddress = orchestratorRequestFactory.buildPostalAddress(seed, AddressType.LegalAddress, random)
        )
    }

}
