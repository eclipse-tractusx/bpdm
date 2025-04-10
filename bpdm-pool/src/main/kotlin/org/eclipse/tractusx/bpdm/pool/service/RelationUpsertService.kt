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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.api.model.RelationType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RelationUpsertService(
    private val relationRepository: RelationRepository,
    private val changelogService: PartnerChangelogService
) {

    @Transactional
    fun upsertRelation(upsertRequest: UpsertRequest): UpsertResult<RelationDb>{
        val source = upsertRequest.source
        val target = upsertRequest.target
        val relationType = upsertRequest.relationType

        // Prevent self-referencing relations
        if (source == target) {
            throw BpdmValidationException("A legal entity cannot have a relation to itself (BPNL: ${source.bpn}).")
        }

        val existingRelation = relationRepository.findAll(
            RelationRepository.byRelation(
                startNode = source,
                endNode = target,
                type = relationType
            )
        ).singleOrNull()

        val upsertResult = if(existingRelation != null)
            UpsertResult(existingRelation, UpsertType.NoChange)
        else
            UpsertResult(createNewRelation(upsertRequest), UpsertType.Created)

        return upsertResult
    }

    private fun createNewRelation(upsertRequest: UpsertRequest): RelationDb{
        val source = upsertRequest.source
        val target = upsertRequest.target

        val newRelation = RelationDb(
            type = upsertRequest.relationType,
            startNode = source,
            endNode = target,
            isActive = true
        )

        relationRepository.save(newRelation)

        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(source.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))
        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))

        return newRelation
    }

    data class UpsertRequest(
        val source: LegalEntityDb,
        val target: LegalEntityDb,
        val relationType: RelationType
    )

}