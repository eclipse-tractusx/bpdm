Feature: Share own company business partner data without BPNs
  Scenario: Update Without Address Type
    Given output "CC_SHG_UWAT_1" with external-ID "CC_SHG_UWAT"
    When the sharing member uploads full valid input "CC_SHG_UWAT_2" with external-ID "CC_SHG_UWAT" without address type
    Then the sharing member receives output "CC_SHG_UWAT_2" with external-ID "CC_SHG_UWAT" with address type "LegalAndSiteMainAddress"

  Scenario: Share Without Address Type
    When the sharing member uploads full valid input "CC_SHG_WAT" with external-ID "CC_SHG_WAT" without address type
    Then the sharing member receives output "CC_SHG_WAT" with external-ID "CC_SHG_WAT" with address type "LegalAndSiteMainAddress"

  Scenario Outline: Share With Address Type
    When the sharing member uploads full valid input "CC_SHG_WAT" with external-ID "<externalId>" with address type "<inputAddressType>"
    Then the sharing member receives output "CC_SHG_WAT" with external-ID "<externalId>" with address type "<outputAddressType>"

    Examples:
      | externalId   | inputAddressType           | outputAddressType          |
      | CC_SHG_WAT_1 | LegalAndSiteMainAddress    | LegalAndSiteMainAddress    |
      | CC_SHG_WAT_2 | LegalAddress               | LegalAndSiteMainAddress    |
      | CC_SHG_WAT_3 | SiteMainAddress            | SiteMainAddress            |
      | CC_SHG_WAT_4 | AdditionalAddress          | AdditionalAddress          |

  Scenario Outline: Share With Missing or Invalid data
    When the sharing member uploads input "CC_SHG_WATT" with external-ID "<externalId>" without mandatory field "<mandatoryField>"
    Then the sharing member receives sharing error "CC_SHG_WATT" with external-ID "<externalId>" with error message "<errorMessage>"

    Examples:
      | externalId    | mandatoryField                           | errorMessage                                       |
      | CC_SHG_WATT_1 | legalName                                | Legal name is null                                 |
      | CC_SHG_WATT_2 | physicalAddress.country                  | Physical Address has no country                    |
      | CC_SHG_WATT_3 | physicalAddress.city                     | Physical Address has no city                       |
      | CC_SHG_WATT_4 | alternativeAddress.country               | Alternative Address has no country                 |
      | CC_SHG_WATT_5 | alternativeAddress.city                  | Alternative Address has no city                    |
      | CC_SHG_WATT_6 | alternativeAddress.deliveryServiceType   | Alternative Address has no delivery service type   |
      | CC_SHG_WATT_7 | alternativeAddress.deliveryServiceNumber | Alternative Address has no delivery service number |