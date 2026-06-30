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
 * "A referenced BPN of this entity type did not resolve to an entity." These errors are keyed by the entity type that
 * failed to resolve, not by the role it played: the role (parent vs update target) is recovered from the operation that
 * produced the error — its error root and its mapper. So one error type is reused wherever that entity is looked up; e.g.
 * a site is a parent in address create and the target in site update, both reported as [UnresolvableSite].
 *
 * Caveat: this cannot distinguish two roles that resolve the *same* entity type *within the same* operation. No current
 * operation does that; if one ever resolved, say, two different sites in different roles, a role distinction would be
 * reintroduced there.
 */
data class UnresolvableLegalEntity(val bpn: String) : AddressCreateParseError, SiteCreateParseError, LegalEntityUpdateParseError
data class UnresolvableSite(val bpn: String) : AddressCreateParseError, SiteUpdateParseError, AddressUpdateParseError
data class UnresolvableAddress(val bpn: String) : AddressUpdateParseError, SiteCreateParseError

/**
 * A single, untyped parent BPN is neither a BPNL nor a BPNS, so its parent role can't even be determined (distinct from
 * [UnresolvableLegalEntity]/[UnresolvableSite], where the type is known but the entity is absent). Only the untyped
 * address-create stage can raise this; the typed stage already has the role decided.
 */
data class InvalidParentBpn(val bpn: String) : AddressCreateParseError
