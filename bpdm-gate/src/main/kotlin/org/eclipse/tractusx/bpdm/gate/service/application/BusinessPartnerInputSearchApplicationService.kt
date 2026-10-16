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

import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.search.BusinessPartnerSearchRequest
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputRecord
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.BusinessPartnerInputSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.BusinessPartnerSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches business partner inputs for every API version.
 */
@Service
class BusinessPartnerInputSearchApplicationService(
    private val parser: BusinessPartnerSearchParser,
    private val operationService: BusinessPartnerInputSearchOperationService,
    private val recordMapper: BusinessPartnerInputRecordMapper
) {

    /**
     * Returns the page of [tenantBpnl]'s business partner inputs that match [request].
     */
    @Transactional(readOnly = true)
    fun search(request: BusinessPartnerSearchRequest, tenantBpnl: String?, pageable: Pageable): Page<BusinessPartnerInputRecord> =
        operationService.search(parser.parse(request), tenantBpnl, pageable)
            .map(recordMapper::toRecord)
}
