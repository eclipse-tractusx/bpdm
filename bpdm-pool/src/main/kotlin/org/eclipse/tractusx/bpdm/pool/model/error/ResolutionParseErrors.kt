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

package org.eclipse.tractusx.bpdm.pool.model.error

/**
 * A referenced BPN of this entity type did not resolve. Keyed by entity type, not role (parent vs. target) — the role is
 * recovered from the producing operation, so one type is reused wherever that entity is looked up.
 *
 * Caveat: can't distinguish two roles resolving the *same* entity type within *one* operation. None do today; add a role
 * distinction there if one ever does.
 */
data class UnresolvableLegalEntity(val bpn: String) : AddressCreateParseError, SiteCreateParseError, LegalEntityUpdateParseError
data class UnresolvableSite(val bpn: String) : AddressCreateParseError, SiteUpdateParseError, AddressUpdateParseError
data class UnresolvableAddress(val bpn: String) : AddressUpdateParseError, SiteCreateParseError

data class InvalidParentBpn(val bpn: String) : AddressCreateParseError
