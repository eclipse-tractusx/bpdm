/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.AddressDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.AddressPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LegalEntityDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.TextDoc
import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.eclipse.tractusx.bpdm.pool.entity.AddressPartner
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.springframework.stereotype.Service

/**
 * Responsible for mapping entities to their OpenSearch document representations
 */
@Service
class DocumentMappingService {

    /**
     * Maps [partner] to [LegalEntityDoc] representation
     */
    fun toDocument(partner: LegalEntity): LegalEntityDoc {
        val partnerStatus = partner.stati.maxWithOrNull(compareBy { it.validFrom })
        return LegalEntityDoc(
            bpn = partner.bpn,
            legalName = TextDoc(partner.legalName.value),
            legalForm = partner.legalForm?.name?.let { TextDoc(it) },
            status = partnerStatus?.description?.let { TextDoc(it) },
            addresses = listOf(toDocument(partner.legalAddress)),
            classifications = partner.classification.mapNotNull { classif -> classif.value?.let { TextDoc(it) } },
            sites = partner.sites.map { TextDoc(it.name) }
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

    /**
     * Maps [addressPartner] to [AddressPartnerDoc] representation
     */
    fun toDocument(addressPartner: AddressPartner): AddressPartnerDoc {
        return AddressPartnerDoc(
            bpn = addressPartner.bpn,
            administrativeAreas = addressPartner.address.administrativeAreas.map { it.value },
            postCodes = addressPartner.address.postCodes.map { it.value },
            localities = addressPartner.address.localities.map { it.value },
            thoroughfares = addressPartner.address.thoroughfares.map { it.value },
            premises = addressPartner.address.premises.map { it.value },
            postalDeliveryPoints = addressPartner.address.postalDeliveryPoints.map { it.value },
            countryCode = addressPartner.address.country.name
        )
    }

}