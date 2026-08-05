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

import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageParseError
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageStillNeeded
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantNotCoveredByAddress
import org.springframework.stereotype.Service

/**
 * The rule that a business partner may only be named in a script its address is also written in, decided on the state
 * the write leaves behind.
 */
@Service
class ScriptVariantCoverageValidator {

    /**
     * Reports one violation per script code a partner in [partners] is named in that [addressScriptCodes] does not cover.
     */
    fun check(addressScriptCodes: Collection<String>, partners: List<PartnerScriptCodes>): List<ScriptVariantCoverageParseError> {
        val covered = addressScriptCodes.toSet()

        return partners.flatMap { partner ->
            partner.scriptCodes.filterNot { it in covered }.distinct().map { scriptCode ->
                if (partner.bpn == null) ScriptVariantNotCoveredByAddress(scriptCode)
                else ScriptVariantCoverageStillNeeded(scriptCode, partner.bpn)
            }
        }.distinct()
    }
}
