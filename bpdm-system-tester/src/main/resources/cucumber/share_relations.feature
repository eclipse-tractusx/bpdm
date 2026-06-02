@CXTPM-944
Feature: Share business partner relations

    @TEST_CXTPM-970
    Scenario: Share new IsOwnedBy-Relation

    Given legal entity "L1"
    Given legal entity "L2"
    Given input data "I1"
            | isOwnCompanyData | true |
    Given input data "I2"
            | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    Given output data "O2" based on input "I2" for legal entity "L2"
    Given relation input data "RI1" of type "IsOwnedBy" from "BP1" to "BP2"
    Given relation output data "RO1" based on input "RI1"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    When uploading into business partner record "BP2" input data "I2"
    When record "BP2" is refined to "L2"
    When uploading into relation record "R1" input data "RI1"
    When legal entity relation record "R1" is refined to "RO1"
    Then polling relation record "R1" sharing state leads to success
    Then relation record "R1" output data matches "RO1"

    Scenario: Share new IsAlternativeHeadquarter-Relation

    Given legal entity "L1"
    Given legal entity "L2"
    Given input data "I1"
            | isOwnCompanyData | true |
    Given input data "I2"
            | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    Given output data "O2" based on input "I2" for legal entity "L2"
    Given relation input data "RI1" of type "IsAlternativeHeadquarterFor" from "BP1" to "BP2"
    Given relation output data "RO1" based on input "RI1"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    When uploading into business partner record "BP2" input data "I2"
    When record "BP2" is refined to "L2"
    When uploading into relation record "R1" input data "RI1"
    When legal entity relation record "R1" is refined to "RO1"
    Then polling relation record "R1" sharing state leads to success
    Then relation record "R1" output data matches "RO1" in any direction

    Scenario: Share new IsManagedBy-Relation

    Given legal entity "L1"
    Given legal entity "L2"
    Given input data "I1"
            | isOwnCompanyData | true |
    Given input data "I2"
            | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    Given output data "O2" based on input "I2" for legal entity "L2"
    Given relation input data "RI1" of type "IsManagedBy" from "BP1" to "BP2" with future validity
    Given relation output data "RO1" based on input "RI1"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    When uploading into business partner record "BP2" input data "I2"
    When record "BP2" is refined to "L2"
    When uploading into relation record "R1" input data "RI1"
    When legal entity relation record "R1" is refined to "RO1"
    Then polling relation record "R1" sharing state leads to success
    Then relation record "R1" output data matches "RO1"

    Scenario: Share new IsReplacedBy-Relation

    Given legal entity "L1"
    Given additional address "A1" of legal entity "L1"
    Given input data "I1"
            | isOwnCompanyData | true |
    Given input data "I2"
            | isOwnCompanyData | true |
    Given output data "O1" based on input "I1" for legal entity "L1"
    Given output data "O1" based on input "I1" for additional address "A1" of legal entity
    Given relation input data "RI1" of type "IsReplacedBy" from "BP1" to "BP2"
    Given relation output data "RO1" based on input "RI1"
    When uploading into business partner record "BP1" input data "I1"
    When record "BP1" is refined to "L1"
    When uploading into business partner record "BP2" input data "I2"
    When record "BP2" is refined to "A1"
    When uploading into relation record "R1" input data "RI1"
    When address relation record "R1" is refined to "RO1"
    Then polling relation record "R1" sharing state leads to success
    Then relation record "R1" output data matches "RO1"






