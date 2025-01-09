/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.util

import org.eclipse.tractusx.bpdm.common.mapping.types.BpnLString
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.BpdmInvalidTenantBpnlException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmTenantResolutionException
import org.springframework.stereotype.Component

@Component
class PrincipalUtil(
    private val  bpnConfigProperties: BpnConfigProperties
) {
    fun resolveTenantBpnl(): BpnLString {
        val tenantBpnlString =  getTenantBnlFromContext() ?: throw BpdmTenantResolutionException()
        return BpnLString.map(tenantBpnlString).successfulResultOrNull  ?: throw BpdmInvalidTenantBpnlException(tenantBpnlString)
    }

    fun getTenantBnlFromContext(): String? {
        return getCurrentUserBpn() ?: bpnConfigProperties.ownerBpnL.takeIf { it.isNotBlank() }
    }
}

