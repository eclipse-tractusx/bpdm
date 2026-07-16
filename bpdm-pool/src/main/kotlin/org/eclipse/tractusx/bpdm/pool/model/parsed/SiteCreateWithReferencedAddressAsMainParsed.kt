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

package org.eclipse.tractusx.bpdm.pool.model.parsed

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb

/**
 * Result of parsing a [SiteCreateWithReferencedAddressAsMainRequest]: the parent BPN resolved to its entity, the
 * referenced address resolved to its existing [mainAddress] entity (the re-parent target), and the loose content
 * validated to [SiteContentParsed]. `create` consumes it directly — it re-parents [mainAddress] onto the new site and
 * overwrites its content rather than building a new address.
 */
data class SiteCreateWithReferencedAddressAsMainParsed(
    val mainAddress: LogisticAddressDb,
    val siteHeader: SiteHeaderParsed
)
