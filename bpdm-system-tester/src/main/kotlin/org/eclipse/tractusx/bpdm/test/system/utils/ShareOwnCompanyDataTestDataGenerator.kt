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
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.gate.v7.TestDataFactoryGateV7
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.RefinementTestDataFactory
import org.eclipse.tractusx.bpdm.test.testdata.orchestrator.copyWithBpnReferenceType
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolRequestFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.PoolResponseFactoryV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withConfidence
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.withParticipantData
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner

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

    fun buildSiteBasedLegalEntity(scenarioUniqueId: String, runUniqueId: String): SiteBasedLegalEntityResult {
        val legalEntity = buildLegalEntityResponse(scenarioUniqueId, runUniqueId, AddressType.LegalAndSiteMainAddress)

        val siteCreate = with(poolRequestFactory.buildLegalSiteCreateRequest(scenarioUniqueId, legalEntity.header.bpnl)) {
            copy(scriptVariants = scriptVariants.zip(legalEntity.scriptVariants) { siteScript, leScript ->
                SiteHeaderScriptVariantDto(leScript.scriptCode, siteScript.name)
            })
        }.let { poolResponseFactory.buildLegalSiteCreate(it, legalEntity, "BPNS$runUniqueId") }

        val taskData = refinementTestDataFactory.buildLegalEntityOnSiteBusinessPartner(
            legalEntity, siteCreate.site, "BPNL000000000001", emptyList()
        ).copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return SiteBasedLegalEntityResult(SiteBasedLegalEntity(legalEntity, siteCreate.site), taskData)
    }

    fun buildLegalEntity(scenarioUniqueId: String, runUniqueId: String): LegalEntityResult {
        val legalEntity = buildLegalEntityResponse(scenarioUniqueId, runUniqueId, AddressType.LegalAddress)

        val taskData = refinementTestDataFactory.buildLegalEntityBusinessPartner(legalEntity, "BPNL000000000001", emptyList())
            .copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return LegalEntityResult(legalEntity, taskData)
    }

    fun buildSite(scenarioUniqueId: String, runUniqueId: String, legalEntity: LegalEntityWithLegalAddressVerboseDto): SiteResult {
        val siteCreate = poolRequestFactory.buildSiteCreateRequest(scenarioUniqueId, legalEntity.header.bpnl)
            .let { poolResponseFactory.buildSiteSiteCreate(it, bpnS = "BPNS$runUniqueId", bpnA = "BPNA$runUniqueId") }
        val site = poolResponseFactory.buildSiteSearchResponse(siteCreate)
        val siteWithParent = SiteWithParent(legalEntity, site)

        val taskData = refinementTestDataFactory.buildSiteBusinessPartner(legalEntity, site, "BPNL000000000001", emptyList())
            .copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return SiteResult(siteWithParent, taskData)
    }

    fun buildInputData(scenarioUniqueId: String, runUniqueId: String): BusinessPartnerInputRequest {
        return testDataFactoryGate.businessPartner.input.request.fromSeed(scenarioUniqueId)
            .withRunUniqueReferences(runUniqueId)
    }

    fun buildAdditionalSiteAddress(
        scenarioUniqueId: String,
        runUniqueId: String,
        siteWithParent: SiteWithParent
    ): AdditionalSiteAddressResult {
        val request = poolRequestFactory.buildAdditionalAddressCreateRequest(scenarioUniqueId, siteWithParent.site.site.bpns)
            .withConfidence(TestDataV7.SharedByOwner)
            .withRunUniqueIdentifiers(runUniqueId)

        val additionalAddress = poolResponseFactory
            .buildAdditionalAddressCreate(request, siteWithParent.legalEntity, "BPNA$runUniqueId")
            .let { it.copy(address = it.address.copy(bpnSite = siteWithParent.site.site.bpns)) }
            .withConfidence(TestDataV7.SharedByOwnerConfidence)
            .let { poolResponseFactory.buildAddressSearchResponse(it) }

        val taskData = refinementTestDataFactory.buildAdditionSiteAddressBusinessPartner(
            siteWithParent.legalEntity, siteWithParent.site, additionalAddress, "BPNL000000000001", emptyList()
        ).copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return AdditionalSiteAddressResult(AdditionalSiteAddressWithParent(siteWithParent, additionalAddress), taskData)
    }

    fun buildAdditionalLegalEntityAddress(
        scenarioUniqueId: String,
        runUniqueId: String,
        legalEntity: LegalEntityWithLegalAddressVerboseDto
    ): AdditionalLegalEntityAddressResult {
        val request = poolRequestFactory.buildAdditionalAddressCreateRequest(scenarioUniqueId, legalEntity)
            .withConfidence(TestDataV7.SharedByOwner)
            .withRunUniqueIdentifiers(runUniqueId)

        val additionalAddress = poolResponseFactory
            .buildAdditionalAddressCreate(request, legalEntity, "BPNA$runUniqueId")
            .withConfidence(TestDataV7.SharedByOwnerConfidence)
            .let { poolResponseFactory.buildAddressSearchResponse(it) }

        val taskData = refinementTestDataFactory.buildAdditionLegalEntityAddressBusinessPartner(
            legalEntity, additionalAddress, "BPNL000000000001", emptyList()
        ).copyWithBpnReferenceType(BpnReferenceType.BpnRequestIdentifier)

        return AdditionalLegalEntityAddressResult(AdditionalLegalEntityAddressWithParent(legalEntity, additionalAddress), taskData)
    }

    private fun buildLegalEntityResponse(
        scenarioUniqueId: String,
        runUniqueId: String,
        legalAddressType: AddressType
    ): LegalEntityWithLegalAddressVerboseDto {
        return with(
            poolRequestFactory.buildLegalEntity(scenarioUniqueId)
                .withParticipantData(true)
                .withConfidence(TestDataV7.SharedByOwner)
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
        }.withConfidence(TestDataV7.SharedByOwnerConfidence)
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
