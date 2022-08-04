package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.service.CdqMappings.toDto
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.springframework.stereotype.Service

@Service
class InputCdqMappingService {

    fun toInput(businessPartner: BusinessPartnerCdq): LegalEntityGateInput {
        return LegalEntityGateInput(
            businessPartner.externalId!!,
            businessPartner.toDto()
        )
    }


}

