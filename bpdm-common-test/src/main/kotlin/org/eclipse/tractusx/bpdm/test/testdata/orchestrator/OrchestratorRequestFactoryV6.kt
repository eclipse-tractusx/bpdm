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
import org.eclipse.tractusx.orchestrator.api.model.UncategorizedProperties
import org.eclipse.tractusx.orchestrator.api.v6.model.BusinessPartner
import org.eclipse.tractusx.orchestrator.api.v6.model.LegalEntity
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskCreateRequestEntry
import java.util.*
import kotlin.random.Random

class OrchestratorRequestFactoryV6(
    private val commonFactory: OrchestratorRequestFactoryCommon
) {

    fun buildTaskCreate(seed: String): TaskCreateRequestEntry{
        return TaskCreateRequestEntry(
            recordId = UUID.randomUUID().toString(),
            businessPartner = buildBusinessPartner(seed)
        )
    }


    fun buildBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartner{
        return BusinessPartner(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = commonFactory.buildUncategorizedProperties(seed, random),
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random),
            additionalAddress = commonFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random)
        )
    }

    fun buildLegalEntityBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartner{
        return BusinessPartner(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedProperties.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = null,
            additionalAddress = null
        )
    }

    fun buildSiteBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartner{
        return BusinessPartner(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedProperties.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random),
            additionalAddress = null
        )
    }

    fun buildLegalAddressSiteBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartner{
        return BusinessPartner(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedProperties.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random).copy(siteMainAddress = null),
            additionalAddress = null
        )
    }

    fun buildLegalEntityAdditionalAddressBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartner{
        return BusinessPartner(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedProperties.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = null,
            additionalAddress = commonFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random)
        )
    }

    fun buildSiteAdditionalAddressBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartner{
        return BusinessPartner(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedProperties.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random),
            additionalAddress = commonFactory.buildPostalAddress(seed, AddressType.AdditionalAddress, random)
        )
    }


    fun buildLegalEntityProperties(seed: String, random: Random = commonFactory.createRandomFromSeed(seed)): LegalEntity {
        return LegalEntity(
            bpnReference = commonFactory.buildBpnLReference(seed),
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = commonFactory.metadata?.legalForms?.random(random) ?: "Legal Form $seed",
            identifiers = commonFactory.buildLegalIdentifiers(seed, random),
            states = commonFactory.buildStates(random),
            confidenceCriteria = commonFactory.buildConfidenceCriteria(random),
            isCatenaXMemberData = random.nextBoolean(),
            hasChanged = random.nextBoolean(),
            legalAddress = commonFactory.buildPostalAddress(seed, AddressType.LegalAddress, random)
        )
    }


    private fun createRandomFromSeed(seed: String): Random{
        return Random(seed.hashCode())
    }
}