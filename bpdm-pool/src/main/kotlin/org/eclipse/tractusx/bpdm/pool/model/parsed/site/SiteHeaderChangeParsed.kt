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

package org.eclipse.tractusx.bpdm.pool.model.parsed.site

import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteStateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.ConfidenceCriteriaParsed

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
/**
 * A partial update to a site's header: each field is a [FieldUpdateRaw] carrying domain values, applied by
 * `SiteUpdateService`. No default values — a full replace must address every field.
 *
 * Build a targeted update by copying from [NoOp], for example
 * `SiteHeaderChangeParsed.NoOp.copy(name = FieldUpdateRaw.Set("…"))`.
 */
data class SiteHeaderChangeParsed(
    val name: FieldUpdateRaw<String>,
    val confidenceCriteria: FieldUpdateRaw<ConfidenceCriteriaParsed>,
    val states: FieldUpdateRaw<List<SiteStateRaw>>,
    val scriptVariants: FieldUpdateRaw<List<SiteScriptVariantParsed>>
) {
    companion object {
        val NoOp = SiteHeaderChangeParsed(
            name = FieldUpdateRaw.NoOp,
            confidenceCriteria = FieldUpdateRaw.NoOp,
            states = FieldUpdateRaw.NoOp,
            scriptVariants = FieldUpdateRaw.NoOp
        )
    }
}