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

import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantWithoutAddressRendering
import org.springframework.stereotype.Service

/**
 * The rule that a legal entity's or site's script variant is rendered by its address in the same script. Pure: given
 * already resolved values it decides the rule and never looks anything up, so it is unaware of resolution failures —
 * callers apply it only to resolved inputs (see `crossValidateParseResults`).
 */
@Service
class ScriptVariantRenderingValidator {

    /**
     * Reports one violation per script code in [nameScriptCodes] that [addressScriptCodes] does not cover.
     */
    fun check(nameScriptCodes: Collection<String>, addressScriptCodes: Collection<String>): List<ScriptVariantWithoutAddressRendering> {
        val rendered = addressScriptCodes.toSet()
        return nameScriptCodes.filterNot { it in rendered }.distinct().map { ScriptVariantWithoutAddressRendering(it) }
    }
}
