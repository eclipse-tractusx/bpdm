/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model.parsed.task

import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceRaw

/**
 * The BPNs the request identifiers of one golden record task were mapped to before the task ran.
 */
class GoldenRecordTaskReferencesParsed(private val bpnByRequestIdentifier: Map<String, String>) {

    /**
     * Returns what [reference] names; a reference without a kind names a BPN.
     */
    fun resolve(reference: BpnReferenceRaw): BpnReferenceParsed {
        val value = reference.value
        return when {
            value.isNullOrEmpty() -> BpnReferenceParsed.Unstated
            reference.kind != BpnReferenceKind.RequestIdentifier -> BpnReferenceParsed.Existing(value)
            else -> bpnByRequestIdentifier[value]?.let { BpnReferenceParsed.Existing(it) } ?: BpnReferenceParsed.Pending(value)
        }
    }

    /**
     * Returns the BPN [reference] names, or null when it names none yet.
     */
    fun bpnOf(reference: BpnReferenceRaw): String? =
        (resolve(reference) as? BpnReferenceParsed.Existing)?.bpn
}
