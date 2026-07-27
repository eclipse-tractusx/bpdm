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

package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.pool.model.parsed.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteScriptVariantParsed

/**
 * A partial update to a site's header: each field is a [FieldUpdate] carrying domain values, applied by
 * `SiteUpdateService`. No default values — a full replace must address every field.
 *
 * Build a targeted update by copying from [NoOp], for example
 * `SiteHeaderUpdate.NoOp.copy(name = FieldUpdate.Set("…"))`.
 */
data class SiteHeaderUpdate(
    val name: FieldUpdate<String>,
    val confidenceCriteria: FieldUpdate<ConfidenceCriteriaParsed>,
    val states: FieldUpdate<List<SiteState>>,
    val scriptVariants: FieldUpdate<List<SiteScriptVariantParsed>>
) {
    companion object {
        val NoOp = SiteHeaderUpdate(
            name = FieldUpdate.NoOp,
            confidenceCriteria = FieldUpdate.NoOp,
            states = FieldUpdate.NoOp,
            scriptVariants = FieldUpdate.NoOp
        )
    }
}
