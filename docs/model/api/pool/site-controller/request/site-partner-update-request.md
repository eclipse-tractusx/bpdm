````mermaid
classDiagram

SitePartnerUpdateRequest ..> SiteState
SitePartnerUpdateRequest ..> LogisticAddressDto
AlternativePostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
LogisticAddressDto ..> AddressState
LogisticAddressDto ..> AddressIdentifier
LogisticAddressDto ..> PhysicalPostalAddress
LogisticAddressDto ..> AlternativePostalAddress

class LogisticAddressDto{
+string: name
+AddressState[]: states;
+AddressIdentifier[]: identifiers;
+PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress;
}

class SiteState{
+string: description
+date: validFrom
+date: validTo
+string: type
}

class AddressIdentifier{
+string: value
+string: type
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

class SitePartnerUpdateRequest{
+string: bpns
+string: name
+SiteState[]: states;
+LogisticAddressDto: mainAddress;
}
class AddressState{
+string: description
+date: validFrom
+date: validTo
+string: type
}

````