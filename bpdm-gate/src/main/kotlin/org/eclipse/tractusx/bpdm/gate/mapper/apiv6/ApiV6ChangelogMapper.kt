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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.gate.api.v6.model.ChangelogType as ChangelogTypeDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.model.output.changelog.ChangelogEntryRecord
import org.eclipse.tractusx.bpdm.gate.model.raw.changelog.ChangelogType
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

/**
 * Maps changelog records to their API v6 form.
 */
@Component
class ApiV6ChangelogMapper {

    /**
     * Returns the API response for [page], which never reports invalid entries.
     */
    fun toPageDto(page: Page<ChangelogEntryRecord>): PageChangeLogDto<ChangelogGateDto> =
        PageChangeLogDto(
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            contentSize = page.numberOfElements,
            content = page.content.map { ChangelogGateDto(externalId = it.externalId, timestamp = it.createdAt, changelogType = toDto(it.changelogType)) },
            invalidEntries = 0,
            errors = emptyList()
        )

    /**
     * Returns the API changelog type for [changelogType].
     */
    fun toDto(changelogType: ChangelogType): ChangelogTypeDto =
        when (changelogType) {
            ChangelogType.CREATE -> ChangelogTypeDto.CREATE
            ChangelogType.UPDATE -> ChangelogTypeDto.UPDATE
        }
}
