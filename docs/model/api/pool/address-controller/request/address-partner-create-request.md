````mermaid
classDiagram


AddressPartnerCreateRequest ..> AddressState
AddressPartnerCreateRequest ..> AddressIdentifier
AddressPartnerCreateRequest ..> PhysicalPostalAddress
AddressPartnerCreateRequest ..> AlternativePostalAddress
AlternativePostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street

class AddressIdentifier{
+string: value
+string: type
}
class AddressPartnerCreateRequest{
+string: name
+string: bpnParent
+string: index
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}
class AddressState{
+string: description
+date: validFrom
+date: validTo
+string: type
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

````