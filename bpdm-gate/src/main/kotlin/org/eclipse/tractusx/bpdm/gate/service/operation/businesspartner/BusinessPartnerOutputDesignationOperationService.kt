/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputDesignationParsed
import org.springframework.stereotype.Service

@Service
class BusinessPartnerOutputDesignationOperationService {
    fun designate(output: BusinessPartnerDb): BusinessPartnerOutputDesignationParsed {
        val identifierType = if (output.process != null) BusinessPartnerType.GENERIC else when (output.postalAddress.addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> BusinessPartnerType.LEGAL_ENTITY
            AddressType.AdditionalAddress -> BusinessPartnerType.ADDRESS
            AddressType.SiteMainAddress, null -> null
        }
        val stateType = if (output.process != null) BusinessPartnerType.GENERIC else when (output.postalAddress.addressType) {
            AddressType.LegalAndSiteMainAddress, AddressType.LegalAddress -> BusinessPartnerType.LEGAL_ENTITY
            AddressType.AdditionalAddress -> BusinessPartnerType.ADDRESS
            AddressType.SiteMainAddress -> BusinessPartnerType.SITE
            null -> null
        }
        return BusinessPartnerOutputDesignationParsed(
            output,
            output.identifiers.filter { it.businessPartnerType == identifierType },
            output.states.filter { it.businessPartnerTyp == stateType }
        )
    }
}
