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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.bpdm.gate.api.client.GateClient

/** One of the sharing members a scenario can share business partner data as. */
enum class SharingMember { FIRST, SECOND, THIRD }

/**
 * The Gate of one sharing member: the whole Gate API, and the sharing states that Gate reports.
 *
 * Each sharing member has a Gate of its own. A Gate answers with the data of the company in the token and
 * decides which of its own records count towards a golden record's sharing member count, so two members
 * sharing through one Gate would be neither separated nor counted as two.
 */
class SharingMemberGate(
    val member: SharingMember,
    client: GateClient,
    val sharingStates: SharingStateWatcher
) : GateClient by client

/** The Gates of the sharing members this run holds credentials for. */
class SharingMemberGates(gates: Collection<SharingMemberGate>) {

    private val gatesByMember = gates.associateBy { it.member }

    /** Returns the sharing member's Gate. */
    fun of(member: SharingMember): SharingMemberGate =
        gatesByMember[member] ?: error("this run has no Gate for the ${member.name.lowercase()} sharing member")

    /** Reports whether this run can act as the sharing member. */
    fun isConfigured(member: SharingMember) = gatesByMember.containsKey(member)
}
