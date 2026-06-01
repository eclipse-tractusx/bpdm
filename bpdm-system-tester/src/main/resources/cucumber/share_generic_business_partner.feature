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
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O1"

    Scenario: Refine input to new legal entity

    Given legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O1"

    Scenario: Refine input to new site

    Given legal entity "L1"
    Given site "S1" of legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for site "S1"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "S1"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O1"

    Scenario: Refine input to new additional address of site

    Given legal entity "L1"
    Given site "S1" of legal entity "L1"
    Given additional address "A1" of site "S1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for additional address "A1" of site
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "A1"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O1"

    Scenario: Refine input to new additional address

    Given legal entity "L1"
    Given additional address "A1" of legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for additional address "A1" of legal entity
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "A1"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O1"

    Scenario: Update Site-Based Legal Entity

    Given site-based legal entity "L1"
    Given site-based legal entity "L2"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given input data "I2"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for site-based legal entity "L1"
    Given output data "O2" based on input "I2" for site-based legal entity "L2"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    Then polling business partner record "BP1" sharing state leads to success
    When uploading into business partner record "BP1" input data "I2"
    When record "BP1" is refined to "L2"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O2"

    Scenario: Update Legal Entity

    Given legal entity "L1"
    Given legal entity "L2"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given input data "I2"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    Given output data "O2" based on input "I2" for legal entity "L2"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    Then polling business partner record "BP1" sharing state leads to success
    When uploading into business partner record "BP1" input data "I2"
    When record "BP1" is refined to "L2"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O2"

    Scenario: Update Site

    Given legal entity "L1"
    Given site "S1" of legal entity "L1"
    Given site "S2" of legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given input data "I2"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for site "S1"
    Given output data "O2" based on input "I2" for site "S2"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "S1"
    Then polling business partner record "BP1" sharing state leads to success
    When uploading into business partner record "BP1" input data "I2"
    When record "BP1" is refined to "S2"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O2"

    Scenario: Update Additional Address Of Site

    Given legal entity "L1"
    Given site "S1" of legal entity "L1"
    Given additional address "A1" of site "S1"
    Given additional address "A2" of site "S1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given input data "I2"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for additional address "A1" of site
    Given output data "O2" based on input "I2" for additional address "A2" of site
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "A1"
    Then polling business partner record "BP1" sharing state leads to success
    When uploading into business partner record "BP1" input data "I2"
    When record "BP1" is refined to "A2"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O2"

    Scenario: Update Additional Address Of Legal Entity

    Given legal entity "L1"
    Given additional address "A1" of legal entity "L1"
    Given additional address "A2" of legal entity "L1"
    Given input data "I1"
        | isOwnCompanyData | true |
    Given input data "I2"
        | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for additional address "A1" of legal entity
    Given output data "O2" based on input "I2" for additional address "A2" of legal entity
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "A1"
    Then polling business partner record "BP1" sharing state leads to success
    When uploading into business partner record "BP1" input data "I2"
    When record "BP1" is refined to "A2"
    Then polling business partner record "BP1" sharing state leads to success
    And business partner record "BP1" output data matches "O2"

