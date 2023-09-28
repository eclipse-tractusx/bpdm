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
                TaskRequesterState(
                    taskId = "0",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.CleanAndSync,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                ),
                TaskRequesterState(
                    taskId = "1",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.CleanAndSync,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                )
            )
        )

    val dummyCleaningReservationResponse = CleaningReservationResponse(
        timeout = Instant.now().plusSeconds(300),
        reservedTasks = listOf(
            CleaningReservation(
                taskId = "0",
                businessPartner = BusinessPartnerFull(
                    generic = BusinessPartnerGeneric(
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
            CleaningReservation(
                taskId = "1",
                businessPartner = BusinessPartnerFull(
                    generic = BusinessPartnerGeneric(
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


    private val businessPartnerFull1 = BusinessPartnerFull(
        generic = BusinessPartnerGeneric(
            nameParts = listOf("Dummy", "Name"),
            postalAddress = PostalAddressDto(
                addressType = AddressType.LegalAddress,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "70771"
                )
            )
        ),
        legalEntity = LegalEntity(
            legalName = "Dummy Name",
            bpnLReference = BpnReference(
                referenceValue = "request-id-l-1",
                referenceType = BpnReferenceType.BpnRequestIdentifier
            ),
            legalAddress = LogisticAddress(
                bpnAReference = BpnReference(
                    referenceValue = "request-id-a-1",
                    referenceType = BpnReferenceType.BpnRequestIdentifier
                ),
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "70771"
                )
            ),
        ),
        address = LogisticAddress(
            bpnAReference = BpnReference(
                referenceValue = "request-id-a-1",
                referenceType = BpnReferenceType.BpnRequestIdentifier
            ),
            physicalPostalAddress = PhysicalPostalAddressDto(
                country = CountryCode.DE,
                postalCode = "70771"
            )
        )
    )

    private val businessPartnerFull2 = BusinessPartnerFull(
        generic = BusinessPartnerGeneric(
            nameParts = listOf("Other", "Name"),
            postalAddress = PostalAddressDto(
                addressType = AddressType.AdditionalAddress,
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "80331"
                )
            )
        ),
        legalEntity = LegalEntity(
            legalName = "Other Name",
            bpnLReference = BpnReference(
                referenceValue = "BPNL1",
                referenceType = BpnReferenceType.Bpn
            ),
            legalAddress = LogisticAddress(
                bpnAReference = BpnReference(
                    referenceValue = "BPNA1",
                    referenceType = BpnReferenceType.Bpn
                ),
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "80333"
                )
            )
        ),
        site = Site(
            name = "Other Site Name",
            bpnSReference = BpnReference(
                referenceValue = "BPNS1",
                referenceType = BpnReferenceType.Bpn
            ),
            mainAddress = LogisticAddress(
                bpnAReference = BpnReference(
                    referenceValue = "BPNA2",
                    referenceType = BpnReferenceType.Bpn
                ),
                physicalPostalAddress = PhysicalPostalAddressDto(
                    country = CountryCode.DE,
                    postalCode = "80331"
                )
            )
        ),
        address = LogisticAddress(
            bpnAReference = BpnReference(
                referenceValue = "BPNA3",
                referenceType = BpnReferenceType.Bpn
            ),
            physicalPostalAddress = PhysicalPostalAddressDto(
                country = CountryCode.DE,
                postalCode = "80331"
            )
        )
    )

    val dummyPoolSyncResponse = CleaningReservationResponse(
        timeout = Instant.now().plusSeconds(300),
        reservedTasks = listOf(
            CleaningReservation(
                taskId = "0",
                businessPartner = businessPartnerFull1
            ),
            CleaningReservation(
                taskId = "1",
                businessPartner = businessPartnerFull2
            )
        )
    )

    val dummyResponseTaskState =
        TaskStateResponse(
            listOf(
                TaskRequesterState(
                    taskId = "0",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.CleanAndSync,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                ),
                TaskRequesterState(
                    taskId = "1",
                    businessPartnerResult = null,
                    processingState = TaskProcessingStateDto(
                        cleaningStep = CleaningStep.Clean,
                        reservationState = ReservationState.Queued,
                        resultState = ResultState.Pending,
                        errors = emptyList(),
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now()
                    )
                )
            )
        )


}