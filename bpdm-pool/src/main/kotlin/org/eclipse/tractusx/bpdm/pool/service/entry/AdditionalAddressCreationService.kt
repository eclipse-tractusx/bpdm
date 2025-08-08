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

package org.eclipse.tractusx.bpdm.pool.service.entry

import jakarta.transaction.Transactional
import org.eclipse.tractusx.bpdm.pool.dto.input.AddressCreate
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.AddressCreateContentError
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.service.operation.AddressCreationOperator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.AddressCreateValidator
import org.eclipse.tractusx.bpdm.pool.util.letValid

class AdditionalAddressCreationService(
    private val validator: AddressCreateValidator,
    private val operator: AddressCreationOperator
) {

    @Transactional
    fun create(requests: List<AddressCreate>): List<Validated<LogisticAddressDb, AddressCreateContentError>>{
        val validationResults =  validator.validate(requests)
        val createdAddresses = validationResults.letValid(operator::create)

        return validationResults.zip(createdAddresses){ validationResult, createdAddress -> Validated.onEmpty(validationResult.errors){ createdAddress!! } }
    }
}