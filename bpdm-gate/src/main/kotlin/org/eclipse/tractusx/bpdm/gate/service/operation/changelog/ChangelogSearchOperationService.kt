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
package org.eclipse.tractusx.bpdm.gate.service.operation.changelog

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.model.search.ChangelogSearchParsed
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the changelog entries of a tenant.
 */
@Service
class ChangelogSearchOperationService(
    private val changelogRepository: ChangelogRepository
) {

    /**
     * Returns the page of [tenantBpnl]'s changelog entries of [goldenRecordType] in [stage] that match [criteria], oldest
     * first.
     */
    @Transactional(readOnly = true)
    fun search(
        criteria: ChangelogSearchParsed,
        stage: StageType,
        goldenRecordType: GoldenRecordType,
        tenantBpnl: String?,
        pageable: Pageable
    ): Page<ChangelogEntryDb> {
        val spec = Specification.allOf(
            ChangelogRepository.Specs.byExternalIdsIn(criteria.externalIds),
            ChangelogRepository.Specs.byCreatedAtGreaterThan(criteria.timestampAfter),
            ChangelogRepository.Specs.byStage(stage),
            ChangelogRepository.Specs.byTenantBpnl(tenantBpnl),
            ChangelogRepository.Specs.byGoldenRecordType(goldenRecordType)
        )
        val oldestFirst = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by(ChangelogEntryDb::createdAt.name).ascending())
        return changelogRepository.findAll(spec, oldestFirst)
    }
}
