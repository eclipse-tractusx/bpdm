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
 * Entity-free result of an address create/update, shared by both operations. Wrapped in `UpsertResult<AddressUpserted>`
 * so the `UpsertType` carries whether the address was created, updated, or left unchanged. Parents are exposed as BPNs
 * (never `SiteDb`/`LegalEntityDb`) so callers and controllers stay decoupled from persistence entities.
 */
data class AddressUpserted(
    val bpn: String,
    val legalEntityBpn: String,
    val siteBpn: String?,
    val address: LogisticAddress,
    val scriptVariants: List<AddressScriptVariant>
)
