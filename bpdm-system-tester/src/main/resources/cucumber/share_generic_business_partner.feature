Feature: Share Valid Generic Business Partner without BPNs
    Scenario: Update Without Address Type
        Given output "CC_SHG_UWAT_1" with external-ID "CC_SHG_UWAT"
        When the sharing member uploads full valid input "CC_SHG_UWAT_2" with external-ID "CC_SHG_UWAT" without address type
        Then the sharing member receives output "CC_SHG_UWAT_2" with external-ID "CC_SHG_UWAT" with address type "LegalAndSiteMainAddress"

    Scenario: Share Without Address Type
        When the sharing member uploads full valid input "CC_SHG_WAT" with external-ID "CC_SHG_WAT" without address type
        Then the sharing member receives output "CC_SHG_WAT" with external-ID "CC_SHG_WAT" with address type "LegalAndSiteMainAddress"

    Scenario: Share With Address Type
        When the sharing member uploads full valid input "CC_SHG_WAT" with external-ID "CC_SHG_WAT" with address type "<inputAddressType>"
        Then the sharing member receives output "CC_SHG_WAT" with external-ID "CC_SHG_WAT" with address type "<outputAddressType>"

        Examples:
          | inputAddressType           | outputAddressType          |
          | LegalAndSiteMainAddress    | LegalAndSiteMainAddress    |
          | LegalAddress               | LegalAndSiteMainAddress    |
          | SiteMainAddress            | SiteMainAddress            |
          | AdditionalAddress          | AdditionalAddress          |
