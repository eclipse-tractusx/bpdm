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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.address

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class AddressGetIT: SharingMemberTest() {

    /**
     * GIVEN address
     * WHEN sharing member fetches address by BPNA
     * THEN sharing member sees address
     */
    @Test
    fun `get address by BPNA`(){
        //GIVEN
        val legalEntityResponse =  testDataClient.createLegalEntity(testName)
        val additionalSiteAddressResponse = testDataClient.createAdditionalAddressFor(legalEntityResponse, testName)

        //WHEN
        val fetchedAddress = poolClient.addresses.getAddress(additionalSiteAddressResponse.address.bpna)

        //THEN
        assertRepository.assertAddressGet(fetchedAddress, additionalSiteAddressResponse.address)
    }

    /**
     * WHEN sharing member requests address by unknown BPNA
     * THEN sharing member sees 404 not found error
     */
    @Test
    fun `try get address by unknown BPNA`(){
        //WHEN
        val unknownGet = {  poolClient.addresses.getAddress("UNKNOWN"); Unit }

        //THEN
        Assertions.assertThatExceptionOfType(WebClientResponseException.NotFound::class.java).isThrownBy(unknownGet)
    }
}