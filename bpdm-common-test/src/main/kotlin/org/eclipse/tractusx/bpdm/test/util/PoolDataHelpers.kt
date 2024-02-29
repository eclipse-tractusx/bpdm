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

package org.eclipse.tractusx.bpdm.test.util

import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.springframework.stereotype.Component


@Component
class PoolDataHelpers(val poolClient:PoolApiClient? ) {



    fun createPoolMetadata() {

        poolClient!!.metadata.createLegalForm(BusinessPartnerNonVerboseValues.legalForm1)
        poolClient!!.metadata.createLegalForm(BusinessPartnerNonVerboseValues.legalForm2)
        poolClient!!.metadata.createLegalForm(BusinessPartnerNonVerboseValues.legalForm3)


        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.identifierTypeDto1)
        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.identifierTypeDto2)
        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.identifierTypeDto3)
        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.identifierType4)

        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.addressIdentifierType1)
        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.addressIdentifierType2)
        poolClient!!.metadata.createIdentifierType(BusinessPartnerNonVerboseValues.addressIdentifierType3)

    }

}