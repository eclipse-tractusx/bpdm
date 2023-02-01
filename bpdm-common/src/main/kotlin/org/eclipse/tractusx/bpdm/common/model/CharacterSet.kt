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

enum class CharacterSet(private val typeName: String) : NamedType, HasDefaultValue<CharacterSet> {
    ARABIC("Arabic"),
    CHINESE("Simplified Chinese"),
    CHINESE_TRADITIONAL("Traditional Chinese"),
    CYRILLIC("Cyrillic"),
    GREEK("Greek"),
    HANGUL_KOREAN("Hangul"),
    HEBREW("Hebrew"),
    HIRAGANA("Hiragana"),
    KANJI("Kanji"),
    KATAKANA("Katakana"),
    LATIN("Latin"),
    THAI("Thai"),
    WESTERN_LATIN_STANDARD("Western Latin Standard (ISO 8859-1; Latin-1)"),
    UNDEFINED("Undefined");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getDefault(): CharacterSet {
        return UNDEFINED
    }
}