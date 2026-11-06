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

package org.eclipse.tractusx.bpdm.pool.model.parsed.address

import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
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
 * The change half of an [AddressChangeParsed]: each field is a [FieldUpdateRaw] carrying domain values, applied by
 * `AddressUpdateService`. There are deliberately no default values — a full replace must address every field, so a
 * missing field can never silently become a no-op. [sites] is replaced like any other collection: a [FieldUpdateRaw.Set]
 * states the address's complete membership, so a site it omits is unlinked.
 *
 * Build a targeted change by copying from [NoOp], for example
 * `AddressContentChangeParsed.NoOp.copy(name = FieldUpdateRaw.Set("…"))`.
 */
data class AddressContentChangeParsed(
    val name: FieldUpdateRaw<String?>,
    val physicalPostalAddress: FieldUpdateRaw<PhysicalPostalAddressParsed>,
    val alternativePostalAddress: FieldUpdateRaw<AlternativePostalAddressParsed?>,
    val confidenceCriteria: FieldUpdateRaw<ConfidenceCriteriaParsed>,
    val identifiers: FieldUpdateRaw<List<AddressIdentifierParsed>>,
    val states: FieldUpdateRaw<List<AddressStateParsed>>,
    val scriptVariants: FieldUpdateRaw<List<AddressScriptVariantParsed>>,
    val sites: FieldUpdateRaw<List<SiteDb>>
) {
    companion object {
        val NoOp = AddressContentChangeParsed(
            name = FieldUpdateRaw.NoOp,
            physicalPostalAddress = FieldUpdateRaw.NoOp,
            alternativePostalAddress = FieldUpdateRaw.NoOp,
            confidenceCriteria = FieldUpdateRaw.NoOp,
            identifiers = FieldUpdateRaw.NoOp,
            states = FieldUpdateRaw.NoOp,
            scriptVariants = FieldUpdateRaw.NoOp,
            sites = FieldUpdateRaw.NoOp
        )
    }
}