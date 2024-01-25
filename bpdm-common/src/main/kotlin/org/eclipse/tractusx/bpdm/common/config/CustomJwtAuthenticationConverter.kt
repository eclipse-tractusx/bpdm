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

package org.eclipse.tractusx.bpdm.common.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import java.util.stream.Collectors

/**
 * Maps a JWT to a collection of GrantedAuthority. Extracts roles from path resource_access.${client_id}.roles in the JWT.
 * This is the default JWT path keycloak puts client roles under. Those roles are combined with the default authorities provided by
 * JwtGrantedAuthoritiesConverter, which extracts them from the scope claim.
 *
 * Example Keycloak JWT:
 * {
 *   ...
 *   "realm_access": {
 *     "roles": [
 *       "some_realm_role",
 *     ]
 *   },
 *   "resource_access": {
 *     "my-application": {
 *       "roles": [
 *         "some_application_role"
 *       ]
 *     }
 *   },
 *   "scope": "email openid profile myscope"
 * }
 *
 */
class CustomJwtAuthenticationConverter(private val resourceId: String, private val requiredBpn: String? = null) : Converter<Jwt, AbstractAuthenticationToken> {
    private val defaultGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()

    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val authorities: Collection<GrantedAuthority> =
            defaultGrantedAuthoritiesConverter.convert(source)!!.plus(extractResourceRoles(source, resourceId)).toSet()
        return JwtAuthenticationToken(source, authorities)
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        private fun extractResourceRoles(jwt: Jwt, resourceId: String, requiredBpn: String? = null): Collection<GrantedAuthority> {
            if (requiredBpn != null && requiredBpn != jwt.claims["bpn"]) {
                return emptyList()
            }

            val resourceAccess = jwt.getClaim<Map<String, Any>>("resource_access")
            val resource: Map<String?, Any?>? = resourceAccess?.get(resourceId) as Map<String?, Any?>?
            val resourceRoles: Collection<String>? = resource?.get("roles") as Collection<String>?
            return if (resourceRoles != null) {
                resourceRoles.stream()
                    .map { role: String -> SimpleGrantedAuthority("ROLE_$role") }
                    .collect(Collectors.toSet())
            } else emptySet()
        }
    }
}
