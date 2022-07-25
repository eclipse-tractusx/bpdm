package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.AddressDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.TextDoc
import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.eclipse.tractusx.bpdm.pool.entity.BusinessPartner
import org.springframework.stereotype.Service

/**
 * Responsible for mapping entities to their OpenSearch document representations
 */
@Service
class OpenSearchDocumentMappingService {

    /**
     * Maps [partner] to [BusinessPartnerDoc] representation
     */
    fun toDocument(partner: BusinessPartner): BusinessPartnerDoc {
        val partnerStatus = partner.stati.maxWithOrNull(compareBy { it.validFrom })
        return BusinessPartnerDoc(
            partner.bpn,
            partner.names.map { TextDoc(it.value) },
            if (partner.legalForm != null) TextDoc(partner.legalForm!!.name) else null,
            if (partnerStatus != null) TextDoc(partnerStatus.officialDenotation) else null,
            partner.addresses.map { toDocument(it) } + partner.sites.flatMap { it.addresses }.map { toDocument(it) },
            partner.classification.map { TextDoc(it.value) },
            partner.sites.map { TextDoc(it.name) }
        )
    }

    /**
     * Maps [address] to [AddressDoc] representation
     */
    fun toDocument(address: Address): AddressDoc {
        return AddressDoc(
            address.administrativeAreas.map { TextDoc(it.value) },
            address.postCodes.map { TextDoc(it.value) },
            address.localities.map { TextDoc(it.value) },
            address.thoroughfares.map { TextDoc(it.value) },
            address.premises.map { TextDoc(it.value) },
            address.postalDeliveryPoints.map { TextDoc(it.value) }
        )
    }

}