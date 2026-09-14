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

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb

/**
 * What one write does to one address: the script codes the address carries afterwards, and the business partners the
 * write names on it.
 *
 * A write that leaves the address content alone still belongs here, because it can add a partner the address has to
 * cover.
 */
sealed interface AddressCoverageWrite {

    val partners: List<PartnerScriptCodes>

    data class Created(override val partners: List<PartnerScriptCodes>, val scriptCodes: Collection<String>) : AddressCoverageWrite

    data class Rewritten(
        val address: LogisticAddressDb,
        override val partners: List<PartnerScriptCodes>,
        val scriptCodes: Collection<String>
    ) : AddressCoverageWrite

    data class PartnerOnly(val address: LogisticAddressDb, override val partners: List<PartnerScriptCodes>) : AddressCoverageWrite
}
