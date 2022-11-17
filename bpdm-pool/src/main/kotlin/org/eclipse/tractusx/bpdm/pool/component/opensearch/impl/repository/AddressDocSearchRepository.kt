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

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.LegalEntityDoc
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.util.BpdmOpenSearchQueryBuilder
import org.eclipse.tractusx.bpdm.pool.dto.request.AddressPartnerSearchRequest
import org.opensearch.action.search.SearchRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.SearchHits
import org.opensearch.search.builder.SearchSourceBuilder
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Creates and executes OpenSearch queries for querying [LegalEntityDoc] entries
 */
@Repository
class AddressDocSearchRepository(
    val restHighLevelClient: RestHighLevelClient,
    val bpdmQueryBuilder: BpdmOpenSearchQueryBuilder
) {

    /**
     * Find [LegalEntityDoc] entries by [partnerSearchRequest] field query texts.
     *
     * Query semantic:  For every non-null [partnerSearchRequest] field query text: the corresponding [LegalEntityDoc] field value needs to
     * either contain the whole query text exactly, or contain some words of it or has matching prefixes.
     *
     * Quality of the result is determined by the type of match: full phrase match > word match > prefix match.
     *
     * OpenSearch query structure:
     *  {
     *      "query":{
     *          "bool": {
     *              "must":[
     *                  {
     *                      "bool":{
     *                          "should": [
     *                              {"match_phrase": [searchRequest] field query text ...},
     *                              {"match": [searchRequest] field query text ...}
     *                              {"prefix": ...}
     *                              .
     *                              . for every word in [searchRequest] field query text
     *                              .
     *                              {"prefix": ...}
     *                          ]
     *                      }
     *                  }
     *                  .
     *                  . for every non-null [searchRequest] field
     *                  .
     *                  { ... }
     *              ],
     *              "filter": { "term": { "countryCode":  [searchRequest] field query text ... } }
     *          }
     *      }
     *  }
     */
    fun findBySearchRequest(partnerSearchRequest: AddressPartnerSearchRequest, pageable: Pageable): SearchHits {
        val lowerCaseSearchRequest = bpdmQueryBuilder.toLowerCaseSearchRequest(partnerSearchRequest)

        val boolQuery = QueryBuilders.boolQuery()
        val mustQuery = boolQuery.must()

        bpdmQueryBuilder.toFieldTextPairs(lowerCaseSearchRequest)
            .map { (fieldName, queryText) -> bpdmQueryBuilder.buildInnerShouldQuery(fieldName, queryText) }
            .forEach { mustQuery.add(it) }

        if (partnerSearchRequest.countryCode != null) {
            boolQuery.filter(QueryBuilders.termQuery(AddressPartnerSearchRequest::countryCode.name, partnerSearchRequest.countryCode!!.name))
        }

        val searchRequest = SearchRequest()
        val searchSourceBuilder = SearchSourceBuilder()
        searchSourceBuilder
            .query(boolQuery)
            .from(pageable.pageNumber * pageable.pageSize)
            .size(pageable.pageSize)
        searchRequest.source(searchSourceBuilder)

        val searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT)

        return searchResponse.hits
    }
}