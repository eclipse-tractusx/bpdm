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

package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.util.metadata.AdminAreaLevel1EntryImporter
import org.eclipse.tractusx.bpdm.pool.util.metadata.IdentifierTypeEntryImporter
import org.eclipse.tractusx.bpdm.pool.util.metadata.LegalFormEntryImporter
import org.eclipse.tractusx.bpdm.pool.util.metadata.TestMetadataEntryImporter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class TestDataConfiguration {

    @Bean
    fun testMetadataEntryImporter(resourceLoader: ResourceLoader): TestMetadataEntryImporter {
        return TestMetadataEntryImporter(resourceLoader)
    }

    @Bean
    fun legalFormEntryImporter(testMetadataEntryImporter: TestMetadataEntryImporter): LegalFormEntryImporter {
        return LegalFormEntryImporter(testMetadataEntryImporter, "classpath:legalforms/2023-09-28-elf-code-list-v1.5.csv")
    }

    @Bean
    fun adminAreaLevel1EntryImporter(testMetadataEntryImporter: TestMetadataEntryImporter): AdminAreaLevel1EntryImporter {
        return AdminAreaLevel1EntryImporter(testMetadataEntryImporter, "classpath:regions/IP2LOCATION-ISO3166-2.CSV")
    }

    @Bean
    fun identifierTypeEntryImporter(testMetadataEntryImporter: TestMetadataEntryImporter): IdentifierTypeEntryImporter {
        return IdentifierTypeEntryImporter(testMetadataEntryImporter, "classpath:identifiertypes/identifier-types.csv")
    }

}