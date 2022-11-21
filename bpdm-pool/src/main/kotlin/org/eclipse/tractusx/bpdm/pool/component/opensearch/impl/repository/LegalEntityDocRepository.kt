/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LEGAL_ENTITIES_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LegalEntityDoc
import org.eclipse.tractusx.bpdm.pool.config.OpenSearchConfigProperties
import org.eclipse.tractusx.bpdm.pool.exception.BpdmOpenSearchException
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.springframework.stereotype.Repository

@Repository
class LegalEntityDocRepository(
    private val openSearchClient: OpenSearchClient,
    private val openSearchConfigProperties: OpenSearchConfigProperties
) {
    fun saveAll(legalEntityDocs: Collection<LegalEntityDoc>) {
        if (legalEntityDocs.isEmpty()) {
            return
        }

        val builder: BulkRequest.Builder = BulkRequest.Builder()

        for (legalEntityDoc in legalEntityDocs) {
            builder.operations { op ->
                op.index { idx ->
                    idx.index(LEGAL_ENTITIES_INDEX_NAME)
                        .id(legalEntityDoc.bpn)
                        .document(legalEntityDoc)
                }
            }

            if (openSearchConfigProperties.refreshOnWrite) {
                builder.refresh(Refresh.True)
            }
        }
        val result: BulkResponse = openSearchClient.bulk(builder.build())

        if (result.errors()) {
            val message = result.items().mapNotNull { it.error() }.mapNotNull { it.reason() }
                .joinToString(separator = "\n", prefix = "Error when saving business partner docs \n")
            throw BpdmOpenSearchException(message)
        }
    }
}