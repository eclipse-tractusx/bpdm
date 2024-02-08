/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.persistence.metamodel.*
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType.*
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class JpaMetaModelReader {

    companion object {
        val DECIMAL_MIN_VALUE: BigDecimal = BigDecimal("-9999999999999.999999")
        val DECIMAL_MAX_VALUE: BigDecimal = BigDecimal("9999999999999.999999")
    }


    fun createJpaClassInfos(metaModel: Metamodel): MutableCollection<JpaClassInfo> {

        return this.createJpaClassInfos(metaModel.entities, metaModel.embeddables)
    }

    private fun createJpaClassInfos(allEntityInfos: Set<EntityType<*>?>, allEmbeddableInfos: Set<EmbeddableType<*>?>): MutableCollection<JpaClassInfo> {

        val className2InfoMap = mutableMapOf<String, JpaClassInfo>()
        val allManagedEntities: MutableList<ManagedType<*>> = mutableListOf()
        allManagedEntities.addAll(allEmbeddableInfos.filterNotNull().map { it })
        allManagedEntities.addAll(allEntityInfos.filterNotNull().map { it })

        // build base classes
        allManagedEntities.forEach { jpaEntity ->
            val entity = createJpaClassInfo(jpaEntity)
            className2InfoMap[entity.entityName] = entity
        }

        // add entity attributes
        allManagedEntities.forEach { jpsEntity ->
            className2InfoMap[JpaFieldWrapper.getEntityName(jpsEntity)]?.let { classInfo ->
                buildAttrForClassInfo(classInfo = classInfo, jpaEntity = jpsEntity, className2InfoMap)
            }
        }

        // add associations to entities
        allEntityInfos.forEach { jpaEntity ->
            jpaEntity?.let {
                className2InfoMap[it.name]?.let { classInfo ->
                    buildAssociationsForClassInfo(classInfo = classInfo, jpaEntity = jpaEntity, className2InfoMap)
                }
            }
        }

        return className2InfoMap.values
    }

    private fun createJpaClassInfo(jpaEntity: ManagedType<*>): JpaClassInfo {

        return JpaClassInfo(
            entityName = JpaFieldWrapper.getEntityName(jpaEntity),
            nameSpace = jpaEntity.javaType.name.replace('.', '_'),
            embeddable = jpaEntity is EmbeddableType
        )
    }

    private fun buildAttrForClassInfo(classInfo: JpaClassInfo, jpaEntity: ManagedType<*>, className2InfoMap: MutableMap<String, JpaClassInfo>) {


        jpaEntity.attributes.forEach { attr ->
            if (!attr.isCollection) {
                if (attr.persistentAttributeType === Attribute.PersistentAttributeType.BASIC) {
                    createPrimitiveField(attr)?.let {
                        classInfo.attributeList.add(it)
                    }
                } else if (attr.persistentAttributeType === ONE_TO_ONE
                    || attr.persistentAttributeType === MANY_TO_ONE
                    || attr.persistentAttributeType === EMBEDDED
                ) {
                    createObjectReference(attr, className2InfoMap)?.let {
                        classInfo.attributeList.add(it)
                    }
                }

            }
        }
    }


    private fun buildAssociationsForClassInfo(classInfo: JpaClassInfo, jpaEntity: EntityType<*>, className2InfoMap: MutableMap<String, JpaClassInfo>) {

        jpaEntity.attributes.forEach {
            if (it.isCollection && it.persistentAttributeType === Attribute.PersistentAttributeType.ONE_TO_MANY) {
                val wrapper = JpaCollectionWrapper(it)
                val metaDetailClass = className2InfoMap[wrapper.referencedType?.name]

                val metaAssocName = it.name
                val metaAssoc = metaDetailClass?.let { it1 -> JpaAssociationInfo(assocName = metaAssocName, detailClass = it1) }
                metaAssoc?.let { it1 -> classInfo.associationList.add(it1) }
            }
        }
    }

    private fun createObjectReference(
        curAttr: Attribute<*, *>,
        className2InfoMap: MutableMap<String, JpaClassInfo>
    ): JdyObjectReferenceInfo? {

        val wrapper = JpaFieldWrapper(curAttr)
        val isKey = (curAttr as SingularAttribute).isId
        val isNotNull = !curAttr.isOptional || !wrapper.isNullable()

        val refTypeName = wrapper.getRefTypeName()
        val referenceType = className2InfoMap[refTypeName]
        val metaAttr = referenceType?.let {
            JdyObjectReferenceInfo(referencedClass = it, attrName = curAttr.getName(), isKey = isKey, notNull = isNotNull, embedded = wrapper.embedded)
        }

        return metaAttr

    }

    private fun createPrimitiveField(curAttr: Attribute<*, *>): JpaAttributeInfo? {
        val wrapper = JpaFieldWrapper(curAttr)
        val metaType = getPrimitiveType(wrapper)
        return if (metaType != null) {

            val isKey = (curAttr as SingularAttribute).isId
            val isNotNull = !curAttr.isOptional || !wrapper.isNullable()
            val isGenerated = wrapper.getGeneratedInfo() != null
            val metaAttr = JpaPrimitiveAttributeInfo(primitiveType = metaType, attrName = curAttr.name, isKey = isKey, notNull = isNotNull)
            metaAttr.generated = isGenerated
            metaAttr
        } else {
            null
        }
    }

    private fun getPrimitiveType(wrapper: JpaFieldWrapper): JpaPrimitiveType? {
        val aTypeClass: Class<*>? = wrapper.getJavaType()
        return if (aTypeClass == null) {
            null
        } else if (aTypeClass.isAssignableFrom(UUID::class.java)) {
            JpaUuidType()
        } else if (aTypeClass.isAssignableFrom(Int::class.java) || Int::class.javaPrimitiveType!!.isAssignableFrom(aTypeClass)) {
            JpaLongType(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
        } else if (aTypeClass.isAssignableFrom(Long::class.java) || Long::class.javaPrimitiveType!!.isAssignableFrom(aTypeClass)) {
            JpaLongType(Long.MIN_VALUE, Long.MAX_VALUE)
        } else if (aTypeClass.isAssignableFrom(Short::class.java) || Short::class.javaPrimitiveType!!.isAssignableFrom(aTypeClass)) {
            JpaLongType(Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong())
        } else if (aTypeClass.isAssignableFrom(Byte::class.java) || ByteArray::class.java.isAssignableFrom(aTypeClass)) {
            JpaBlobType()
        } else if (aTypeClass.isAssignableFrom(String::class.java)) {
            val column = wrapper.getAnnotationInfo(Column::class.java)
            val length = column?.length ?: 40
            val email = wrapper.getAnnotationInfo(Email::class.java)
            if (email == null) JpaStringType(length) else JpaStringType(length, TypeHint.EMAIL)
        } else if (aTypeClass.isAssignableFrom(Date::class.java) || aTypeClass.isAssignableFrom(Timestamp::class.java)
            || aTypeClass.isAssignableFrom(Instant::class.java)
        ) {
            val temporal = wrapper.getAnnotationInfo(Temporal::class.java)
            val temporalType: TemporalType = temporal?.value ?: TemporalType.TIMESTAMP
            val hasDate = temporalType === TemporalType.TIMESTAMP || temporalType === TemporalType.DATE
            val hasTime = temporalType === TemporalType.TIMESTAMP || temporalType === TemporalType.TIME
            JpaTimeStampType(hasDate, hasTime)
        } else if (aTypeClass.isAssignableFrom(LocalDate::class.java)) {
            JpaTimeStampType(isDatePartUsed = true, isTimePartUsed = false)
        } else if (aTypeClass.isAssignableFrom(LocalTime::class.java)) {
            JpaTimeStampType(isDatePartUsed = false, isTimePartUsed = true)
        } else if (aTypeClass.isAssignableFrom(LocalDateTime::class.java)) {
            JpaTimeStampType(isDatePartUsed = true, isTimePartUsed = true)
        } else if (aTypeClass.isAssignableFrom(Boolean::class.java) || Boolean::class.javaPrimitiveType!!.isAssignableFrom(aTypeClass)) {
            JpaBooleanType()
        } else if (aTypeClass.isAssignableFrom(Double::class.java) || aTypeClass.isAssignableFrom(Float::class.java)
            || Double::class.javaPrimitiveType!!.isAssignableFrom(aTypeClass) || Float::class.javaPrimitiveType!!.isAssignableFrom(aTypeClass)
        ) {
            JpaFloatType()
        } else if (aTypeClass.isAssignableFrom(BigDecimal::class.java)) {
            val column = wrapper.getAnnotationInfo(Column::class.java)
            var scale = 0

            if (column != null) {
                scale = column.scale

            }
            val decMin: DecimalMin? = wrapper.getAnnotationInfo(DecimalMin::class.java)
            val decMax: DecimalMax? = wrapper.getAnnotationInfo(DecimalMax::class.java)
            val minValue = if (decMin != null) BigDecimal(decMin.value) else DECIMAL_MIN_VALUE
            val maxValue = if (decMax != null) BigDecimal(decMax.value) else DECIMAL_MAX_VALUE
            JpaDecimalType(minValue, maxValue, scale)
        } else {
            if (aTypeClass.isEnum) {
                val column = wrapper.getAnnotationInfo(Column::class.java)
                val length = column?.length ?: 40
                val domainValues: MutableList<DbDomainValue> = ArrayList<DbDomainValue>()
                for (jpaField in aTypeClass.declaredFields) {
                    if (jpaField.isEnumConstant) {
                        domainValues.add(DbDomainValue(jpaField.name, jpaField.name))
                    }
                }
                return JpaStringType(length, TypeHint.ENUM, domainValues, wrapper.type.javaType)
            }
            null
        }
    }

}


class JpaClassInfo(
    val entityName: String,
    val nameSpace: String,
    val attributeList: MutableList<JpaAttributeInfo> = mutableListOf(),
    val associationList: MutableList<JpaAssociationInfo> = mutableListOf(),
    val embeddable: Boolean = false

)

abstract class JpaAttributeInfo(val attrName: String, val isKey: Boolean = false, var generated: Boolean = false, val notNull: Boolean = false)

class JpaPrimitiveAttributeInfo(
    val primitiveType: JpaPrimitiveType, attrName: String, isKey: Boolean = false, generated: Boolean = false, notNull: Boolean = false

) : JpaAttributeInfo(attrName, isKey, generated, notNull)

class JdyObjectReferenceInfo(
    val referencedClass: JpaClassInfo,
    attrName: String,
    isKey: Boolean = false,
    generated: Boolean = false,
    notNull: Boolean = false,
    val embedded: Boolean = false

) : JpaAttributeInfo(attrName, isKey, generated, notNull)


class JpaAssociationInfo(
    val assocName: String,
    val detailClass: JpaClassInfo
)

interface JpaPrimitiveType {
    fun getTypeName(): String
}

class JpaLongType(
    val minValue: Long?,
    val maxValue: Long?,
    val domValues: List<DbDomainValue>? = null
) : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "Long"
    }
}

