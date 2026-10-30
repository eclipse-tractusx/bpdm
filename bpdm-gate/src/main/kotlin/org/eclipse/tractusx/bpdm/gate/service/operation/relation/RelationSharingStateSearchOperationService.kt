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

import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationSharingStateSearchParsed
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the relation sharing states of a tenant.
 */
@Service
class RelationSharingStateSearchOperationService(
    private val relationRepository: RelationRepository
) {

    /**
     * Returns the page of [tenantBpnl]'s relations that have a sharing state matching [criteria].
     */
    @Transactional(readOnly = true)
    fun search(criteria: RelationSharingStateSearchParsed, tenantBpnl: String, pageable: Pageable): Page<RelationDb> {
        val spec = Specification.allOf(
            RelationRepository.Specs.bySharingStateNotNull(),
            RelationRepository.Specs.byTenantBpnL(tenantBpnl),
            RelationRepository.Specs.byExternalIds(criteria.externalIds),
            RelationRepository.Specs.bySharingStateTypes(criteria.sharingStateTypes),
            RelationRepository.Specs.bySharingStateUpdatedAfter(criteria.updatedAfter)
        )
        return relationRepository.findAll(spec, pageable)
    }
}
