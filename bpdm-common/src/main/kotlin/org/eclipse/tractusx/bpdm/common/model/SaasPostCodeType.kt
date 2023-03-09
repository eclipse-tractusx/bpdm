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

package org.eclipse.tractusx.bpdm.common.model

enum class SaasPostCodeType(private val codeName: String, private val url: String) : NamedUrlType, HasDefaultValue<SaasPostCodeType> {
    CEDEX("Courrier d’Entreprise à Distribution Exceptionnelle", ""),
    LARGE_MAIL_USER("Large mail user", ""),
    OTHER("Other type", ""),
    POST_BOX("Post Box", ""),
    REGULAR("Regular", "");

    override fun getTypeName(): String {
        return codeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): SaasPostCodeType {
        return OTHER
    }
}