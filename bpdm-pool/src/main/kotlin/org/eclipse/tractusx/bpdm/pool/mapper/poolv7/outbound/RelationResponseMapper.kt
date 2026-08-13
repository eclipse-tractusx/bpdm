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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.RelationValidityPeriod
import org.eclipse.tractusx.bpdm.pool.api.model.RelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteRelationVerboseDto
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.springframework.stereotype.Component

/**
 * Maps stored legal entity, site and address relations to the v7 API relation DTOs.
 */
@Component
class RelationResponseMapper {

    /**
     * Returns the given legal entity relation as the API reports it.
     */
    fun toRelation(relation: RelationDb): RelationVerboseDto =
        RelationVerboseDto(
            type = relation.type,
            businessPartnerSourceBpnl = relation.startNode.bpn,
            businessPartnerTargetBpnl = relation.endNode.bpn,
            validityPeriods = toValidityPeriods(relation.validityPeriods),
            reasonCode = relation.reasonCode?.technicalKey
        )

    /**
     * Returns the given address relation as the API reports it.
     */
    fun toAddressRelation(relation: AddressRelationDb): AddressRelationVerboseDto =
        AddressRelationVerboseDto(
            type = relation.type,
            businessPartnerSourceBpna = relation.startAddress.bpn,
            businessPartnerTargetBpna = relation.endAddress.bpn,
            validityPeriods = toValidityPeriods(relation.validityPeriods),
            reasonCode = relation.reasonCode?.technicalKey
        )

    /**
     * Returns the given site relation as the API reports it.
     */
    fun toSiteRelation(relation: SiteRelationDb): SiteRelationVerboseDto =
        SiteRelationVerboseDto(
            type = relation.type,
            businessPartnerSourceBpns = relation.startSite.bpn,
            businessPartnerTargetBpns = relation.endSite.bpn,
            validityPeriods = toValidityPeriods(relation.validityPeriods),
            reasonCode = relation.reasonCode?.technicalKey
        )

    private fun toValidityPeriods(validityPeriods: Collection<RelationValidityPeriodDb>): List<RelationValidityPeriod> =
        validityPeriods.sortedBy { it.validFrom }.map { RelationValidityPeriod(validFrom = it.validFrom, validTo = it.validTo) }
}
