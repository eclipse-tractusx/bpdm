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

package org.eclipse.tractusx.bpdm.common.mapping

class BpnValidation(
    private val errorCode: String,
    private val errorDetails: String,
    bpnTypeDescriptor: String,
    bpnPrefix: String = "BPN",
    codeLength: Int = 10,
    checksumLength: Int = 2,
    allowedAlphabet: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
): BpdmValidation<String> {

    companion object{
        val bpnLValidation = BpnValidation(
            errorCode = CommonValidationErrorCodes.BpnL.name,
            errorDetails = CommonValidationErrorCodes.BpnL.description,
            bpnTypeDescriptor =  "L",
        )
    }


    private val correctTypePrefix = bpnPrefix + bpnTypeDescriptor
    private val correctPrefixLength = correctTypePrefix.length
    private val correctLength = correctPrefixLength + codeLength + checksumLength
    private val allowedAlphabetSet = allowedAlphabet.toSet()

    override fun validate(value: String, context: ValidationContext): ValidationError? {
        val wrongLength = value.length != correctLength
        val wrongTypePrefix = !value.startsWith(correctTypePrefix)
        val wrongAlphabet = value.any { it !in allowedAlphabetSet }
        // We could also check the checksum here but won't do for now to be compatible to legacy checksums

        return if(wrongLength || wrongTypePrefix || wrongAlphabet)
            ValidationError(errorCode, errorDetails, value, context)
        else null

    }
}