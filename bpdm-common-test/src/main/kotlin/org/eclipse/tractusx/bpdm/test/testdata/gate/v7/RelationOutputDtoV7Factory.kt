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

import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationValidityPeriodDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import java.time.Instant

class RelationOutputDtoV7Factory {

    fun fromGoldenRecord(externalId: String, goldenRecordRelation: BusinessPartnerRelations): RelationOutputDto{
        return RelationOutputDto(
            externalId,
            when (goldenRecordRelation.relationType) {
                RelationType.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
                RelationType.IsManagedBy -> SharableRelationType.IsManagedBy
                RelationType.IsOwnedBy -> SharableRelationType.IsOwnedBy
            },
            goldenRecordRelation.businessPartnerSourceBpnl,
            goldenRecordRelation.businessPartnerTargetBpnl,
            goldenRecordRelation.validityPeriods.map {
                RelationValidityPeriodDto(it.validFrom, it.validTo)
            },
            Instant.MIN
        )
    }
}