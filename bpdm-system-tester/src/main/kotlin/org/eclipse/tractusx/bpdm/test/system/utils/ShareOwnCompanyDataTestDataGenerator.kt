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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteHeaderScriptVariantDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SitePartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnReferenceType
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.GivenConfidence
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withConfidence
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import java.time.Instant

/**
 * Generates test data for the ShareOwnCompanyData system tests.
 *
 * Uses a two-seed strategy to make repeated test executions independent without database clearing:
 * - [scenarioUniqueId]: seeds all structural/enumerated values (names, addresses, legal forms, etc.)
 *   so they are stable across runs for the same scenario.
 * - [runUniqueId]: replaces identifier values and BPN references so each test run produces
 *   values that cannot collide with records created by previous runs.
 */
class ShareOwnCompanyDataTestDataGenerator(
    private val poolRequestFactory: PoolRequestFactoryV7,
    private val poolResponseFactory: PoolResponseFactoryV7,
    private val refinementTestDataFactory: RefinementTestDataFactory,
    private val testDataFactoryGate: TestDataFactoryGateV7
) {

    data class SiteBasedLegalEntityResult(
        val siteBasedLegalEntity: SiteBasedLegalEntity,
        val taskData: BusinessPartner
    )

    data class LegalEntityResult(
        val legalEntity: LegalEntityWithLegalAddressVerboseDto,
        val taskData: BusinessPartner
    )

    data class SiteResult(
        val siteWithParent: SiteWithParent,
        val taskData: BusinessPartner
    )

    data class AdditionalSiteAddressResult(
        val additionalSiteAddressWithParent: AdditionalSiteAddressWithParent,
        val taskData: BusinessPartner
    )

    data class AdditionalLegalEntityAddressResult(
        val additionalLegalEntityAddressWithParent: AdditionalLegalEntityAddressWithParent,
        val taskData: BusinessPartner
    )

    fun buildSiteBasedLegalEntity(id: String, givenConfidence: GivenConfidence = TestDataV7.SharedByOwner): SiteBasedLegalEntityResult {
        val context = ScenarioContext.current()!!
        val legalEntity = buildLegalEntityResponse(scenarioId(id), context.runId(id), AddressType.LegalAndSiteMainAddress, givenConfidence)

        val siteCreate = with(poolRequestFactory.buildLegalSiteCreateRequest(scenarioId(id), legalEntity.header.bpnl)) {
            copy(scriptVariants = scriptVariants.zip(legalEntity.scriptVariants) { siteScript, leScript ->
                SiteHeaderScriptVariantDto(leScript.scriptCode, siteScript.name)
            })
        }.let { poolResponseFactory.buildLegalSiteCreate(it, legalEntity, "BPNS${context.runId(id)}") }

        val taskData = refinementTestDataFactory.buildLegalEntityOnSiteBusinessPartner(
            legalEntity, siteCreate.site, "BPNL000000000001", emptyList()
        ).copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return SiteBasedLegalEntityResult(SiteBasedLegalEntity(legalEntity, siteCreate.site), taskData)
    }

    fun buildLegalEntity(id: String, givenConfidence: GivenConfidence = TestDataV7.SharedByOwner): LegalEntityResult {
        val context = ScenarioContext.current()!!
        val legalEntity = buildLegalEntityResponse(scenarioId(id), context.runId(id), AddressType.LegalAddress, givenConfidence)

        val taskData = refinementTestDataFactory.buildLegalEntityBusinessPartner(legalEntity, "BPNL000000000001", emptyList())
            .copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return LegalEntityResult(legalEntity, taskData)
    }

    /**
     * Builds a site under [legalEntity], its main address included. [sharedMainAddressId] pins the site's script codes to
     * that shared identity instead of the site's own seed: sites sharing one main address are all covered by that one
     * address, so they have to be named in the same scripts.
     */
    fun buildSite(id: String, legalEntity: LegalEntityWithLegalAddressVerboseDto, sharedMainAddressId: String? = null): SiteResult {
        val context = ScenarioContext.current()!!
        val legalEntityParent = legalEntity.copy(header = legalEntity.header.withConfidence(TestDataV7.SharedByOwnerConfidence))

        val siteCreate = poolRequestFactory.buildSiteCreateRequest(scenarioId(id), legalEntityParent.header.bpnl)
            .withScriptCodesOf(sharedMainAddressId)
            .let { poolResponseFactory.buildSiteSiteCreate(it, bpnS = "BPNS${context.runId(id)}", bpnA = "BPNA${context.runId(id)}") }
        val site = with(poolResponseFactory.buildSiteSearchResponse(siteCreate)){ copy(mainAddress = mainAddress.withConfidence(TestDataV7.SyncedSharedByOwnerConfidence)) }
        val siteWithParent = SiteWithParent(legalEntityParent, site)

        val taskData = refinementTestDataFactory.buildSiteBusinessPartner(legalEntityParent, site, "BPNL000000000001", emptyList())
            .copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return SiteResult(siteWithParent, taskData)
    }

    fun buildInputData(id: String): BusinessPartnerInputRequest {
        val context = ScenarioContext.current()!!
        return testDataFactoryGate.businessPartner.input.request.fromSeed(scenarioId(id))
            .withRunUniqueReferences(context.runId(id))
            .copy(externalSequenceTimestamp = Instant.now())
    }

    fun buildAdditionalSiteAddress(
        id: String,
        siteWithParent: SiteWithParent,
        givenConfidence: GivenConfidence = TestDataV7.SharedByOwner
    ): AdditionalSiteAddressResult {
        val context = ScenarioContext.current()!!
        // The legal entity above a refined-to additional address is a parent the golden record process
        // determined, so it never inherits the owner signal (see [asDeterminedParent]). The site keeps its
        // own confidence: a site always carries OwnerShared confidence.
        val determinedParent = siteWithParent.copy(legalEntity = siteWithParent.legalEntity.asDeterminedParent())
        val request = poolRequestFactory.buildAdditionalAddressCreateRequest(scenarioId(id), determinedParent.site.site.bpns)
            .withConfidence(givenConfidence)
            .withRunUniqueIdentifiers(context.runId(id))

        val additionalAddress = poolResponseFactory
            .buildAdditionalAddressCreate(request, determinedParent.legalEntity, "BPNA${context.runId(id)}")
            .let { it.copy(address = it.address.copy(bpnSite = determinedParent.site.site.bpns)) }
            .withConfidence(TestDataV7.SyncedSharedByOwnerConfidence)
            .let { poolResponseFactory.buildAddressSearchResponse(it) }

        val taskData = refinementTestDataFactory.buildAdditionSiteAddressBusinessPartner(
            determinedParent.legalEntity, determinedParent.site, additionalAddress, "BPNL000000000001", emptyList()
        ).copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return AdditionalSiteAddressResult(AdditionalSiteAddressWithParent(determinedParent, additionalAddress), taskData)
    }

    fun buildAdditionalLegalEntityAddress(
        id: String,
        legalEntity: LegalEntityWithLegalAddressVerboseDto,
        givenConfidence: GivenConfidence = TestDataV7.SharedByOwner
    ): AdditionalLegalEntityAddressResult {
        val context = ScenarioContext.current()!!
        // The legal entity above a refined-to additional address is a parent the golden record process
        // determined, so it never inherits the owner signal the address was shared with (see
        // [asDeterminedParent]).
        val legalEntityParent = legalEntity.asDeterminedParent()

        val request = poolRequestFactory.buildAdditionalAddressCreateRequest(scenarioId(id), legalEntity)
            .withConfidence(givenConfidence)
            .withRunUniqueIdentifiers(context.runId(id))

        val additionalAddress = poolResponseFactory
            .buildAdditionalAddressCreate(request, legalEntityParent, "BPNA${context.runId(id)}")
            .withConfidence(TestDataV7.SyncedSharedByOwnerConfidence)
            .let { poolResponseFactory.buildAddressSearchResponse(it) }

        val taskData = refinementTestDataFactory.buildAdditionLegalEntityAddressBusinessPartner(
            legalEntityParent, additionalAddress, "BPNL000000000001", emptyList()
        ).copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return AdditionalLegalEntityAddressResult(AdditionalLegalEntityAddressWithParent(legalEntityParent, additionalAddress), taskData)
    }

    /**
     * Renders a legal entity as a parent the golden record process *determined* (the legal entity above a
     * refined-to site or additional address). The owner signal a record was shared with is finally assigned by
     * the refinement step and belongs only to the entity the record was refined to; a determined parent never
     * inherits it. So a determined parent always carries NoConfidence (owner signal off, not externally
     * verified, confidence level 0).
     */
    private fun LegalEntityWithLegalAddressVerboseDto.asDeterminedParent(): LegalEntityWithLegalAddressVerboseDto =
        withConfidence(TestDataV7.NotCheckedNotOwned).withConfidence(TestDataV7.NoConfidence)

    private fun scenarioId(id: String) = "$id${ScenarioContext.current()!!.scenarioSuffix}"

    /** Rewrites the site's script variants to the script code [sharedMainAddressId] seeds, leaving them alone when null. */
    private fun SitePartnerCreateRequest.withScriptCodesOf(sharedMainAddressId: String?): SitePartnerCreateRequest {
        val sharedScriptCode = sharedMainAddressId?.let { poolRequestFactory.scriptCodeFor(scenarioId(it)) } ?: return this
        return copy(site = site.copy(scriptVariants = site.scriptVariants.map { it.copy(scriptCode = sharedScriptCode) }))
    }

    private fun buildLegalEntityResponse(
        scenarioUniqueId: String,
        runUniqueId: String,
        legalAddressType: AddressType,
        givenConfidence: GivenConfidence = TestDataV7.SharedByOwner
    ): LegalEntityWithLegalAddressVerboseDto {
        return with(
            poolRequestFactory.buildLegalEntity(scenarioUniqueId)
                .withParticipantData(givenConfidence.sharedByOwner)
                .withConfidence(givenConfidence)
                .withRunUniqueIdentifiers(runUniqueId)
                .let { poolResponseFactory.buildLegalEntityWithLegalAddress(it) }
        ) {
            copy(
                header = header.copy(bpnl = "BPNL$runUniqueId"),
                legalAddress = legalAddress.copy(
                    bpna = "BPNAL$runUniqueId",
                    addressType = legalAddressType
                )
            )
        }.withConfidence(TestDataV7.SyncedSharedByOwnerConfidence)
    }

    private fun LegalEntityDto.withRunUniqueIdentifiers(runUniqueId: String): LegalEntityDto =
        copy(
            header = header.copy(
                identifiers = header.identifiers.mapIndexed { index, it ->
                    it.copy(value = "${it.type} Value $runUniqueId $index")
                }
            ),
            legalAddress = legalAddress.copy(
                identifiers = legalAddress.identifiers.mapIndexed { index, it ->
                    it.copy(value = "${it.type} Value $runUniqueId $index")
                }
            )
        )

    private fun AddressPartnerCreateRequest.withRunUniqueIdentifiers(runUniqueId: String): AddressPartnerCreateRequest =
        copy(
            address = address.copy(
                identifiers = address.identifiers.mapIndexed { index, it ->
                    it.copy(value = "${it.type} Value $runUniqueId $index")
                }
            )
        )

    private fun BusinessPartnerInputRequest.withRunUniqueReferences(runUniqueId: String): BusinessPartnerInputRequest =
        copy(
            identifiers = identifiers.mapIndexed { index, it ->
                it.copy(value = "Identifier Value ${index + 1} $runUniqueId")
            },
            legalEntity = legalEntity.copy(legalEntityBpn = "BPNL $runUniqueId"),
            site = site.copy(siteBpn = "BPNS $runUniqueId"),
            address = address.copy(addressBpn = "BPNA $runUniqueId")
        )
}
