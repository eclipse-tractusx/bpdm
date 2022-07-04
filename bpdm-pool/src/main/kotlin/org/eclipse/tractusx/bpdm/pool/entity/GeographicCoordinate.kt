package org.eclipse.tractusx.bpdm.pool.entity

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class GeographicCoordinate (
    @Column(name = "latitude")
    val latitude: Float,
    @Column(name = "longitude")
    val longitude: Float,
    @Column(name = "altitude")
    val altitude: Float?,
)