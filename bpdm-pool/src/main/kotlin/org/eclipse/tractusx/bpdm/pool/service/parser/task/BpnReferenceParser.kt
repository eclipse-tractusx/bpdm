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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceKind
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.springframework.stereotype.Service

/**
 * Decides what a stated BPN reference is: a BPN that exists, a request identifier still waiting for one, or nothing
 * stated at all.
 *
 * A request identifier names whichever record an earlier write registered it against, so a task of the same batch
 * resolves it to the record the task before it left behind. Naming no record is not a rejection: it is how a request
 * asks for one to be created, which is why this parser rejects nothing and the caller looking the record up decides
 * what a reference to a missing record means.
 */
@Service
class BpnReferenceParser(
    private val bpnRequestIdentifierRepository: BpnRequestIdentifierRepository
) {

    /**
     * Reports what the reference turned out to be, without looking for the record itself.
     */
    fun parse(reference: BpnReferenceRequest): BpnReferenceParsed {
        resolve(reference)?.let { return BpnReferenceParsed.Existing(it) }

        val requestIdentifier = reference.value
        return if (reference.type == BpnReferenceKind.RequestIdentifier && !requestIdentifier.isNullOrEmpty())
            BpnReferenceParsed.Pending(requestIdentifier)
        else
            BpnReferenceParsed.Unstated
    }

    private fun resolve(reference: BpnReferenceRequest): String? =
        when (reference.type) {
            BpnReferenceKind.RequestIdentifier -> reference.value?.let { bpnRequestIdentifierRepository.findByRequestIdentifier(it)?.bpn }
            else -> reference.value
        }
}
