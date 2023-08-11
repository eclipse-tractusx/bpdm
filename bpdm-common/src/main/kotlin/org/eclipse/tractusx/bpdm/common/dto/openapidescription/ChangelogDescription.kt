/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object ChangelogDescription {
    const val header = "An entry of the changelog, which is created each time a business partner is modified and " +
            "contains data about the change. The actual new state of the business partner is not included."

    const val changelogType = "One of the actions for which the changelog entry was created: create, update."
    const val timestamp = "The date and time when the changelog entry was created."
    const val businessPartnerType = "One of the types of business partners for which the changelog entry was created: legal entity, site, address."
    const val bpn = "The business partner number for which the changelog entry was created. Can be either a BPNL, BPNS or BPNA."
    const val externalId = "The external identifier of the business partner for which the changelog entry was created."
}