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

package org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.search.BusinessPartnerSearchParsed
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the business partner inputs of a tenant.
 */
@Service
class BusinessPartnerInputSearchOperationService(
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    /**
     * Returns the page of [tenantBpnl]'s business partner inputs that match [criteria].
     */
    @Transactional(readOnly = true)
    fun search(criteria: BusinessPartnerSearchParsed, tenantBpnl: String?, pageable: Pageable): Page<BusinessPartnerDb> {
        val spec = Specification.allOf(
            BusinessPartnerRepository.Specs.byExternalIdsIn(criteria.externalIds),
            BusinessPartnerRepository.Specs.byTenantBpnl(tenantBpnl),
            BusinessPartnerRepository.Specs.byStage(StageType.Input)
        )
        return businessPartnerRepository.findAll(spec, pageable)
    }
}
