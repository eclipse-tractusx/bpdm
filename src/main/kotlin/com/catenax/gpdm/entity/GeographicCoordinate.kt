package com.catenax.gpdm.entity

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class GeographicCoordinate (
    @Column(name = "latitude")
    val latitude: Double,
    @Column(name = "longitude")
    val longitude: Double,
    @Column(name = "altitude")
    val altitude: Double,
)