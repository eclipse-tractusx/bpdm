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

import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

class RelationInputRequestV7Factory(
    private val testMetadata: GateTestMetadataV7
) {

    fun fromSeed(seed: String): RelationPutEntry = SeededCreator(seed).create()

    fun toExpectedResponse(entry: RelationPutEntry): RelationDto = RelationDto(
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

        fun create(): RelationPutEntry = RelationPutEntry(
            externalId = seed,
            relationType = RelationType.entries.random(random),
            businessPartnerSourceExternalId = "$seed Source",
            businessPartnerTargetExternalId = "$seed Target",
            reasonCode = testMetadata.reasonCodes.random(random),
            validityPeriods = createValidityPeriods()
        )

        private fun createValidityPeriods(): List<RelationValidityPeriodDto> {
            val validFrom = LocalDate.of(2000 + random.nextInt(0, 20), random.nextInt(1, 13), random.nextInt(1, 28))
            val validTo = if (random.nextBoolean()) validFrom.plusYears(random.nextLong(1, 30)) else null
            return listOf(RelationValidityPeriodDto(validFrom = validFrom, validTo = validTo))
        }
    }
}

fun RelationPutEntry.withExternalId(externalId: String) = copy(externalId = externalId)
fun RelationPutEntry.withRelationType(relationType: RelationType) = copy(relationType = relationType)
fun RelationPutEntry.withSource(businessPartnerSourceExternalId: String) = copy(businessPartnerSourceExternalId = businessPartnerSourceExternalId)
fun RelationPutEntry.withTarget(businessPartnerTargetExternalId: String) = copy(businessPartnerTargetExternalId = businessPartnerTargetExternalId)
fun RelationPutEntry.withValidityPeriods(validityPeriods: List<RelationValidityPeriodDto>) = copy(validityPeriods = validityPeriods)

fun RelationType.other(): RelationType = RelationType.entries.first { it != this }
