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

import org.eclipse.tractusx.bpdm.gate.mapper.DbToOutputMapper
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationOutputSearchRaw
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationOutputSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.relation.RelationOutputSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches relation outputs.
 */
@Service
class RelationOutputSearchApplicationService(
    private val parser: RelationOutputSearchParser,
    private val operationService: RelationOutputSearchOperationService,
    private val dbToOutputMapper: DbToOutputMapper
) {

    /**
     * Returns the page of [tenantBpnl]'s relation outputs that match [request].
     */
    @Transactional(readOnly = true)
    fun search(request: RelationOutputSearchRaw, tenantBpnl: String, pageable: Pageable): Page<RelationOutputRecord> =
        operationService.search(parser.parse(request), tenantBpnl, pageable)
            .map(dbToOutputMapper::toOutputRecord)
}
