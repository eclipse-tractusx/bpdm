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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputDeleteRequest
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputDeleteResult
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationInputDeleteOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.relation.RelationInputDeleteParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Deletes a relation input.
 */
@Service
class RelationInputDeleteApplicationService(
    private val parser: RelationInputDeleteParser,
    private val operationService: RelationInputDeleteOperationService
) {

    /**
     * Deletes the relation [request] names for [tenantBpnl], or changes nothing and returns every violation if it
     * cannot be deleted.
     */
    @Transactional
    fun delete(request: RelationInputDeleteRequest, tenantBpnl: String): RelationInputDeleteResult =
        when (val parseResult = parser.parse(request, tenantBpnl)) {
            is ParseResult.Failure -> RelationInputDeleteResult.Rejected(parseResult.errors)
            is ParseResult.Success -> {
                operationService.delete(parseResult.parsed)
                RelationInputDeleteResult.Deleted
            }
        }
}
