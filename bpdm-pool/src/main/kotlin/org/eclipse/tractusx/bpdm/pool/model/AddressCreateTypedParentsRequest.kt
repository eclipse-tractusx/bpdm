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

/**
 * Request to create an address whose parents are given as *typed* BPNs: the parent roles are already known — the legal
 * entity is always supplied, the site optional — but the BPNs are still unresolved strings. `parse` resolves them to
 * entities (or yields `UnresolvableLegalEntity`/`UnresolvableSite`), producing an [AddressCreateParsed].
 *
 * This is the middle stage of the parent-resolution pipeline: [AddressCreateUntypedParentRequest] (single BPN, role not
 * yet known) resolves down to this; this resolves down to [AddressCreateResolvedParentsRequest] (parent entities in hand).
 */
data class AddressCreateTypedParentsRequest(
    val legalEntityBpn: String,
    val siteBpn: String?,
    val content: AddressContentRequest
)
