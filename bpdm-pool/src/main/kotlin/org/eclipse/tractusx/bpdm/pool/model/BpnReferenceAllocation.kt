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

import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.ResolvedBpnReferences
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest

/**
 * The BPNs a batch's request identifiers stand for, extended as the batch issues new ones.
 *
 * A request identifier is answered by one BPN for the whole batch, so an entry that names the same identifier as an
 * earlier entry reaches the BPN that entry issued rather than issuing a second one.
 */
class BpnReferenceAllocation(resolved: ResolvedBpnReferences) {

    private val bpnByRequestIdentifier = resolved.bpnByRequestIdentifier.toMutableMap()
    private val allocatedBpnByRequestIdentifier: MutableMap<String, String> = mutableMapOf()

    /** The BPN this reference names, or null where a request identifier has none yet. */
    fun resolve(reference: BpnReferenceRequest?): String? =
        when {
            reference == null -> null
            reference.type == BpnReferenceKind.RequestIdentifier -> bpnByRequestIdentifier[reference.value]
            else -> reference.value
        }

    /** Records that this reference's request identifier now names [bpn], unless it already names one. */
    fun allocate(reference: BpnReferenceParsed, bpn: String) {
        if (reference !is BpnReferenceParsed.Pending) return
        if (bpnByRequestIdentifier.containsKey(reference.requestIdentifier)) return

        allocatedBpnByRequestIdentifier[reference.requestIdentifier] = bpn
        bpnByRequestIdentifier[reference.requestIdentifier] = bpn
    }

    /** The allocations made since the last drain, which the caller is now responsible for persisting. */
    fun drainAllocated(): Map<String, String> {
        val allocated = allocatedBpnByRequestIdentifier.toMap()
        allocatedBpnByRequestIdentifier.clear()
        return allocated
    }
}
