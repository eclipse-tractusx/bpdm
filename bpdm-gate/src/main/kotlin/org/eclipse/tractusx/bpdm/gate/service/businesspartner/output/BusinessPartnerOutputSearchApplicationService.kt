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
package org.eclipse.tractusx.bpdm.gate.service.businesspartner.output

import org.eclipse.tractusx.bpdm.gate.model.search.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.BusinessPartnerOutputRecord
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.output.operation.BusinessPartnerOutputSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.businesspartner.parser.BusinessPartnerSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches business partner outputs for every API version.
 */
@Service
class BusinessPartnerOutputSearchApplicationService(
    private val parser: BusinessPartnerSearchParser,
    private val operationService: BusinessPartnerOutputSearchOperationService
) {

    /**
     * Returns the page of [tenantBpnl]'s business partner outputs that match [request].
     */
    @Transactional(readOnly = true)
    fun search(request: BusinessPartnerSearchRequest, tenantBpnl: String?, pageable: Pageable): Page<BusinessPartnerOutputRecord> =
        operationService.search(parser.parse(request), tenantBpnl, pageable)
}
