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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.common.exception.BpdmValidationErrorException
import org.eclipse.tractusx.bpdm.common.mapping.ValidationContext
import org.eclipse.tractusx.bpdm.common.mapping.ValidationError
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequestV6
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmAlreadyExists
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError.TechnicalKeyAlreadyTaken
import org.eclipse.tractusx.bpdm.pool.model.error.LegalFormCreateParseError.UnresolvableAdministrativeArea
import org.springframework.stereotype.Component

private const val ADMINISTRATIVE_AREA_NOT_FOUND = "AdministrativeAreaNotFound"

/**
 * Maps the legal form parser's sealed parse errors to the errors the v6 legal form endpoints report them with.
 */
@Component
class LegalFormParseErrorMapperV6 {

    /**
     * Returns the exception reporting a failed legal form create parse, reporting a technical key that is already taken
     * ahead of an unresolvable administrative area because a client has to free the key before the rest can be judged.
     */
    fun toCreateException(errors: List<LegalFormCreateParseError>): RuntimeException {
        val alreadyTaken = errors.filterIsInstance<TechnicalKeyAlreadyTaken>().firstOrNull()
        if (alreadyTaken != null)
            return BpdmAlreadyExists(LegalFormDb::class.simpleName!!, alreadyTaken.technicalKey)
        return BpdmValidationErrorException(errors.filterIsInstance<UnresolvableAdministrativeArea>().map { toValidationError(it) })
    }

    private fun toValidationError(error: UnresolvableAdministrativeArea): ValidationError =
        ValidationError(
            validationErrorCode = ADMINISTRATIVE_AREA_NOT_FOUND,
            errorDetails = "Administrative area '${error.regionCode}' not found in system.",
            erroneousValue = error.regionCode,
            context = ValidationContext.fromRoot(LegalFormRequestV6::class, "request", LegalFormRequestV6::administrativeAreaLevel1)
        )
}
