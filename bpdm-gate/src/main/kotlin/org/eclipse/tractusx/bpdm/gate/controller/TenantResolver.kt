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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidTenantBpnlException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmTenantResolutionException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Determines the tenant a request acts for from the authenticated caller.
 */
@Component
class TenantResolver(
    private val bpnConfigProperties: BpnConfigProperties
) {

    /**
     * Returns the BPN claimed by the caller's token, or null when the caller presents none.
     */
    fun currentUserBpn(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return (authentication as? JwtAuthenticationToken)?.tokenAttributes?.get("bpn") as String?
    }

    /**
     * Returns the caller's tenant BPNL, falling back to the configured owner BPNL, and fails when neither is present or
     * the value is not a valid BPNL.
     */
    fun resolveTenantBpnl(): BpnLString {
        val tenantBpnl = currentUserBpn() ?: bpnConfigProperties.ownerBpnL.takeIf { it.isNotBlank() } ?: throw BpdmTenantResolutionException()
        return BpnLString.map(tenantBpnl).successfulResultOrNull ?: throw BpdmInvalidTenantBpnlException(tenantBpnl)
    }
}
