/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.entity

import jakarta.persistence.*
import org.eclipse.tractusx.bpdm.common.model.BaseEntity
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb

@Entity
@Table(name = "business_partner_script_variants")
class BusinessPartnerScriptVariantDb(
    @Column(name = COLUMN_SCRIPT_CODE, nullable = false)
    var scriptCode: String,

    @ManyToOne
    @JoinColumn(name = COLUMN_BUSINESS_PARTNER_ID, nullable = false)
    val businessPartner: BusinessPartnerDb,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_partner_script_variant_name_parts", joinColumns = [JoinColumn(name = "script_variant_id")])
    @OrderColumn(name = "name_parts_order")
    @Column(name = "name_part", nullable = false)
    val nameParts: MutableList<String> = mutableListOf(),

    @Column(name = "short_name")
    var shortName: String? = null,

    @Column(name = "legal_name")
    var legalName: String? = null,

    @Column(name = "site_name")
    var siteName: String? = null,

    @Column(name = "address_name")
    var addressName: String? = null,

    @Embedded
    var physicalAddress: PhysicalPostalAddressScriptVariantDb? = null,

    @Embedded
    var alternativeAddress: AlternativePostalAddressScriptVariantDb? = null

): BaseEntity(){
    companion object{
        const val COLUMN_SCRIPT_CODE = "script_code"
        const val COLUMN_BUSINESS_PARTNER_ID = "business_partner_id"
    }
}