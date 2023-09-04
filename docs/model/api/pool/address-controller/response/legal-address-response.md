````mermaid
classDiagram

AlternativePostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
LegalAddressResponse ..> PhysicalPostalAddress
LegalAddressResponse ..> AlternativePostalAddress


class AlternativePostalAddress{
+string: country
+string: postalCode
+string: city
+string: administrativeAreaLevel1
+string: deliveryServiceNumber
+string: deliveryServiceType
+string: deliveryServiceQualifier
+GeoCoordinates: geographicCoordinates;
}

class PhysicalPostalAddress{
+string: country
+string: postalCode
+string: city
+string: administrativeAreaLevel1
+string: administrativeAreaLevel2
+string: administrativeAreaLevel3
+string: district
+string: companyPostalCode
+string: industrialZone
+string: building
+string: floor
+string: door
+GeoCoordinates: geographicCoordinates;
+Street: street;
}

class Street{
+string: name
+string: houseNumber
+string: milestone
+string: direction
}

class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}

class LegalAddressResponse{
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}

````