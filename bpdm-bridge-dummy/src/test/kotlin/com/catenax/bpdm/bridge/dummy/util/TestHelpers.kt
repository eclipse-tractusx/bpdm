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

package com.catenax.bpdm.bridge.dummy.util

import com.catenax.bpdm.bridge.dummy.testdata.CommonValues
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierType
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.springframework.stereotype.Component

@Component
class TestHelpers(
    entityManagerFactory: EntityManagerFactory,
    private val poolClient: PoolApiClient
) {

    val em: EntityManager = entityManagerFactory.createEntityManager()

    fun truncateDbTables() {
        truncateDbTablesFromSchema("bpdm")
        truncateDbTablesFromSchema("bpdmgate")
        truncateDbTablesFromSchema("bpdm-bridge-dummy")
    }

    private fun truncateDbTablesFromSchema(dbSchemaName: String) {
        em.transaction.begin()

        em.createNativeQuery(
            """
            DO $$ DECLARE table_names RECORD;
            BEGIN
                FOR table_names IN SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema='$dbSchemaName'
                    AND table_name NOT IN ('flyway_schema_history') 
                LOOP 
                    EXECUTE format('TRUNCATE TABLE "$dbSchemaName".%I CONTINUE IDENTITY CASCADE;', table_names.table_name);
                END LOOP;
            END $$;
        """.trimIndent()
        ).executeUpdate()

        em.transaction.commit()
    }

    fun createPoolMetadata() {
        val identifierType1 =
            IdentifierType(CommonValues.identifierTypeTechnicalKey1, IdentifierBusinessPartnerType.LEGAL_ENTITY, CommonValues.identifierTypeName1)
        val identifierType2 =
            IdentifierType(CommonValues.identifierTypeTechnicalKey2, IdentifierBusinessPartnerType.LEGAL_ENTITY, CommonValues.identifierTypeName2)
        val identifierType3 =
            IdentifierType(CommonValues.identifierTypeTechnicalKey3, IdentifierBusinessPartnerType.LEGAL_ENTITY, CommonValues.identifierTypeName3)
        val identifierType4 =
            IdentifierType(CommonValues.identifierTypeTechnicalKey4, IdentifierBusinessPartnerType.LEGAL_ENTITY, CommonValues.identifierTypeName4)

        poolClient.metadata.createIdentifierType(identifierType1)
        poolClient.metadata.createIdentifierType(identifierType2)
        poolClient.metadata.createIdentifierType(identifierType3)
        poolClient.metadata.createIdentifierType(identifierType4)

        val legalForm1 = LegalFormRequest(
            technicalKey = CommonValues.legalFormTechnicalKey1,
            name = CommonValues.legalFormName1,
            abbreviation = CommonValues.legalFormAbbreviation1,
        )
        val legalForm2 = LegalFormRequest(
            technicalKey = CommonValues.legalFormTechnicalKey2,
            name = CommonValues.legalFormName2,
            abbreviation = CommonValues.legalFormAbbreviation2,
        )

        poolClient.metadata.createLegalForm(legalForm1)
        poolClient.metadata.createLegalForm(legalForm2)
    }
}
