package com.catenax.gpdm.entity

import java.sql.Date
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "registrations")
class Registration(
    @Column(name = "hardening_grade", nullable = false)
    @Enumerated(EnumType.STRING)
    val hardeningGrade: HardeningGrade,
    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    val issuingAgency: IssuingAgency,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: RegistrationStatus,
    @Column(name = "initial_registration", nullable = false)
    val initialRegistration: LocalDateTime,
    //this column here is still useful even though we already have an update column:
    //UPDATED_AT: last time the record in the database has been changed (for example data corrections)
    //LAST_UPDATE: last time this identifier registration has been through an official update process
    @Column(name = "last_update", nullable = false)
    val lastUpdate: LocalDateTime,
): BaseEntity()

enum class HardeningGrade(val description: String){
    GOLD("Business partner or address data is approved by the content owner."),
    SILVER("A registered issuing agency approved the business partner or address data in terms of data quality trustee"),
    BRONZE("Business partner or address data was fetched from a trusted data source but not manually validated by the related BPN issuing agency."),
    UNKNOWN("The hardening grade is not known to due ongoing data validation or data record retirement.")
}


enum class RegistrationStatus(val description: String){
    ISSUED("Business partner or address data is processed, i.e., data quality is checked, but BPN is not yet assigned and hardening grade not yet set."),
    RELEASED("Business partner or address data is published in the CX BPDM data pool with a BPN identifier."),
    RETIRED("Business partner or address data is no longer published in the CX BPDM data pool .")
}