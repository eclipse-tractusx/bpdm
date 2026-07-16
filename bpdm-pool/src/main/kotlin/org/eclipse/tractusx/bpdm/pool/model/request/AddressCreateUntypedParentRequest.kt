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

package org.eclipse.tractusx.bpdm.pool.model.request

/**
 * Request to create an address under a single, *untyped* parent: [bpnParent] may be a BPNL or a BPNS and the role is not
 * yet known. Resolution must first determine the parent type (a legal-entity parent stands for itself; a site parent also
 * contributes its own legal entity) — reporting the precise `BpnNotValid`/`LegalEntityNotFound`/`SiteNotFound` errors —
 * before the request becomes an [AddressCreateTypedParentsRequest].
 *
 * This is the loosest stage of the parent-resolution pipeline: untyped BPN → typed BPNs
 * ([AddressCreateTypedParentsRequest]) → resolved entities ([AddressCreateResolvedParentsRequest]).
 */
data class AddressCreateUntypedParentRequest(
    val bpnParent: String,
    val content: AddressContentRequest
)
