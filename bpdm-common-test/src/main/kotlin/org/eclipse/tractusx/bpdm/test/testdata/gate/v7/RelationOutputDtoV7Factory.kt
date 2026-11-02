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

import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Entry
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.output.RelationOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.output.SharableRelationTypeV7
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import java.time.Instant

class RelationOutputDtoV7Factory {

    fun fromGoldenRecord(externalId: String, goldenRecordRelation: BusinessPartnerRelations): RelationOutputV7Dto{
        return RelationOutputV7Dto(
            externalId,
            when (goldenRecordRelation.relationType) {
                RelationType.IsAlternativeHeadquarterFor -> SharableRelationTypeV7.IsAlternativeHeadquarterFor
                RelationType.IsManagedBy -> SharableRelationTypeV7.IsManagedBy
                RelationType.IsOwnedBy -> SharableRelationTypeV7.IsOwnedBy
                RelationType.IsReplacedBy -> SharableRelationTypeV7.IsReplacedBy
            },
            goldenRecordRelation.businessPartnerSourceBpn,
            goldenRecordRelation.businessPartnerTargetBpn,
            goldenRecordRelation.validityPeriods.map {
                RelationValidityPeriodV7Dto(it.validFrom, it.validTo)
            },
            goldenRecordRelation.reasonCode,
            Instant.MIN
        )
    }

    fun fromInput(entry: RelationPutV7Entry): RelationOutputV7Dto = RelationOutputV7Dto(
        externalId = entry.externalId,
        relationType = when (entry.relationType) {
            RelationTypeV7.IsAlternativeHeadquarterFor -> SharableRelationTypeV7.IsAlternativeHeadquarterFor
            RelationTypeV7.IsManagedBy -> SharableRelationTypeV7.IsManagedBy
            RelationTypeV7.IsOwnedBy -> SharableRelationTypeV7.IsOwnedBy
            RelationTypeV7.IsReplacedBy -> SharableRelationTypeV7.IsReplacedBy
        },
        sourceBpn = "",
        targetBpn = "",
        validityPeriods = entry.validityPeriods,
        reasonCode = entry.reasonCode,
        updatedAt = Instant.MIN
    )
}