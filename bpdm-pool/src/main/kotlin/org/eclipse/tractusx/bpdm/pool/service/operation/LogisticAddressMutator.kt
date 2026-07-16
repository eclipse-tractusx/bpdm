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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.*

/**
 * The permitted vocabulary of changes to an existing logistic address: an update may revise descriptive content and
 * *add* site membership, but never re-identify (`bpn`), re-parent (`legalEntity`), or rewire address relations — those
 * are absent here so a writer cannot reach them. Properties are readable so callers can carry forward values they do
 * not overwrite; the restriction is on what may be *written*.
 */
interface LogisticAddressMutator {
    var name: String?
    var physicalPostalAddress: PhysicalPostalAddressDb
    var alternativePostalAddress: AlternativePostalAddressDb?
    var confidenceCriteria: ConfidenceCriteriaDb

    fun replaceIdentifiers(identifiers: Collection<AddressIdentifierDb>)
    fun replaceStates(states: Collection<AddressStateDb>)
    fun replaceScriptVariants(scriptVariants: Collection<LogisticAddressScriptVariantDb>)

    /** Adds this address to a site's membership set. Idempotent; membership is never removed here. */
    fun assignToSite(site: SiteDb)
}
