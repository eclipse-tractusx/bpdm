@CXTPM-944
Feature: Share own company business partner data without BPNs

    # A sharing member uploads a business partner record without specifying an
    # address type. The system should automatically resolve it to LegalAndSiteMainAddress
    # as the default. This covers the implicit address type assignment during the
    # cleaning and enrichment pipeline.
    #
    @TEST_CXTPM-942
    Scenario: Refine input to new site-based legal entity

    Given site-based legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for site-based legal entity "L1"
    When uploading into business partner record "A" input data "I1"
    When record "A" is refined to "L1"
    Then polling business partner record "A" sharing state leads to success
    And business partner record "A" output data matches "O1"

    Scenario: Refine input to new legal entity

    Given legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    When uploading into business partner record "A" input data "I1"
    When record "A" is refined to "L1"
    Then polling business partner record "A" sharing state leads to success
    And business partner record "A" output data matches "O1"

    Scenario: Refine input to new site

    Given legal entity "L1"
    Given site "S1" of legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for site "S1"
    When uploading into business partner record "A" input data "I1"
    When record "A" is refined to "S1"
    Then polling business partner record "A" sharing state leads to success
    And business partner record "A" output data matches "O1"