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

package org.eclipse.tractusx.bpdm.orchestrator.util

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.orchestrator.api.model.*
import java.time.Instant

//While we don't have a service logic implementation of the API  use a dummy response for the endpoints
object DummyValues {

    val dummyResponseCreateTask =
        TaskCreateResponse(
            listOf(
                TaskClientStateDto(
                    taskId = "0",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        resultState = ResultState.Pending,
                        step = TaskStep.CleanAndSync,
                        stepState = StepState.Queued,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                ),
                TaskClientStateDto(
                    taskId = "1",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        resultState = ResultState.Pending,
                        step = TaskStep.CleanAndSync,
                        stepState = StepState.Queued,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                )
            )
        )

    val dummyStepReservationResponse = TaskStepReservationResponse(
        timeout = Instant.now().plusSeconds(300),
        reservedTasks = listOf(
            TaskStepReservationEntryDto(
                taskId = "0",
                businessPartner = BusinessPartnerFullDto(
                    generic = BusinessPartnerGenericDto(
                        nameParts = listOf("Dummy", "Name"),
                        postalAddress = PostalAddressDto(
                            physicalPostalAddress = PhysicalPostalAddressDto(
                                country = CountryCode.DE,
                                postalCode = "70771"
                            )
                        )
                    )
                )
            ),
            TaskStepReservationEntryDto(
                taskId = "1",
                businessPartner = BusinessPartnerFullDto(
                    generic = BusinessPartnerGenericDto(
                        nameParts = listOf("Other", "Name"),
                        postalAddress = PostalAddressDto(
                            physicalPostalAddress = PhysicalPostalAddressDto(
                                country = CountryCode.DE,
                                postalCode = "80331"
                            )
                        )
                    )
                )
            )
        )
    )


    private val businessPartnerFull1 = BusinessPartnerFullDto(
        generic = BusinessPartnerGenericDto(
            nameParts = listOf("Dummy", "Name"),
            postalAddress = PostalAddressDto(
                addressType = AddressType.LegalAddress,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "70771"
                )
            )
        ),
        legalEntity = LegalEntityDto(
            legalName = "Dummy Name",
            bpnLReference = BpnReferenceDto(
                referenceValue = "request-id-l-1",
                referenceType = BpnReferenceType.BpnRequestIdentifier
            ),
            legalAddress = LogisticAddressDto(
                bpnAReference = BpnReferenceDto(
                    referenceValue = "request-id-a-1",
                    referenceType = BpnReferenceType.BpnRequestIdentifier
                ),
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "70771"
                )
            ),
        ),
        address = LogisticAddressDto(
            bpnAReference = BpnReferenceDto(
                referenceValue = "request-id-a-1",
                referenceType = BpnReferenceType.BpnRequestIdentifier
            ),
            physicalPostalAddress = PhysicalPostalAddressDto(
                country = CountryCode.DE,
                postalCode = "70771"
            )
        )
    )

    private val businessPartnerFull2 = BusinessPartnerFullDto(
        generic = BusinessPartnerGenericDto(
            nameParts = listOf("Other", "Name"),
            postalAddress = PostalAddressDto(
                addressType = AddressType.AdditionalAddress,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "80331"
                )
            )
        ),
        legalEntity = LegalEntityDto(
            legalName = "Other Name",
            bpnLReference = BpnReferenceDto(
                referenceValue = "BPNL1",
                referenceType = BpnReferenceType.Bpn
            ),
            legalAddress = LogisticAddressDto(
                bpnAReference = BpnReferenceDto(
                    referenceValue = "BPNA1",
                    referenceType = BpnReferenceType.Bpn
                ),
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "80333"
                )
            )
        ),
        site = SiteDto(
            name = "Other Site Name",
            bpnSReference = BpnReferenceDto(
                referenceValue = "BPNS1",
                referenceType = BpnReferenceType.Bpn
            ),
            mainAddress = LogisticAddressDto(
                bpnAReference = BpnReferenceDto(
                    referenceValue = "BPNA2",
                    referenceType = BpnReferenceType.Bpn
                ),
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "80331"
                )
            )
        ),
        address = LogisticAddressDto(
            bpnAReference = BpnReferenceDto(
                referenceValue = "BPNA3",
                referenceType = BpnReferenceType.Bpn
            ),
            physicalPostalAddress = PhysicalPostalAddressDto(
                country = CountryCode.DE,
                postalCode = "80331"
            )
        )
    )

    val dummyPoolSyncResponse = TaskStepReservationResponse(
        timeout = Instant.now().plusSeconds(300),
        reservedTasks = listOf(
            TaskStepReservationEntryDto(
                taskId = "0",
                businessPartner = businessPartnerFull1
            ),
            TaskStepReservationEntryDto(
                taskId = "1",
                businessPartner = businessPartnerFull2
            )
        )
    )

    val dummyResponseTaskState =
        TaskStateResponse(
            listOf(
                TaskClientStateDto(
                    taskId = "0",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        resultState = ResultState.Pending,
                        step = TaskStep.CleanAndSync,
                        stepState = StepState.Queued,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                ),
                TaskClientStateDto(
                    taskId = "1",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        resultState = ResultState.Pending,
                        step = TaskStep.Clean,
                        stepState = StepState.Queued,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                )
            )
        )


}