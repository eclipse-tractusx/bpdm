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

package org.eclipse.tractusx.bpdm.pool.service.parser.bpn

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.springframework.stereotype.Service

/**
 * Tells apart the kinds of business partner a BPN can name, going by the prefix this Pool issues its BPNs under. It
 * answers from the BPN's shape alone, so a type it reports is no promise that the partner exists.
 */
@Service
class BpnTypeResolver(
    bpnConfigProperties: BpnConfigProperties
) {
    private val bpnlPrefix = "${bpnConfigProperties.id}${bpnConfigProperties.legalEntityChar}"
    private val bpnsPrefix = "${bpnConfigProperties.id}${bpnConfigProperties.siteChar}"
    private val bpnAPrefix = "${bpnConfigProperties.id}${bpnConfigProperties.addressChar}"

    /**
     * Returns the kind of business partner the given BPN names, or null when it carries no prefix this Pool issues.
     */
    fun resolveType(bpn: String): BusinessPartnerType? {
        return with(bpn) {
            when {
                startsWith(bpnlPrefix) -> BusinessPartnerType.LEGAL_ENTITY
                startsWith(bpnsPrefix) -> BusinessPartnerType.SITE
                startsWith(bpnAPrefix) -> BusinessPartnerType.ADDRESS
                else -> null
            }
        }
    }
}
