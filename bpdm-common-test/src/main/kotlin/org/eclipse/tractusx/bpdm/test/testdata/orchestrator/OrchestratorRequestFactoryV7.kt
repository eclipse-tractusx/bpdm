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
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.BusinessPartnerV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.BusinessPartnerRelationsV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.LegalEntityV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.relationtask.TaskCreateRelationsRequestEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskCreateRequestEntryV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner.UncategorizedPropertiesV7Dto
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

class OrchestratorRequestFactoryV7(
    private val businessPartnerTestDataFactory: BusinessPartnerTestDataFactory,
    private val commonFactory: OrchestratorRequestFactoryCommon
) {

    fun buildBusinessPartnerTaskCreateEntry(seed: String): TaskCreateRequestEntryV7Dto {
        return TaskCreateRequestEntryV7Dto(
            recordId = UUID.randomUUID().toString(),
            businessPartner = buildAdditionalAddressOfSiteBusinessPartner(seed)
        )
    }

    fun buildRelationTaskCreateEntry(seed: String, recordId: String?): TaskCreateRelationsRequestEntryV7Dto {
        return TaskCreateRelationsRequestEntryV7Dto(
            recordId = recordId,
            businessPartnerRelations = buildRelation(seed)
        )
    }

    fun buildRelation(seed: String): BusinessPartnerRelationsV7Dto{
        val random = createRandomFromSeed(seed)
        return BusinessPartnerRelationsV7Dto(
            RelationTypeV7.entries.random(random),
            "$seed Source BpnL",
            "$seed Target BpnL",
            validityPeriods = listOf(
                RelationValidityPeriodV7Dto(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)),
                RelationValidityPeriodV7Dto(LocalDate.of(2026, 1, 1), null),
            ),
            reasonCode = commonFactory.metadata!!.reasonCodes.random(random)
        )
    }

    fun buildAdditionalAddressOfSiteBusinessPartner(seed: String): BusinessPartnerV7Dto{
        return businessPartnerTestDataFactory.createFullBusinessPartner(seed)
    }

    fun buildLegalEntityBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV7Dto {
        return BusinessPartnerV7Dto(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV7Dto.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = null,
            additionalAddress = null
        )
    }

    fun buildSiteBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV7Dto {
        return BusinessPartnerV7Dto(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV7Dto.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random),
            additionalAddress = null
        )
    }

    fun buildSiteWithAdditionalSitesBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV7Dto {
        return buildSiteBusinessPartner(seed, random).copy(additionalSites = commonFactory.buildAdditionalSites(seed))
    }

    fun buildLegalAddressSiteBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV7Dto {
        return BusinessPartnerV7Dto(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV7Dto.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random).copy(siteMainAddress = null),
            additionalAddress = null
        )
    }

    fun buildLegalEntityAdditionalAddressBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV7Dto {
        return BusinessPartnerV7Dto(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV7Dto.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = null,
            additionalAddress = commonFactory.buildPostalAddressWithScripVariants(seed, AddressType.AdditionalAddress, random)
        )
    }

    fun buildSiteAdditionalAddressBusinessPartner(seed: String, random: Random = createRandomFromSeed(seed)): BusinessPartnerV7Dto {
        return BusinessPartnerV7Dto(
            nameParts = commonFactory.buildNameParts(seed),
            owningCompany = "BPNLOwner",
            uncategorized = UncategorizedPropertiesV7Dto.empty,
            legalEntity = buildLegalEntityProperties(seed, random),
            site = commonFactory.buildSite(seed, random),
            additionalAddress = commonFactory.buildPostalAddressWithScripVariants(seed, AddressType.AdditionalAddress, random)
        )
    }


    fun buildLegalEntityProperties(seed: String, random: Random = commonFactory.createRandomFromSeed(seed)): LegalEntityV7Dto {
        return LegalEntityV7Dto(
            bpnReference = commonFactory.buildBpnLReference(seed),
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = commonFactory.metadata?.legalForms?.random(random) ?: "Legal Form $seed",
            identifiers = commonFactory.buildLegalIdentifiers(seed, random),
            states = commonFactory.buildStates(random),
            confidenceCriteria = commonFactory.buildConfidenceCriteria(random),
            isParticipantData = random.nextBoolean(),
            hasChanged = random.nextBoolean(),
            legalAddress = commonFactory.buildPostalAddress(seed, AddressType.LegalAddress, random),
            scriptVariants = listOf(commonFactory.buildLegalEntityScriptVariant(seed, random)),
            updatedAt = commonFactory.buildUpdatedAt(random)
        )
    }

    private fun createRandomFromSeed(seed: String): Random{
        return Random(seed.hashCode())
    }




}