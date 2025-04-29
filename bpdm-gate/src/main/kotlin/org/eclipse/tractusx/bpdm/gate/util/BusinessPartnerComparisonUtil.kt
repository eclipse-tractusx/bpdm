/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.util

import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.PostalAddressDb
import org.springframework.stereotype.Component

@Component
class BusinessPartnerComparisonUtil {

    fun hasChanges(entity: BusinessPartnerDb, persistedBP: BusinessPartnerDb): Boolean {

        return entity.nameParts != persistedBP.nameParts ||
                entity.roles != persistedBP.roles ||
                entity.shortName != persistedBP.shortName ||
                entity.legalName != persistedBP.legalName ||
                entity.siteName != persistedBP.siteName ||
                entity.addressName != persistedBP.addressName ||
                entity.legalForm != persistedBP.legalForm ||
                entity.isOwnCompanyData != persistedBP.isOwnCompanyData ||
                entity.bpnL != persistedBP.bpnL ||
                entity.bpnS != persistedBP.bpnS ||
                entity.bpnA != persistedBP.bpnA ||
                entity.stage != persistedBP.stage ||
                entity.identifiers != persistedBP.identifiers ||
                entity.states != persistedBP.states ||
                entity.classifications != persistedBP.classifications ||
                postalAddressHasChanges(entity.postalAddress, persistedBP.postalAddress)
    }

    private fun postalAddressHasChanges(entityPostalAddress: PostalAddressDb, persistedPostalAddress: PostalAddressDb): Boolean {
        return (entityPostalAddress.addressType != persistedPostalAddress.addressType) ||
                (entityPostalAddress.alternativePostalAddress != persistedPostalAddress.alternativePostalAddress) ||
                (entityPostalAddress.physicalPostalAddress != persistedPostalAddress.physicalPostalAddress)
    }
}