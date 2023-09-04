classDiagram

AlternativePostalAddress ..> GeoCoordinates LegalEntityPartnerUpdateRequest ..> LegalEntityIdentifier LegalEntityPartnerUpdateRequest ..> LegalEntityState
LegalEntityPartnerUpdateRequest ..> Classification LegalEntityPartnerUpdateRequest ..> LogisticAddressDto LogisticAddressDto ..> AddressState LogisticAddressDto
..> AddressIdentifier LogisticAddressDto ..> PhysicalPostalAddress LogisticAddressDto ..> AlternativePostalAddress PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street

class AddressIdentifier{ +string: value +string: type } class AddressState{ +string: description +date: validFrom +date: validTo +string: type } class
AlternativePostalAddress{ +string: country +string: postalCode +string: city +string: administrativeAreaLevel1 +string: deliveryServiceNumber +string:
deliveryServiceType +string: deliveryServiceQualifier +GeoCoordinates: geographicCoordinates; } class Classification{ +string: value +string: code +string: type
} class GeoCoordinates{ +number: longitude +number: latitude +number: altitude } class LegalEntityIdentifier{ +string: value +string: type +string: issuingBody
} class LegalEntityPartnerUpdateRequest{ +string: bpnl +string: legalName +string: legalShortName +string: legalForm +LegalEntityIdentifier[]: identifiers;
+LegalEntityState[]: states; +Classification[]: classifications; +LogisticAddressDto: legalAddress; } class LegalEntityState{ +string: officialDenotation +date:
validFrom +date: validTo +string: type } class LogisticAddressDto{ +string: name +AddressState[]: states; +AddressIdentifier[]: identifiers;
+PhysicalPostalAddress: physicalPostalAddress; +AlternativePostalAddress: alternativePostalAddress; } class PhysicalPostalAddress{ +string: country +string:
postalCode +string: city +string: administrativeAreaLevel1 +string: administrativeAreaLevel2 +string: administrativeAreaLevel3 +string: district +string:
companyPostalCode +string: industrialZone +string: building +string: floor +string: door +GeoCoordinates: geographicCoordinates; +Street: street; } class
Street{ +string: name +string: houseNumber +string: milestone +string: direction }

