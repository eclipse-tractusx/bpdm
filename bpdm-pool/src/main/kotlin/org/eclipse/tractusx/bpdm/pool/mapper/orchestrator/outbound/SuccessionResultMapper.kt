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

import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.springframework.stereotype.Component

/**
 * Turns a written succession into the relation a golden record relation task reports back.
 */
@Component
class SuccessionResultMapper {

    /**
     * Reports the succession between two legal entities as it now stands.
     */
    fun toTaskResult(relation: RelationDb): BusinessPartnerRelations =
        toTaskResult(relation.startNode.bpn, relation.endNode.bpn, relation.validityPeriods, relation.reasonCode?.technicalKey)

    /**
     * Reports the succession between two sites as it now stands.
     */
    fun toTaskResult(relation: SiteRelationDb): BusinessPartnerRelations =
        toTaskResult(relation.startSite.bpn, relation.endSite.bpn, relation.validityPeriods, relation.reasonCode?.technicalKey)

    /**
     * Reports the succession between two addresses as it now stands.
     */
    fun toTaskResult(relation: AddressRelationDb): BusinessPartnerRelations =
        toTaskResult(relation.startAddress.bpn, relation.endAddress.bpn, relation.validityPeriods, relation.reasonCode?.technicalKey)

    private fun toTaskResult(
        predecessorBpn: String,
        successorBpn: String,
        validityPeriods: Collection<RelationValidityPeriodDb>,
        reasonCode: String?
    ): BusinessPartnerRelations =
        BusinessPartnerRelations(
            relationType = RelationType.IsReplacedBy,
            businessPartnerSourceBpn = predecessorBpn,
            businessPartnerTargetBpn = successorBpn,
            validityPeriods = validityPeriods.sortedBy { it.validFrom }.map { RelationValidityPeriod(it.validFrom, it.validTo) },
            reasonCode = reasonCode
        )
}
