package com.catenax.gpdm.component.elastic.impl.service

import com.catenax.gpdm.component.elastic.impl.doc.AddressDoc
import com.catenax.gpdm.component.elastic.impl.doc.BusinessPartnerDoc
import com.catenax.gpdm.component.elastic.impl.doc.TextDoc
import com.catenax.gpdm.entity.Address
import com.catenax.gpdm.entity.BusinessPartner
import org.springframework.stereotype.Service

@Service
class DocumentMappingService{

    fun toDocument(partner: BusinessPartner): BusinessPartnerDoc {
        val partnerStatus = partner.stati.maxWithOrNull(compareBy{it.validFrom})
        return BusinessPartnerDoc(
            partner.bpn,
            partner.names.map { TextDoc(it.value) },
            if(partner.legalForm != null) TextDoc(partner.legalForm.name) else null,
            if(partnerStatus != null) TextDoc(partnerStatus.officialDenotation) else null,
            partner.addresses.map { toDocument(it) },
            partner.classification.map { TextDoc(it.value) }
        )
    }

    fun toDocument(address: Address): AddressDoc {
        return  AddressDoc(
         address.administrativeAreas.map { TextDoc(it.value) },
         address.postCodes.map { TextDoc(it.value) },
         address.localities.map { TextDoc(it.value) },
         address.thoroughfares.map { TextDoc(it.value) },
         address.premises.map { TextDoc(it.value) },
         address.postalDeliveryPoints.map { TextDoc(it.value) }
     )
    }

}