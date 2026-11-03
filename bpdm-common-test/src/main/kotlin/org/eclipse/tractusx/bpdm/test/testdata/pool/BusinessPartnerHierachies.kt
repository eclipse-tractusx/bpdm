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

package org.eclipse.tractusx.bpdm.test.testdata.pool

import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.AddressPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityPartnerCreateV7Request
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SitePartnerCreateV7Request



data class LegalEntityHierarchy(
    val legalEntity: LegalEntityPartnerCreateV7Request,
    val siteHierarchy: List<SiteHierarchy> = emptyList(),
    val addresses: List<AddressPartnerCreateV7Request> = emptyList()
) {
    fun setParentBpnl(bpnL: String) =
        copy(
            siteHierarchy = siteHierarchy.map { it.copy(site = it.site.copy(bpnlParent = bpnL)) },
            addresses = addresses.map { it.copy(bpnParent = bpnL) }
        )

    fun with(site: SiteHierarchy) = copy(siteHierarchy = siteHierarchy.plus(site))
    fun with(address: AddressPartnerCreateV7Request) = copy(addresses = addresses.plus(address))

    fun getAllSites() = siteHierarchy.map { it.site }
    fun getAllAddresses() = siteHierarchy.flatMap { it.addresses }.plus(addresses)
}

data class SiteHierarchy(
    val site: SitePartnerCreateV7Request,
    val addresses: List<AddressPartnerCreateV7Request> = emptyList()
) {

    fun setParentBpns(bpnS: String) = copy(addresses = addresses.map { it.copy(bpnParent = bpnS) })
    fun with(address: AddressPartnerCreateV7Request) = copy(addresses = addresses.plus(address))
}
