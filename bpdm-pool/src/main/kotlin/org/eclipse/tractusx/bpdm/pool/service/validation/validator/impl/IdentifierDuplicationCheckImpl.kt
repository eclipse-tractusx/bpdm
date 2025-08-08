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

package org.eclipse.tractusx.bpdm.pool.service.validation.validator.impl

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.dto.input.IdentifierIdentity
import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidationResult
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IdentifierError
import org.eclipse.tractusx.bpdm.pool.repository.AddressIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityIdentifierRepository
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.IdentifierDuplicationCheck
import org.springframework.stereotype.Service

@Service
class IdentifierDuplicationCheckImpl(
    private val addressIdentifierRepository: AddressIdentifierRepository,
    private val legalEntityIdentifierRepository: LegalEntityIdentifierRepository
): IdentifierDuplicationCheck {

    override fun validate(
        identifiers: List<IdentifierIdentity>,
        type: IdentifierBusinessPartnerType
    ): List<ValidationResult<IdentifierError.Duplicate>> {

        return when(type){
            IdentifierBusinessPartnerType.LEGAL_ENTITY -> validateLegalIdentifiers(identifiers)
            IdentifierBusinessPartnerType.ADDRESS -> validateAddressIdentifiers(identifiers)
        }
    }

    private fun validateAddressIdentifiers(identifiers: List<IdentifierIdentity>): List<ValidationResult<IdentifierError.Duplicate>> {
        val foundIdentifiers = addressIdentifierRepository.findByValueIn(identifiers.map { it.value }.toSet())
        addressIdentifierRepository.joinType(foundIdentifiers)
        addressIdentifierRepository.joinAddress(foundIdentifiers)

        val existingIdentifiers = foundIdentifiers.map { IdentifierIdentity(it.value, it.type.technicalKey, it.address.bpn) }
 
        return validateIdentifiers(identifiers, existingIdentifiers)
    }

    private fun validateLegalIdentifiers(identifiers: List<IdentifierIdentity>): List<ValidationResult<IdentifierError.Duplicate>> {
        val foundIdentifiers = legalEntityIdentifierRepository.findByValueIn(identifiers.map { it.value }.toSet())
        legalEntityIdentifierRepository.joinType(foundIdentifiers)
        legalEntityIdentifierRepository.joinLegalEntity(foundIdentifiers)

        val existingIdentifiers = foundIdentifiers.map { IdentifierIdentity(it.value, it.type.technicalKey, it.legalEntity.bpn) }

        return validateIdentifiers(identifiers, existingIdentifiers)
    }

    private fun validateIdentifiers(newIdentifiers: List<IdentifierIdentity>, existingIdentifiers: List<IdentifierIdentity>):  List<ValidationResult<IdentifierError.Duplicate>>{
        val(creationIdentifiers, updateIdentifiers) = newIdentifiers.plus(existingIdentifiers).partition { it.bpn == null }

        val groupsWithBpn =  updateIdentifiers.groupBy { it }.mapValues { entry -> entry.value.size > 1 }
        val groupsWithoutBpn = creationIdentifiers.plus(updateIdentifiers).groupBy { IdentifierIdentityWithoutBpn(it.value, it.typeKey) }.mapValues { entry -> entry.value.size > 1 }

        return newIdentifiers.mapIndexed { index, id ->
            val isDuplicate = if(id.bpn == null) groupsWithoutBpn[IdentifierIdentityWithoutBpn(id.value, id.typeKey)] else groupsWithBpn[id]
            if(isDuplicate!!)  ValidationResult.fail(IdentifierError.Duplicate(index)) else ValidationResult.success()
        }
    }

    private data class IdentifierIdentityWithoutBpn(
        val value: String,
        val typeKey: String
    )

}