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

package org.eclipse.tractusx.bpdm.client.service

import org.eclipse.tractusx.bpdm.client.config.WebClientConfig
import org.springframework.http.HttpMethod


class PoolClientRequestService(baseUrl: String) {

   private val poolClient = WebClientConfig(baseUrl);

    fun searchLegalEntities() {

        val url = "/getUserRanges?name=John&email=John@email.com&companyName=TestCompany"
        val response = poolClient.executeMethod(url, HttpMethod.GET, String::class.java)
        println(response)

    }



}