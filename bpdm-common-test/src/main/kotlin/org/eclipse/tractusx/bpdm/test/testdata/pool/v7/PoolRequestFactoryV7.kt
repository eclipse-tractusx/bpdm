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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v7

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SitePartnerCreateVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.TestMetadataV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.common.BusinessPartnerCommonRequestFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random
import org.eclipse.tractusx.bpdm.pool.api.model.response.AddressPartnerCreateVerboseDto as AddressPartnerCreateVerboseDtoV7

class PoolRequestFactoryV7(
    private val testMetadata: TestMetadataV7
): BusinessPartnerCommonRequestFactory(
    testMetadata.addressIdentifierTypes.map { it.technicalKey },
    testMetadata.adminAreas.map { it.code },
    testMetadata.scriptCodes.map { it.technicalKey }
) {

    fun buildLegalEntityCreateRequest(seed: String, isParticipantData: Boolean = false): LegalEntityPartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerCreateRequest(
            legalEntity = buildLegalEntity(seed, random, isParticipantData),
            index = seed
        )
    }

    fun buildLegalEntityUpdateRequest(seed: String, bpnl: String): LegalEntityPartnerUpdateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerUpdateRequest(
            bpnl = bpnl,
            legalEntity = buildLegalEntity(seed, random)
        )
    }

    fun buildLegalEntity(seed: String, random: Random = createRandom(seed), isParticipantData: Boolean = false): LegalEntityDto {
        return LegalEntityDto(
            header = buildLegalEntityHeaderDto(seed, random, isParticipantData),
            legalAddress = createAddressDto("Legal Address $seed", random),
            scriptVariants = listOf(buildLegalEntityScriptVariantDto(seed, random)),
        )
    }

    fun buildLegalEntityHeaderDto(seed: String, random: Random = createRandom(seed), isParticipantData: Boolean = false): LegalEntityHeaderDto{
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityHeaderDto(
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = testMetadata.legalForms.random(random).technicalKey,
            identifiers = listOf(buildLegalEntityIdentifier(seed, 0, random), buildLegalEntityIdentifier(seed, 1, random)),
            states = listOf(
                LegalEntityStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                LegalEntityStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = false,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 0,
                lastConfidenceCheckAt = timeStamp,
                nextConfidenceCheckAt = timeStamp.plusDays(7),
                confidenceLevel = 0
            ),
            isParticipantData = isParticipantData
        )
    }

    fun buildLegalEntityIdentifier(seed: String, index: Int = 0, random: Random = Random("$seed $index".hashCode().toLong())): LegalEntityIdentifierDto{
        val idKey = testMetadata.legalEntityIdentifierTypes.random(random).technicalKey
        return LegalEntityIdentifierDto("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed")
    }

    fun buildLegalEntityScriptVariantDto(seed: String, random: Random = createRandom(seed)): LegalEntityScriptVariantDto {
        val scriptCode = availableScriptCodes.random(random)

        return LegalEntityScriptVariantDto(
            scriptCode = scriptCode,
            legalName = buildScriptVariantStringValue("Legal Name", seed, scriptCode),
            shortName = buildScriptVariantStringValue("Legal Short Name", seed, scriptCode),
            legalAddress = buildPostalAddressScriptVariant(scriptCode, seed)
        )
    }

    fun buildSiteCreateRequest(seed: String, legalEntityParent: LegalEntityWithLegalAddressVerboseDto): SitePartnerCreateRequest {
        return with(buildSiteCreateRequest(seed, legalEntityParent.header.bpnl)){ copy(site = site.withMainAddressIsShared()) }
    }

    fun buildLegalAddressSiteCreateRequest(seed: String, legalEntityParent: LegalEntityWithLegalAddressVerboseDto): SiteCreateRequestWithLegalAddressAsMain {
        return buildLegalAddressSiteCreateRequest(seed, legalEntityParent.header.bpnl)
    }

    fun createSiteUpdateRequest(seed: String, siteToUpdate: SitePartnerCreateVerboseDto): SitePartnerUpdateRequest {
        return with(createSiteUpdateRequest(seed, siteToUpdate.site.bpns)){ copy(site = site.withMainAddressIsShared()) }
    }

    fun buildAdditionalAddressCreateRequest(seed: String, legalEntity: LegalEntityWithLegalAddressVerboseDto): AddressPartnerCreateRequest =
        buildAdditionalAddressCreateRequest(seed, legalEntity.header.bpnl)

    fun buildAdditionalAddressCreateRequest(seed: String, site: SitePartnerCreateVerboseDto): AddressPartnerCreateRequest =
        buildAdditionalAddressCreateRequest(seed, site.site.bpns)

    fun buildAddressUpdateRequest(seed: String, legalEntity: LegalEntityWithLegalAddressVerboseDto): AddressPartnerUpdateRequest =
        buildAddressUpdateRequest(seed, legalEntity.legalAddress.bpna)

    fun buildAddressUpdateRequest(seed: String, site: SitePartnerCreateVerboseDto): AddressPartnerUpdateRequest =
        buildAddressUpdateRequest(seed, site.mainAddress.bpna)

    fun buildAddressUpdateRequest(seed: String, createdAddress: AddressPartnerCreateVerboseDtoV7): AddressPartnerUpdateRequest =
        buildAddressUpdateRequest(seed, createdAddress.address.bpna)

    private fun createRandom(seed: String) =  Random(seed.hashCode().toLong())

    private fun SiteDto.withMainAddressIsShared() =
        copy(mainAddress = mainAddress.copy(confidenceCriteria = mainAddress.confidenceCriteria.withGivenConfidence(TestDataV7.IsShared)))
}