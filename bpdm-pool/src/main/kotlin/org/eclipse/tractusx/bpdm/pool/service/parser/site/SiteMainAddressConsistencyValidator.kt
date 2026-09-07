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

package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.error.SiteMainAddressOmitted
import org.springframework.stereotype.Service

/**
 * The rule that a stated site membership of an address must keep every site the address is the main address of. Such a
 * site is bound to the address by its own main-address relation, which no statement about membership can dissolve.
 *
 * Pure: given already resolved values it decides the rule and never looks anything up, so it is unaware of resolution
 * failures — callers apply it only to resolved inputs (see `crossValidateParseResults`).
 */
@Service
class SiteMainAddressConsistencyValidator {

    /**
     * Reports one violation per site the address is the main address of that the stated sites leave out.
     */
    fun check(address: LogisticAddressDb, statedSites: List<SiteDb>): List<SiteMainAddressOmitted> {
        val statedBpns = statedSites.map { it.bpn }.toSet()
        return address.sites
            .filter { it.mainAddress.bpn == address.bpn }
            .filterNot { statedBpns.contains(it.bpn) }
            .map { SiteMainAddressOmitted(it.bpn) }
    }
}
