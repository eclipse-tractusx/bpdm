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
package org.eclipse.tractusx.bpdm.gate.service.application

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.model.output.changelog.ChangelogEntryRecord
import org.eclipse.tractusx.bpdm.gate.model.raw.changelog.ChangelogSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.operation.changelog.ChangelogSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.changelog.ChangelogSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the changelogs of business partners and relations for every API version.
 */
@Service
class ChangelogSearchApplicationService(
    private val parser: ChangelogSearchParser,
    private val operationService: ChangelogSearchOperationService
) {

    /**
     * Returns the page of [tenantBpnl]'s changelog entries of [goldenRecordType] in [stage] that match [request].
     */
    @Transactional(readOnly = true)
    fun search(
        request: ChangelogSearchRaw,
        stage: StageType,
        goldenRecordType: GoldenRecordType,
        tenantBpnl: String?,
        pageable: Pageable
    ): Page<ChangelogEntryRecord> =
        operationService.search(parser.parse(request), stage, goldenRecordType, tenantBpnl, pageable)
            .map { ChangelogEntryRecord(externalId = it.externalId, createdAt = it.createdAt.instant, changelogType = it.changelogType) }
}
