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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.api.model.LegalEntityRelationType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.RelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UltimateOwnerResolutionService(
    private val relationRepository: RelationRepository,
    private val legalEntityRepository: LegalEntityRepository
) {
    private val logger = KotlinLogging.logger { }


    fun resolveUltimateOwner(legalEntity: LegalEntityDb): String? {
        val visited = mutableSetOf<String>()
        return resolveUltimateOwnerWithCycleProtection(legalEntity, visited)
    }


    private fun resolveUltimateOwnerWithCycleProtection(legalEntity: LegalEntityDb, visited: MutableSet<String>): String? {
        val currentBpn = legalEntity.bpn
        
        if (currentBpn in visited) {
            logger.warn { "Cycle detected in ownership chain at BPNL: $currentBpn" }
            return null
        }
        visited.add(currentBpn)

        val owningRelations = relationRepository.findByTypeAndStartNode(LegalEntityRelationType.IsOwnedBy, legalEntity)
        
        if (owningRelations.isEmpty()) {
            return if (legalEntity.ownershipUltimate) currentBpn else null
        }
        for (relation in owningRelations) {
            val parent = relation.endNode
            val parentUltimateOwner = resolveUltimateOwnerWithCycleProtection(parent, visited)
            if (parentUltimateOwner != null) {
                return parentUltimateOwner
            }
        }
        return null
    }

    @Transactional
    fun updateUltimateOwnerForEntity(legalEntity: LegalEntityDb) {
        val ultimateOwnerBpnl = resolveUltimateOwner(legalEntity)
        legalEntity.ultimateOwnerBpnl = ultimateOwnerBpnl
        legalEntityRepository.save(legalEntity)
        
        logger.debug { "Updated ultimateOwnerBpnl for ${legalEntity.bpn} to $ultimateOwnerBpnl" }
    }

    @Transactional
    fun updateUltimateOwnerForEntityAndDescendants(legalEntity: LegalEntityDb, visited: MutableSet<String> = mutableSetOf()) {
        val currentBpn = legalEntity.bpn
        
        if (currentBpn in visited) {
            logger.warn { "Cycle detected in descendant update at BPNL: $currentBpn" }
            return
        }
        visited.add(currentBpn)

        val ultimateOwnerBpnl = resolveUltimateOwner(legalEntity)
        legalEntity.ultimateOwnerBpnl = ultimateOwnerBpnl
        legalEntityRepository.save(legalEntity)
        
        logger.debug { "Updated ultimateOwnerBpnl for ${legalEntity.bpn} to $ultimateOwnerBpnl" }

        val childRelations = relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, legalEntity)
        
        for (relation in childRelations) {
            val child = relation.startNode
            updateUltimateOwnerForEntityAndDescendants(child, visited)
        }
    }


    fun findDescendants(legalEntity: LegalEntityDb): Set<LegalEntityDb> {
        val descendants = mutableSetOf<LegalEntityDb>()
        val queue = ArrayDeque<LegalEntityDb>()
        val visited = mutableSetOf<String>()

        queue.add(legalEntity)
        visited.add(legalEntity.bpn)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            
            val childRelations = relationRepository.findByTypeAndEndNode(LegalEntityRelationType.IsOwnedBy, current)
            
            for (relation in childRelations) {
                val child = relation.startNode
                if (child.bpn !in visited) {
                    visited.add(child.bpn)
                    descendants.add(child)
                    queue.add(child)
                }
            }
        }

        return descendants
    }
}
