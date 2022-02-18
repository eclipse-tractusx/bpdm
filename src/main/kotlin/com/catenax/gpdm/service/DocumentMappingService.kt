package com.catenax.gpdm.service

import com.catenax.gpdm.dto.elastic.AddressDoc
import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.entity.Address
import com.catenax.gpdm.entity.BusinessPartner
import org.springframework.stereotype.Service

@Service
class DocumentMappingService {

    fun toDocument(partner: BusinessPartner): BusinessPartnerDoc {
        return BusinessPartnerDoc(
            partner.bpn,
            partner.names.map { it.value },
            partner.legalForm?.name,
            partner.stati.maxWithOrNull(compareBy{it.validFrom})?.officialDenotation,
            partner.addresses.map { toDocument(it) },
            partner.classification.map { it.value }
        )
    }

    fun toDocument(address: Address): AddressDoc {
        return  AddressDoc(
         address.administrativeAreas.map { it.value },
         address.postCodes.map { it.value },
         address.localities.map { it.value },
         address.thoroughfares.map { it.value },
         address.premises.map { it.value },
         address.postalDeliveryPoints.map { it.value }
     )
    }
}