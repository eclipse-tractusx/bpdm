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

package org.eclipse.tractusx.bpdm.common.service

import org.eclipse.tractusx.bpdm.common.dto.saas.AddressSaas
import org.eclipse.tractusx.bpdm.common.model.SassAddressType

class SaasAddressesMapping(val addresses: Collection<AddressSaas>) {

    // TODO change to technicalKey == ...
    fun saasAlternativeAddressMapping(): SaasAddressToDtoMapping? {
        val address = addresses.find { address -> address.types.any { it.name == SassAddressType.ALTERNATIVE_LEGAL.getTypeName() } }
        return address?.let { SaasAddressToDtoMapping(it) }
    }

    fun saasPhysicalAddressMapping(): SaasAddressToDtoMapping? {
        val address = addresses.find { address -> address.types.any { it.technicalKey == SassAddressType.LEGAL_ADDRESS.getTypeName() } }
        return address?.let { SaasAddressToDtoMapping(it) }
    }
}