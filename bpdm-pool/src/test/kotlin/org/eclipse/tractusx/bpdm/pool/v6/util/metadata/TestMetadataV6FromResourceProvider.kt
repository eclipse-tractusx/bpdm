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

package org.eclipse.tractusx.bpdm.pool.v6.util.metadata

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.test.testdata.pool.v6.TestMetadataV6

class TestMetadataV6FromResourceProvider(
    private val legalFormImporter: LegalFormImporterV6,
    private val adminAreaLevel1Importer: AdminAreaLevel1ImporterV6,
    private val identifierTypeImporter: IdentifierTypeImporterV6
) {

    fun createMetadata(): TestMetadataV6 {
        val allIdentifierTypes = identifierTypeImporter.importFromResource()

        return TestMetadataV6(
            legalForms =  legalFormImporter.importFromResource(),
            legalEntityIdentifierTypes = allIdentifierTypes.filter { it.businessPartnerType == IdentifierBusinessPartnerType.LEGAL_ENTITY },
            addressIdentifierTypes = allIdentifierTypes.filter { it.businessPartnerType == IdentifierBusinessPartnerType.ADDRESS },
            adminAreas = adminAreaLevel1Importer.importFromResource()
        )

    }
}