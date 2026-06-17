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
 * Result of parsing an [AddressUpdateRequest]: the loose request has been validated to a bounded [LogisticAddress] and
 * the target BPN resolved to its existing entity. Update never re-parents, so no parent entities are carried. `update`
 * consumes it directly to apply changes to [target].
 */
data class AddressUpdateParsed(
    val target: LogisticAddressDb,
    val address: LogisticAddressParsed,
    val scriptVariants: List<AddressScriptVariantParsed>
)
