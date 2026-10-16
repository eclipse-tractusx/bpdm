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
package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.model.changelog.ChangelogEntryRecord
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

/**
 * Maps a page of changelog entry records to the changelog response of the API.
 */
@Component
class ChangelogDtoMapper {

    /**
     * Returns the API response for [page], which never reports invalid entries.
     */
    fun toPageDto(page: Page<ChangelogEntryRecord>): PageChangeLogDto<ChangelogGateDto> =
        PageChangeLogDto(
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            contentSize = page.numberOfElements,
            content = page.content.map { ChangelogGateDto(externalId = it.externalId, timestamp = it.createdAt, changelogType = it.changelogType) },
            invalidEntries = 0,
            errors = emptyList()
        )
}
