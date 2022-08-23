/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.TextDoc
import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.eclipse.tractusx.bpdm.pool.entity.BusinessPartner
import org.springframework.stereotype.Service

/**
 * Responsible for mapping entities to their OpenSearch document representations
 */
@Service
class DocumentMappingService {

    /**
     * Maps [partner] to [BusinessPartnerDoc] representation
     */
    fun toDocument(partner: BusinessPartner): BusinessPartnerDoc {
        val partnerStatus = partner.stati.maxWithOrNull(compareBy { it.validFrom })
        return BusinessPartnerDoc(
            partner.bpn,
            partner.names.map { TextDoc(it.value) },
            if (partner.legalForm?.name != null) TextDoc(partner.legalForm!!.name!!) else null,
            if (partnerStatus?.officialDenotation != null) TextDoc(partnerStatus.officialDenotation) else null,
            partner.addresses.map { toDocument(it) } + partner.sites.flatMap { it.addresses }.map { toDocument(it) },
            partner.classification.filter { it.value != null }.map { TextDoc(it.value!!) },
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