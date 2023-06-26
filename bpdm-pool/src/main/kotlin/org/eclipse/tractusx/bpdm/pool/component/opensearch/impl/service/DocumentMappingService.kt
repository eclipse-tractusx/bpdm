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
import org.eclipse.tractusx.bpdm.pool.entity.AlternativePostalAddress
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntity
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddress
import org.eclipse.tractusx.bpdm.pool.entity.PhysicalPostalAddress
import org.springframework.stereotype.Service

/**
 * Responsible for mapping entities to their OpenSearch document representations
 */
@Service
class DocumentMappingService {

    /**
     * Maps [LegalEntity] to [LegalEntityDoc] representation
     */
    fun toDocument(partner: LegalEntity): LegalEntityDoc {
        val partnerStatus = partner.states.maxWithOrNull(compareBy { it.validFrom })
        return LegalEntityDoc(
            bpn = partner.bpn,
            legalName = TextDoc(partner.legalName.value),
            legalForm = partner.legalForm?.name?.let { TextDoc(it) },
            status = partnerStatus?.officialDenotation?.let { TextDoc(it) },
            addresses = toAddresses(partner.legalAddress),
            classifications = partner.classifications.mapNotNull { classif -> classif.value?.let { TextDoc(it) } },
            sites = partner.sites.map { TextDoc(it.name) }
        )
    }

    /**
     * Maps [LogisticAddress] to [AddressPartnerDoc] representation
     */
    fun toDocument(logisticAddress: LogisticAddress): Collection<AddressPartnerDoc> {

        val addresses: MutableList<AddressPartnerDoc> = mutableListOf()
        val list = listOfNotNull(logisticAddress.name)
        addresses.add(toAddressPartnerDoc(list, (logisticAddress.physicalPostalAddress), logisticAddress.bpn))
        // TODO OpenSearch indexing doesn't work as expected when creating two AddressPartnerDocs with the same BPN (which is the ID), only last is indexed!
        //  For now don't index alternativePostalAddress, since this would override (more important) physicalPostalAddress!
//        if (logisticAddress.alternativePostalAddress != null) {
//            addresses.add(toAddressPartnerDoc((AlternativePostalAddressToSaasMapping(logisticAddress.alternativePostalAddress!!)), logisticAddress.bpn))
//        }

        return addresses
    }

    /**
     * Maps [logisticAddress] to [AddressPartnerDoc] representation
     */
    fun toAddresses(logisticAddress: LogisticAddress): Collection<AddressDoc> {

       val addresses: MutableList<AddressDoc> = mutableListOf()

        addresses.add(toAddressDoc(logisticAddress.physicalPostalAddress))
        if (logisticAddress.alternativePostalAddress != null) {
            addresses.add(toAddressDoc((logisticAddress.alternativePostalAddress!!)))
        }

        return addresses
    }

    fun toAddressPartnerDoc(name: List<String>, address: PhysicalPostalAddress, bpn: String): AddressPartnerDoc {
        return AddressPartnerDoc(
            bpn = bpn,
            name = name
        )
    }


    fun toAddressDoc(address: PhysicalPostalAddress): AddressDoc {
        return AddressDoc(
            administrativeAreas = setOf(TextDoc(address.administrativeAreaLevel1.toString())),
            postCodes = setOf(TextDoc(address.postCode.toString())),
            localities = setOf(TextDoc("")),
            thoroughfares = setOf(TextDoc("")),
            premises = setOf(TextDoc("")),
            postalDeliveryPoints = setOf(TextDoc(""))
        )
    }

    fun toAddressDoc(address: AlternativePostalAddress): AddressDoc {
        return AddressDoc(
            administrativeAreas = setOf(TextDoc(address.administrativeAreaLevel1.toString())),
            postCodes = setOf(TextDoc(address.postCode.toString())),
            localities = setOf(TextDoc("")),
            thoroughfares = setOf(TextDoc("")),
            premises = setOf(TextDoc("")),
            postalDeliveryPoints = setOf(TextDoc(""))
        )
    }


}