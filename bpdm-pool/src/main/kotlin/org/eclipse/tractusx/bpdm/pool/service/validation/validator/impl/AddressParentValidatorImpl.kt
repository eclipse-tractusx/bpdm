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

import org.eclipse.tractusx.bpdm.pool.dto.valid.AddressParent
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidationResult
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.AddressParentError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.InvalidError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IsMissingError
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.AddressParentValidator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.BpnLValidator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.BpnSValidator
import org.eclipse.tractusx.bpdm.pool.util.letNonNull
import org.eclipse.tractusx.bpdm.pool.util.mapErrors
import org.eclipse.tractusx.bpdm.pool.util.mapNonNull
import org.springframework.stereotype.Service

@Service
class AddressParentValidatorImpl(
    private val bpnLValidator: BpnLValidator,
    private val bpnSValidator: BpnSValidator,
    private val legalEntityRepository: LegalEntityRepository
): AddressParentValidator {

    override fun validate(parentBpns: List<String?>): List<Validated<AddressParent, AddressParentError>> {

        val typeWithParentBpn = parentBpns.map { parentBpn ->
            when {
                parentBpn?.startsWith("BPNL") ?: false -> ParentBpnType.BpnL
                parentBpn?.startsWith("BPNS") ?: false -> ParentBpnType.BpnS
                else -> ParentBpnType.None
            }
        }.zip(parentBpns)

        val legalEntityResults = typeWithParentBpn
            .mapNonNull { (type, parentBpn) ->  parentBpn.takeIf { type == ParentBpnType.BpnL } }
            .letNonNull(bpnLValidator::validate)
            .letNonNull { it.mapErrors{AddressParentError.LegalEntity(it) as AddressParentError } }

        val siteResults = typeWithParentBpn
            .mapNonNull { (type, parentBpn) ->  parentBpn.takeIf { type == ParentBpnType.BpnS } }
            .letNonNull(bpnSValidator::validate)
            .letNonNull { it.mapErrors{AddressParentError.Site(it) as AddressParentError } }

        val noneResults = typeWithParentBpn
            .mapNonNull { (type, parentBpn) ->  parentBpn.takeIf { type == ParentBpnType.None } }
            .letNonNull(::validateNoneTypeBpns)

        val legalEntities = legalEntityResults.mapNotNull { it?.validatedValue }
        legalEntityRepository.joinLegalAddresses(legalEntities.toSet())
        val addressParentResults = legalEntityResults.zip(siteResults){ legalEntityResult, siteResult ->
            when{
                legalEntityResult != null -> Validated.onEmpty(legalEntityResult.errors){ AddressParent(legalEntityResult.validValue, null) }
                siteResult != null -> Validated.onEmpty(siteResult.errors){ AddressParent(siteResult.validValue.legalEntity, siteResult.validatedValue) }
                else -> null
            }
        }

        return addressParentResults.zip(noneResults){ addressParentResult, noneResult ->
            val noneResultErrors = noneResult?.errors ?: emptyList()
            if(addressParentResult == null) return@zip Validated.fail(noneResultErrors)
            Validated.onEmpty(addressParentResult.errors + noneResultErrors){ addressParentResult.validValue }
        }
    }

    private enum class ParentBpnType{
        BpnL,
        BpnS,
        None
    }

    private fun validateNoneTypeBpns(parentBpns: List<String?>): List<ValidationResult<AddressParentError>>{
        return parentBpns.map { parentBpn ->
            if(parentBpn.isNullOrBlank()) ValidationResult.fail(AddressParentError.IsMissing(IsMissingError()))
            else ValidationResult.fail(AddressParentError.Invalid(InvalidError()))
        }
    }


}