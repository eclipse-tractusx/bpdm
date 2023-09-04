````mermaid
classDiagram

AlternativePostalAddress ..> GeoCoordinates
MainAddressResponse ..> PhysicalPostalAddress
MainAddressResponse ..> AlternativePostalAddress
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street

class MainAddressResponse{
+string: bpnSite
+date: createdAt
+date: updatedAt
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
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

class GeoCoordinates{
+number: longitude
+number: latitude
+number: altitude
}

class Street{
+string: name
+string: houseNumber
+string: milestone
+string: direction
}


````