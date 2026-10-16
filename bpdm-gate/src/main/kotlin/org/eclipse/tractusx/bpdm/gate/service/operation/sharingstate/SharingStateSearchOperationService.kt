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

package org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate

import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.search.SharingStateSearchParsed
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the business partner sharing states of a tenant.
 */
@Service
class SharingStateSearchOperationService(
    private val sharingStateRepository: SharingStateRepository
) {

    /**
     * Returns the page of [tenantBpnl]'s sharing states that match [criteria].
     */
    @Transactional(readOnly = true)
    fun search(criteria: SharingStateSearchParsed, tenantBpnl: String?, pageable: Pageable): Page<SharingStateDb> {
        val spec = Specification.allOf(
            SharingStateRepository.Specs.byExternalIdsIn(criteria.externalIds),
            SharingStateRepository.Specs.byTenantBpnl(tenantBpnl),
            SharingStateRepository.Specs.bySharingStateTypes(criteria.sharingStateTypes),
            SharingStateRepository.Specs.byUpdatedAfter(criteria.updatedAfter)
        )
        return sharingStateRepository.findAll(spec, pageable)
    }
}
