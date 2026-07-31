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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.springframework.stereotype.Service

/**
 * Reads the business partners whose names an address has to cover.
 */
@Service
class AddressPartnerScriptCodeReader {

    /**
     * Returns the script codes the partners built on [address] are named in today, leaving out every BPN in
     * [rewrittenBpns] whose new script codes the caller states itself.
     */
    fun storedPartners(address: LogisticAddressDb, rewrittenBpns: Set<String> = emptySet()): List<PartnerScriptCodes> {
        val legalEntity = address.legalEntity
            ?.takeIf { it.legalAddress == address && it.bpn !in rewrittenBpns }
            ?.let { PartnerScriptCodes(it.bpn, it.scriptCodes()) }
        val sites = address.sites
            .filter { it.mainAddress == address && it.bpn !in rewrittenBpns }
            .map { PartnerScriptCodes(it.bpn, it.scriptCodes()) }

        return listOfNotNull(legalEntity).plus(sites)
    }
}
