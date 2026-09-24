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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.springframework.stereotype.Component

/**
 * Turns a written ownership or data management relation into the relation a golden record relation task reports back.
 */
@Component
class GoverningRelationResultMapper {

    /**
     * Reports the relation as it now stands.
     */
    fun toTaskResult(relation: RelationDb): BusinessPartnerRelations =
        BusinessPartnerRelations(
            relationType = toTaskRelationType(relation.type),
            businessPartnerSourceBpn = relation.startNode.bpn,
            businessPartnerTargetBpn = relation.endNode.bpn,
            validityPeriods = relation.validityPeriods.sortedBy { it.validFrom }.map { RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = relation.reasonCode?.technicalKey
        )

    private fun toTaskRelationType(type: LegalEntityRelationType): RelationType =
        when (type) {
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            LegalEntityRelationType.IsManagedBy -> RelationType.IsManagedBy
            LegalEntityRelationType.IsOwnedBy -> RelationType.IsOwnedBy
            LegalEntityRelationType.IsReplacedBy -> RelationType.IsReplacedBy
        }
}
