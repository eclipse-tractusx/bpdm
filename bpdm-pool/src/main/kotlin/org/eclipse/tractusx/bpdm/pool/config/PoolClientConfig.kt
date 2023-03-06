///*******************************************************************************
// * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
// *
// * See the NOTICE file(s) distributed with this work for additional
// * information regarding copyright ownership.
// *
// * This program and the accompanying materials are made available under the
// * terms of the Apache License, Version 2.0 which is available at
// * https://www.apache.org/licenses/LICENSE-2.0.
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// *
// * SPDX-License-Identifier: Apache-2.0
// ******************************************************************************/
//
//package org.eclipse.tractusx.bpdm.pool.config
//
//import org.eclipse.tractusx.bpdm.pool.client.config.PoolClientServiceConfig
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.beans.factory.annotation.Qualifier
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.core.env.Environment
//import org.springframework.http.HttpHeaders
//import org.springframework.http.MediaType
//import org.springframework.web.reactive.function.client.WebClient
//
//@Configuration
//class PoolClientConfig {
//
//
//    @Autowired
//    lateinit var environment: Environment
//
//
//
//    @Bean
//    fun poolClient(webClient: WebClient): PoolClientServiceConfig? {
//
//        val port = environment.getProperty("server.port")?.toInt() ?: 8080
//        val webClient = WebClient.create("http://localhost:$port")
//        return PoolClientServiceConfig(webClient);
//    }
//
//
//}