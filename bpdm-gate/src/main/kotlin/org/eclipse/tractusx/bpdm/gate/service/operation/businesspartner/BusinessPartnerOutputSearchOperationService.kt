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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.IdentifierDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.StateDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputDesignationParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerSearchParsed
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches the business partner outputs of a tenant.
 */
@Service
class BusinessPartnerOutputSearchOperationService(
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    /**
     * Returns the page of [tenantBpnl]'s business partner outputs that match [criteria], each showing the identifiers
     * and states of the business partner its address type designates as its own.
     */
    @Transactional(readOnly = true)
    fun search(criteria: BusinessPartnerSearchParsed, tenantBpnl: String?, pageable: Pageable): Page<BusinessPartnerOutputDesignationParsed> {
        val spec = Specification.allOf(
            BusinessPartnerRepository.Specs.byExternalIdsIn(criteria.externalIds),
            BusinessPartnerRepository.Specs.byTenantBpnl(tenantBpnl),
            BusinessPartnerRepository.Specs.byStage(StageType.Output)
        )
        return businessPartnerRepository.findAll(spec, pageable)
            .map { BusinessPartnerOutputDesignationParsed(it, designatedIdentifiers(it), designatedStates(it)) }
    }

    private fun designatedIdentifiers(output: BusinessPartnerDb): List<IdentifierDb> =
        when (output.postalAddress.addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> output.identifiers.filter { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY }
            AddressType.AdditionalAddress -> output.identifiers.filter { it.businessPartnerType == BusinessPartnerType.ADDRESS }
            AddressType.SiteMainAddress, null -> emptyList()
        }

    private fun designatedStates(output: BusinessPartnerDb): List<StateDb> =
        when (output.postalAddress.addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> output.states.filter { it.businessPartnerTyp == BusinessPartnerType.LEGAL_ENTITY }
            AddressType.AdditionalAddress -> output.states.filter { it.businessPartnerTyp == BusinessPartnerType.ADDRESS }
            AddressType.SiteMainAddress -> output.states.filter { it.businessPartnerTyp == BusinessPartnerType.SITE }
            null -> emptyList()
        }
}
