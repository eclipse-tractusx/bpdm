/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.operation.relation

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.RelationStageDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputSearchParsed
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the relation inputs of a tenant.
 */
@Service
class RelationInputSearchOperationService(
    private val relationStageRepository: RelationStageRepository
) {

    /**
     * Returns the page of [tenantBpnl]'s relation inputs that match [criteria].
     */
    @Transactional(readOnly = true)
    fun search(criteria: RelationInputSearchParsed, tenantBpnl: String, pageable: Pageable): Page<RelationStageDb> {
        val spec = Specification.allOf(
            RelationStageRepository.Specs.byTenantBpnL(tenantBpnl),
            RelationStageRepository.Specs.byStage(StageType.Input),
            RelationStageRepository.Specs.byExternalIds(criteria.externalIds),
            RelationStageRepository.Specs.byRelationshipType(criteria.relationType),
            RelationStageRepository.Specs.bySourceExternalIds(criteria.sourceExternalIds),
            RelationStageRepository.Specs.byTargetExternalIds(criteria.targetExternalIds),
            RelationStageRepository.Specs.byUpdatedAfter(criteria.updatedAfter)
        )
        return relationStageRepository.findAll(spec, pageable)
    }
}