class JpaUuidType : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "UUID"
    }
}

class JpaBlobType : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "Blob"
    }
}

class JpaStringType(
    val length: Int,
    val typeHint: TypeHint? = null,
    val domValues: List<DbDomainValue>? = null,
    val domValueType: Class<out Any>? = null
) : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "String"
    }
}

class JpaTimeStampType(
    val isDatePartUsed: Boolean,
    val isTimePartUsed: Boolean
) : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "DateTime"
    }
}

class JpaBooleanType : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "Boolean"
    }
}

class JpaFloatType : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "Float"
    }
}

class JpaDecimalType(
    val minValue: BigDecimal?,
    val maxValue: BigDecimal?,
    val scale: Int,
    val domValues: List<DbDomainValue>? = null
) : JpaPrimitiveType {
    override fun getTypeName(): String {
        return "BigDecimal"
    }
}


enum class TypeHint {
    TELEPHONE,
    URL,
    ENUM,
    EMAIL;

    val dbValue: String
        get() = name
    val representation: String
        get() = name
}


class DbDomainValue(
    val domValue: String,
    val representation: String
)

class JpaCollectionWrapper(anAttr: Attribute<*, *>) {

    private var attr: Attribute<*, *> = anAttr
    var field: Field? = null
    var type: PluralAttribute.CollectionType? = null
    var referencedType: EntityType<*>? = null

