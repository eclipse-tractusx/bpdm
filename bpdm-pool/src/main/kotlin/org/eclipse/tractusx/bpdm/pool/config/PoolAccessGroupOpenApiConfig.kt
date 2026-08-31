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

import org.eclipse.tractusx.bpdm.common.util.OpenApiCustomizerFactory
import org.eclipse.tractusx.bpdm.common.util.OpenApiExampleCustomizer
import org.eclipse.tractusx.bpdm.common.util.PermissionMethodFilterFactory
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Publishes one OpenAPI documentation group per Pool user group, each holding only the endpoints that user group may call.
 */
@Configuration
class PoolAccessGroupOpenApiConfig(
    private val permissionProperties: PermissionConfigProperties
) {

    @Bean
    fun permissionMethodFilterFactory(): PermissionMethodFilterFactory =
        PermissionMethodFilterFactory(permissionProperties)

    @Bean
    fun participantAccessGroup(
        permissionMethodFilterFactory: PermissionMethodFilterFactory,
        openApiCustomizerFactory: OpenApiCustomizerFactory,
        openApiExampleCustomizer: OpenApiExampleCustomizer
    ): GroupedOpenApi =
        accessGroup(
            group = "v7-participant",
            displayName = "V7 — Dataspace Participant",
            permissions = with(permissionProperties) { setOf(readMemberPartner, readMemberChangelog, readMetaData) },
            permissionMethodFilterFactory = permissionMethodFilterFactory,
            openApiCustomizerFactory = openApiCustomizerFactory,
            openApiExampleCustomizer = openApiExampleCustomizer
        )

    @Bean
    fun sharingMemberAccessGroup(
        permissionMethodFilterFactory: PermissionMethodFilterFactory,
        openApiCustomizerFactory: OpenApiCustomizerFactory,
        openApiExampleCustomizer: OpenApiExampleCustomizer
    ): GroupedOpenApi =
        accessGroup(
            group = "v7-sharing-member",
            displayName = "V7 — Sharing Member",
            permissions = with(permissionProperties) { setOf(readPartner, readChangelog, readMetaData) },
            permissionMethodFilterFactory = permissionMethodFilterFactory,
            openApiCustomizerFactory = openApiCustomizerFactory,
            openApiExampleCustomizer = openApiExampleCustomizer
        )

    @Bean
    fun adminAccessGroup(
        permissionMethodFilterFactory: PermissionMethodFilterFactory,
        openApiCustomizerFactory: OpenApiCustomizerFactory,
        openApiExampleCustomizer: OpenApiExampleCustomizer
    ): GroupedOpenApi =
        accessGroup(
            group = "v7-admin",
            displayName = "V7 — Admin",
            permissions = with(permissionProperties) {
                setOf(readPartner, writePartner, readMemberPartner, readChangelog, readMemberChangelog, readMetaData, writeMetaData)
            },
            permissionMethodFilterFactory = permissionMethodFilterFactory,
            openApiCustomizerFactory = openApiCustomizerFactory,
            openApiExampleCustomizer = openApiExampleCustomizer
        )

    private fun accessGroup(
        group: String,
        displayName: String,
        permissions: Set<String>,
        permissionMethodFilterFactory: PermissionMethodFilterFactory,
        openApiCustomizerFactory: OpenApiCustomizerFactory,
        openApiExampleCustomizer: OpenApiExampleCustomizer
    ): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group(group)
            .pathsToMatch("/v7/**")
            .displayName(displayName)
            .addOpenApiMethodFilter(permissionMethodFilterFactory.grantedBy(permissions))
            .addOpenApiCustomizer(openApiCustomizerFactory.sortSchemaCustomiser())
            .addOpenApiCustomizer(openApiCustomizerFactory.versionApiCustomizer("v7"))
            .addOpenApiCustomizer(openApiExampleCustomizer)
            .build()
}
