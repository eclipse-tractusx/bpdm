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

package org.eclipse.tractusx.bpdm.pool.model.update

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.AddressState
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AlternativePostalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.PhysicalPostalAddressParsed

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
 * The change half of an [AddressUpdate]: each field is a [FieldUpdate] carrying domain values, applied by
 * `AddressUpdateService`. There are deliberately no default values — a full replace must address every field, so a
 * missing field can never silently become a no-op. [sites] is replaced like any other collection: a [FieldUpdate.Set]
 * states the address's complete membership, so a site it omits is unlinked.
 *
 * Build a targeted change by copying from [NoOp], for example
 * `AddressContentUpdate.NoOp.copy(name = FieldUpdate.Set("…"))`.
 */
data class AddressContentUpdate(
    val name: FieldUpdate<String?>,
    val physicalPostalAddress: FieldUpdate<PhysicalPostalAddressParsed>,
    val alternativePostalAddress: FieldUpdate<AlternativePostalAddressParsed?>,
    val confidenceCriteria: FieldUpdate<ConfidenceCriteriaParsed>,
    val identifiers: FieldUpdate<List<AddressIdentifierParsed>>,
    val states: FieldUpdate<List<AddressState>>,
    val scriptVariants: FieldUpdate<List<AddressScriptVariantParsed>>,
    val sites: FieldUpdate<List<SiteDb>>
) {
    companion object {
        val NoOp = AddressContentUpdate(
            name = FieldUpdate.NoOp,
            physicalPostalAddress = FieldUpdate.NoOp,
            alternativePostalAddress = FieldUpdate.NoOp,
            confidenceCriteria = FieldUpdate.NoOp,
            identifiers = FieldUpdate.NoOp,
            states = FieldUpdate.NoOp,
            scriptVariants = FieldUpdate.NoOp,
            sites = FieldUpdate.NoOp
        )
    }
}