    init {
        val listAttr: PluralAttribute<*, *, *> = anAttr as PluralAttribute<*, *, *>
        type = listAttr.collectionType
        field = if (anAttr.getJavaMember() is Field) {
            anAttr.getJavaMember() as Field
        } else {
            null
        }
        referencedType = listAttr.elementType as EntityType
    }

    fun getJavaType(): Class<*>? {
        return attr.javaType
    }

    fun <T : Annotation?> getAnnotationInfo(annotation: Class<T>): T? {
        return if (field is Field) {
            field!!.getAnnotation(annotation)
        } else {
            null
        }
    }

    fun isNullable(): Boolean {
        val columnInfo = getAnnotationInfo(Column::class.java)
        return columnInfo?.nullable ?: false
    }

}

class JpaFieldWrapper(anAttr: Attribute<*, *>) {

    companion object {
        fun getEntityName(jpaEntity: Type<*>): String {

            return when (jpaEntity) {
                is EntityType -> jpaEntity.name
                is EmbeddableType -> jpaEntity.getJavaType().simpleName
                else -> ""
            }
        }
    }

    private var attr: Attribute<*, *> = anAttr
    var field: Member? = null
    val type: Type<*>
    val embedded: Boolean

    init {
        val singAttr: SingularAttribute<*, *> = anAttr as SingularAttribute
        this.type = singAttr.type
        this.field = if (anAttr.getJavaMember() is Field) {
            anAttr.getJavaMember()
        } else {
            null
        }
        this.embedded = attr.persistentAttributeType === EMBEDDED
    }

    fun <T : Annotation?> getAnnotationInfo(annotation: Class<T>): T? {
        return if (field is Field) {
            (field as Field?)!!.getAnnotation(annotation)
        } else {
            null
        }
    }

    fun getJavaType(): Class<*>? {
        return attr.javaType
    }

    fun isNullable(): Boolean {
        val columnInfo = getAnnotationInfo(Column::class.java)
        return columnInfo?.nullable ?: true
    }

    fun getGeneratedInfo(): Any? {
        return getAnnotationInfo(GeneratedValue::class.java)
    }

    fun getRefTypeName(): String {

        return getEntityName(type)

    }
}