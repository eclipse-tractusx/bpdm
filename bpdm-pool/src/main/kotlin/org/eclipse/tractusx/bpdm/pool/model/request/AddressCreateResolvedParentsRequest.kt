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

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb

/**
 * Request to create an address whose parents are already *resolved* to entities — the legal entity always present, the
 * site optional. Nothing about the parents remains to be looked up, so this is what the lowest layer ([AddressCreateService])
 * consumes; `parse` only has to validate the address [content] before [AddressCreateParsed] is persisted. In-transaction
 * callers whose parent entity is not yet persisted use this directly rather than going through BPN resolution.
 *
 * This is the final stage of the parent-resolution pipeline: untyped BPN ([AddressCreateUntypedParentRequest]) → typed
 * BPNs ([AddressCreateTypedParentsRequest]) → resolved entities.
 */
data class AddressCreateResolvedParentsRequest(
    val legalEntity: LegalEntityDb,
    val site: SiteDb?,
    val content: AddressContentRequest
)
