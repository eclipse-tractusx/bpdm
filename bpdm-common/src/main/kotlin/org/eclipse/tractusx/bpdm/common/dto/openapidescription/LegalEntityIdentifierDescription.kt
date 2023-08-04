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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object LegalEntityIdentifierDescription {
    const val header = "A legal entity identifier (uniquely) identifies the legal entity, such as the German " +
            "Handelsregisternummer, a VAT number, etc."

    const val value = "The value of the identifier like \"DE123465789\"."
    const val type = "The type of the identifier."
    const val issuingBody = "The name of the official register, where the identifier is registered. " +
            "For example, a Handelsregisternummer in Germany is only valid with its corresponding Handelsregister."
}
