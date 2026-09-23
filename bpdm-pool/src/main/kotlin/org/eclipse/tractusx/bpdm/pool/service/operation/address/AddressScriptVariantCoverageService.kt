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

package org.eclipse.tractusx.bpdm.pool.service.operation.address

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressScriptVariantDb
import org.springframework.stereotype.Service

/**
 * Keeps an address written in every script a partner built on it is named in.
 *
 * A partner states only its own script variants, so a write that leaves one out is not asking for it to go: the variant
 * goes only once no partner needs it.
 */
@Service
class AddressScriptVariantCoverageService {

    /**
     * Reports the script variants the address carries after a write that states [stated]: those, plus every variant the
     * write leaves out that a partner built on the address is still named in.
     */
    fun mergeScriptVariants(address: LogisticAddressDb, stated: List<LogisticAddressScriptVariantDb>): List<LogisticAddressScriptVariantDb> {
        val statedCodes = stated.mapTo(mutableSetOf()) { it.scriptCode.technicalKey }
        val neededCodes = partnerScriptCodes(address)

        val retained = address.scriptVariants.filter { it.scriptCode.technicalKey !in statedCodes && it.scriptCode.technicalKey in neededCodes }

        return stated + retained
    }

    // Partners are matched by BPN: navigating to an address yields a lazy proxy, which is never reference-equal to the
    // address being written.
    private fun partnerScriptCodes(address: LogisticAddressDb): Set<String> {
        val legalEntity = address.legalEntity?.takeIf { it.legalAddress.bpn == address.bpn }
        val sites = address.sites.filter { it.mainAddress.bpn == address.bpn }

        return legalEntity?.scriptCodes().orEmpty().plus(sites.flatMap { it.scriptCodes() }).toSet()
    }
}
