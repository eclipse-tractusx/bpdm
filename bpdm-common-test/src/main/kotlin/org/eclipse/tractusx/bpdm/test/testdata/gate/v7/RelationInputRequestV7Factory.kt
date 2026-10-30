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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v7

import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.request.RelationPutV7Entry
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

class RelationInputRequestV7Factory(
    private val testMetadata: GateTestMetadataV7
) {

    fun fromSeed(seed: String): RelationPutV7Entry = SeededCreator(seed).create()

    fun toExpectedResponse(entry: RelationPutV7Entry): RelationV7Dto = RelationV7Dto(
        externalId = entry.externalId,
        relationType = entry.relationType,
        businessPartnerSourceExternalId = entry.businessPartnerSourceExternalId,
        businessPartnerTargetExternalId = entry.businessPartnerTargetExternalId,
        validityPeriods = entry.validityPeriods,
        reasonCode = entry.reasonCode,
        updatedAt = Instant.now(),
        createdAt = Instant.now()
    )

    private inner class SeededCreator(private val seed: String) {

        private val random = Random(seed.hashCode())

        fun create(): RelationPutV7Entry = RelationPutV7Entry(
            externalId = seed,
            relationType = RelationTypeV7.entries.random(random),
            businessPartnerSourceExternalId = "$seed Source",
            businessPartnerTargetExternalId = "$seed Target",
            reasonCode = testMetadata.reasonCodes.random(random),
            validityPeriods = createValidityPeriods()
        )

        private fun createValidityPeriods(): List<RelationValidityPeriodV7Dto> {
            val validFrom = LocalDate.of(2000 + random.nextInt(0, 20), random.nextInt(1, 13), random.nextInt(1, 28))
            val validTo = if (random.nextBoolean()) validFrom.plusYears(random.nextLong(1, 30)) else null
            return listOf(RelationValidityPeriodV7Dto(validFrom = validFrom, validTo = validTo))
        }
    }
}

fun RelationPutV7Entry.withExternalId(externalId: String) = copy(externalId = externalId)
fun RelationPutV7Entry.withRelationType(relationType: RelationTypeV7) = copy(relationType = relationType)
fun RelationPutV7Entry.withSource(businessPartnerSourceExternalId: String) = copy(businessPartnerSourceExternalId = businessPartnerSourceExternalId)
fun RelationPutV7Entry.withTarget(businessPartnerTargetExternalId: String) = copy(businessPartnerTargetExternalId = businessPartnerTargetExternalId)
fun RelationPutV7Entry.withValidityPeriods(validityPeriods: List<RelationValidityPeriodV7Dto>) = copy(validityPeriods = validityPeriods)

fun RelationTypeV7.other(): RelationTypeV7 = RelationTypeV7.entries.first { it != this }
