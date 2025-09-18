/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v6

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityStateDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerCreateRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalEntityPartnerUpdateRequest
import org.eclipse.tractusx.bpdm.test.testdata.pool.common.BusinessPartnerCommonRequestFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

class BusinessPartnerV6RequestFactory(
    testMetadata: TestMetadataV6
): BusinessPartnerCommonRequestFactory(
    testMetadata.addressIdentifierTypes.map { it.technicalKey },
    testMetadata.adminAreas.map { it.code }
) {
    private val availableLegalForms = testMetadata.legalForms.map { it.technicalKey }
    private val availableLegalEntityIdentifiers = testMetadata.legalEntityIdentifierTypes.map { it.technicalKey }


    fun buildLegalEntityCreateRequest(seed: String): LegalEntityPartnerCreateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerCreateRequest(
            legalEntity = createLegalEntityDto(seed, random),
            legalAddress = createAddressDto("Legal Address $seed", random),
            index = seed
        )
    }

    fun createLegalEntityUpdateRequest(seed: String, bpnl: String): LegalEntityPartnerUpdateRequest {
        val longSeed = seed.hashCode().toLong()
        val random = Random(longSeed)

        return LegalEntityPartnerUpdateRequest(
            bpnl = bpnl,
            legalEntity = createLegalEntityDto(seed, random),
            legalAddress = createAddressDto("Legal Address $seed", random)
        )
    }

    fun createLegalEntityDto(seed: String, random: Random =  Random(seed.hashCode().toLong())): LegalEntityDto {
        val timeStamp = LocalDateTime.ofEpochSecond(random.nextLong(0, 365241780471), random.nextInt(0, 999999999), ZoneOffset.UTC)

        return LegalEntityDto(
            legalName = "Legal Name $seed",
            legalShortName = "Legal Short Name $seed",
            legalForm = availableLegalForms.randomOrNull(random),
            identifiers = listOf(createLegalEntityIdentifier(seed, 0, random), createLegalEntityIdentifier(seed, 1, random)),
            states = listOf(
                LegalEntityStateDto(validFrom = timeStamp, validTo = timeStamp.plusDays(10), BusinessStateType.ACTIVE),
                LegalEntityStateDto(validFrom = timeStamp.plusDays(10), validTo = null, BusinessStateType.INACTIVE),
            ),
            confidenceCriteria = ConfidenceCriteriaDto(
                sharedByOwner = true,
                checkedByExternalDataSource = false,
                numberOfSharingMembers = 1,
                lastConfidenceCheckAt = timeStamp,
                nextConfidenceCheckAt = timeStamp.plusDays(7),
                confidenceLevel = 5
            ),
            isCatenaXMemberData = random.nextBoolean()
        )
    }

    fun createLegalEntityIdentifier(seed: String, index: Int, random: Random = Random("$seed $index".hashCode().toLong())): LegalEntityIdentifierDto{
        val idKey = availableLegalEntityIdentifiers.random(random)
        return LegalEntityIdentifierDto("$idKey Value $seed $index", idKey, "$idKey Issuing Body $seed")
    }
}