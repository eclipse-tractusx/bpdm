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

package org.eclipse.tractusx.bpdm.test.testdata.gate

import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto

object BusinessPartnerNonVerboseValues {

    val physicalAddressMinimal = BusinessPartnerVerboseValues.physicalAddressMinimal

    val bpPostalAddressInputDtoMinimal = AddressRepresentationInputDto(
        addressType = null,
        name = null,
        physicalPostalAddress = physicalAddressMinimal
    )

    val bpInputRequestMinimal = BusinessPartnerInputRequest(
        externalId = BusinessPartnerVerboseValues.externalId2,
        address = bpPostalAddressInputDtoMinimal,
        externalSequenceTimestamp = null
    )

    val bpInputRequestFull = BusinessPartnerVerboseValues.bpInputRequestFull

    val bpInputRequestChina = BusinessPartnerVerboseValues.bpInputRequestChina

    val bpInputRequestCleaned = BusinessPartnerVerboseValues.bpInputRequestCleaned

    val bpInputRequestError = BusinessPartnerVerboseValues.bpInputRequestError
}