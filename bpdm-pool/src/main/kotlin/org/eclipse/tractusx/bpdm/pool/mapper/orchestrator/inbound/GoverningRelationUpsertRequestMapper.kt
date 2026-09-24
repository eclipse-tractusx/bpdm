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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.model.request.DataManagementUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.OwnershipUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.RelationValidityPeriodRequest
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.springframework.stereotype.Component

/**
 * Turns the ownership or data management relation a golden record relation task states into the request the Pool parses.
 */
@Component
class GoverningRelationUpsertRequestMapper {

    /**
     * Reads the relation as an ownership, in which the source is the legal entity being owned.
     */
    fun toOwnershipRequest(relations: BusinessPartnerRelations): OwnershipUpsertRequest =
        OwnershipUpsertRequest(
            ownedBpn = relations.businessPartnerSourceBpn,
            ownerBpn = relations.businessPartnerTargetBpn,
            validityPeriods = toValidityPeriodRequests(relations),
            reasonCode = relations.reasonCode
        )

    /**
     * Reads the relation as a data management relation, in which the source is the legal entity whose data is managed.
     */
    fun toDataManagementRequest(relations: BusinessPartnerRelations): DataManagementUpsertRequest =
        DataManagementUpsertRequest(
            managedBpn = relations.businessPartnerSourceBpn,
            managerBpn = relations.businessPartnerTargetBpn,
            validityPeriods = toValidityPeriodRequests(relations),
            reasonCode = relations.reasonCode
        )

    private fun toValidityPeriodRequests(relations: BusinessPartnerRelations): List<RelationValidityPeriodRequest> =
        relations.validityPeriods.map { RelationValidityPeriodRequest(it.validFrom, it.validTo) }
}
