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

package org.eclipse.tractusx.bpdm.common.util

class JpaMermaidCreator {

    private val MAX_ENUM_VALUES = 15

    fun getMermaid(allClassInfos: MutableCollection<JpaClassInfo>, title: String): String {

        val mermaid = StringBuilder()
        val mermaidRelationship = StringBuilder()

        appendHeader(mermaid, title)
        appendEnums(mermaid, allClassInfos)
        val whiteSpace = "        "
        allClassInfos
            .sortedBy { it.entityName }
            .forEach { classInfo ->
                mermaid.append("    class ").append(classInfo.entityName).append("{").appendLine()
                if (classInfo.embeddable) {
                    mermaid.appendLine("        <<embeddable>>")
                }
                classInfo.attributeList.forEach { attrInfo ->
                    if (attrInfo is JpaPrimitiveAttributeInfo) {
                        if (attrInfo.primitiveType is JpaStringType && attrInfo.primitiveType.typeHint == TypeHint.ENUM) {
                            mermaid.append("        ").append(attrInfo.primitiveType.domValueType?.simpleName)
                        } else {
                            mermaid.append(whiteSpace).append(attrInfo.primitiveType.getTypeName())
                        }
                    }
                    if (attrInfo is JdyObjectReferenceInfo) {
                        mermaid.append(whiteSpace).append(attrInfo.referencedClass.entityName)
                        if (attrInfo.embedded) {
                            mermaidRelationship.append("   ").append(classInfo.entityName).append(" ..> ").append(attrInfo.referencedClass.entityName)
                                .appendLine()
                        }
                    }
                    mermaid.append(" ").append(attrInfo.attrName)
                    if (attrInfo.notNull) {
                        mermaid.append(" [1]")
                    }
                    mermaid.appendLine()
                }
                mermaid.appendLine("   }")
                classInfo.associationList.forEach { assocInfo ->
                    mermaidRelationship.append("   ").append(classInfo.entityName).append(" ..> ").append(assocInfo.detailClass.entityName)
                        .append(" : ").append(assocInfo.assocName).appendLine()
                }
            }

        mermaid.appendLine()
        mermaid.append(mermaidRelationship)
        return mermaid.toString()
    }

    private fun appendEnums(mermaid: StringBuilder, allClassInfos: MutableCollection<JpaClassInfo>) {

        val allEnums: MutableSet<Class<out Any>> = mutableSetOf()

        allClassInfos.forEach { classInfo ->
            classInfo.attributeList.forEach { attrInfo ->
                if (attrInfo is JpaPrimitiveAttributeInfo
                    && attrInfo.primitiveType is JpaStringType
                    && attrInfo.primitiveType.typeHint == TypeHint.ENUM
                    && attrInfo.primitiveType.domValueType != null
                    && !allEnums.contains(attrInfo.primitiveType.domValueType)
                ) {
                    appendEnum(mermaid, attrInfo.primitiveType.domValueType, attrInfo.primitiveType.domValues)
                    allEnums.add(attrInfo.primitiveType.domValueType)
                }

            }
        }

    }

    private fun appendEnum(mermaid: StringBuilder, enumTyp: Class<out Any>?, domValues: List<DbDomainValue>?) {
        if (enumTyp != null && domValues != null) {
            mermaid.append("    class ").append(enumTyp.simpleName).append("{").appendLine()
                .appendLine("        <<enumeration>>")
            domValues
                .sortedBy { it.domValue }
                .take(MAX_ENUM_VALUES)
                .forEach {
                    mermaid.append("        ").appendLine(it.domValue)
                }
            mermaid.appendLine("    } ")
        }
    }

    private fun appendHeader(mermaid: StringBuilder, title: String) {
        mermaid.appendLine("---")
        mermaid.appendLine(title)
        mermaid.appendLine("---")
        mermaid.appendLine("classDiagram")
    }
}