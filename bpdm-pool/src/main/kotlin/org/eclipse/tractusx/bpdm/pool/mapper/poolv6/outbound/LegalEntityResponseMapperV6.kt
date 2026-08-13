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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.common.service.toDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityIdentifierVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityRelationTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityStateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerCreateVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityWithLegalAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.RelationVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityIdentifierDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityStateDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.springframework.stereotype.Component

/**
 * Maps stored legal entities to the v6 API legal entity DTOs.
 */
@Component
class LegalEntityResponseMapperV6(
    private val addressResponseMapperV6: AddressResponseMapperV6,
    private val confidenceCriteriaResponseMapperV6: ConfidenceCriteriaResponseMapperV6,
    private val legalFormResponseMapperV6: LegalFormResponseMapperV6
) {

    /**
     * Returns the given legal entity as the v6 API reports it.
     */
    fun toLegalEntity(legalEntity: LegalEntityDb): LegalEntityVerboseDtoV6 =
        with(legalEntity) {
            LegalEntityVerboseDtoV6(
                bpnl = bpn,
                legalName = legalName.value,
                legalShortName = legalName.shortName,
                legalFormVerbose = legalForm?.let { legalFormResponseMapperV6.toLegalForm(it) },
                identifiers = identifiers.map { toIdentifier(it) },
                states = states.map { toState(it) },
                relations = startNodeRelations.plus(endNodeRelations).mapNotNull { toRelation(it) },
                currentness = currentness,
                confidenceCriteria = confidenceCriteriaResponseMapperV6.toConfidenceCriteria(confidenceCriteria),
                isCatenaXMemberData = isDataSpaceParticipant,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

    /**
     * Returns the given legal entity together with its legal address as the v6 API reports them.
     */
    fun toLegalEntityWithLegalAddress(legalEntity: LegalEntityDb): LegalEntityWithLegalAddressVerboseDtoV6 =
        LegalEntityWithLegalAddressVerboseDtoV6(
            legalEntity = toLegalEntity(legalEntity),
            legalAddress = addressResponseMapperV6.toAddress(legalEntity.legalAddress)
        )

    /**
     * Returns the given created or updated legal entity as the v6 API reports it, tagged with the key of the request
     * that wrote it.
     */
    fun toUpsertResponse(legalEntity: LegalEntityDb, entryId: String?): LegalEntityPartnerCreateVerboseDtoV6 =
        LegalEntityPartnerCreateVerboseDtoV6(
            legalEntity = toLegalEntity(legalEntity),
            legalAddress = addressResponseMapperV6.toAddress(legalEntity.legalAddress),
            index = entryId
        )

    private fun toIdentifier(identifier: LegalEntityIdentifierDb): LegalEntityIdentifierVerboseDtoV6 =
        LegalEntityIdentifierVerboseDtoV6(
            identifier.value,
            TypeKeyNameVerboseDto(identifier.type.technicalKey, identifier.type.name),
            identifier.issuingBody
        )

    private fun toState(state: LegalEntityStateDb): LegalEntityStateVerboseDtoV6 =
        LegalEntityStateVerboseDtoV6(state.validFrom, state.validTo, state.type.toDto())

    // Relation types introduced after v6 was frozen are omitted from v6 responses rather than added to its enum,
    // which would change a deprecated contract. Mapping them exhaustively keeps a later type from silently reaching here.
    private fun toRelation(relation: RelationDb): RelationVerboseDtoV6? =
        toRelationType(relation.type)?.let {
            RelationVerboseDtoV6(
                it,
                relation.startNode.bpn,
                relation.endNode.bpn
            )
        }

    private fun toRelationType(relationType: LegalEntityRelationType): LegalEntityRelationTypeV6? =
        when (relationType) {
            LegalEntityRelationType.IsAlternativeHeadquarterFor -> LegalEntityRelationTypeV6.IsAlternativeHeadquarterFor
            LegalEntityRelationType.IsManagedBy -> LegalEntityRelationTypeV6.IsManagedBy
            LegalEntityRelationType.IsOwnedBy -> LegalEntityRelationTypeV6.IsOwnedBy
            LegalEntityRelationType.IsReplacedBy -> null
        }
}
