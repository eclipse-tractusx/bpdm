package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "legal_forms")
class LegalForm(
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "url")
    val url: String?,
    @Column(name = "language", nullable = false)
    val language: LanguageCode,
    @Column(name = "abbreviation")
    val mainAbbreviation: String?,
    @ManyToMany(cascade = [CascadeType.ALL])
    @JoinTable(
        name = "legal_forms_legal_categories",
        joinColumns = [JoinColumn(name = "form_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")],
        indexes = [Index(columnList = "form_id"), Index(columnList = "category_id")]
    )
    val categories: Set<LegalFormCategory>,
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String
) : BaseEntity()