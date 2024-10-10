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

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component


@Component
class DbTestHelpers(private val entityManagerFactory: EntityManagerFactory?) {

    val em: EntityManager? by lazy {
        entityManagerFactory?.createEntityManager()
    }



    fun truncateDbTables() {
        truncateDbTablesFromSchema("bpdm")
        truncateDbTablesFromSchema("bpdmgate")
        truncateDbTablesFromSchema("bpdm-bridge-dummy")
        truncateDbTablesFromSchema("bpdm-orchestrator")
    }

    private fun truncateDbTablesFromSchema(dbSchemaName: String) {
        em?.transaction?.begin()

        em?.createNativeQuery(
            """
                    DO $$ DECLARE table_names RECORD;
                    BEGIN
                        FOR table_names IN SELECT table_name
                            FROM information_schema.tables
                            WHERE table_schema='$dbSchemaName'
                            AND table_name NOT IN ('flyway_schema_history','regions', 'legal_forms') 
                        LOOP 
                            EXECUTE format('TRUNCATE TABLE "$dbSchemaName".%I CONTINUE IDENTITY CASCADE;', table_names.table_name);
                        END LOOP;
                    END $$;
                """.trimIndent()
        )?.executeUpdate()

        em?.transaction?.commit()
    }



}