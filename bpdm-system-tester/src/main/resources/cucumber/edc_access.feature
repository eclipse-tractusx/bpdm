# This feature answers one question only: can the BPDM APIs be reached over the EDC at all? It negotiates
# nothing of its own - the negotiation happens once while the context starts - and it reads one page per
# data offer without writing anything, so it can be run as often as an EDC setup needs shaking out:
#
#   java -jar bpdm-system-tester/target/bpdm-system-tester.jar --tags @EdcAccess
#
# Every scenario reports on one offer, so a run says which offers work rather than stopping at the first
# that does not. A run that reaches no API over an EDC skips them all.
#
# The reads deliberately differ in shape: the Pool is read with a GET carrying query parameters and the
# Gate with a POST carrying a body, which between them exercise all four proxy settings of the assets
# (proxyPath, proxyQueryParams, proxyMethod and proxyBody). An empty page is a pass - what is asserted is
# that the data plane answered, not what the API holds.
@EdcAccess
Feature: Reaching The BPDM APIs Over The EDC

  #h3. Test Objective:
  #
  #* Verify the Pool answers a read over the data offer a dataspace participant consumes it with.
  @BPDM @Smoke
  Scenario: Pool Read Access Over Its Data Offer
    Then the Pool answers a read over the EDC

  #h3. Test Objective:
  #
  #* Verify a sharing member's Gate answers a read of its input over the data offer it consumes it with.
  @BPDM @Smoke
  Scenario: Gate Input Access Over Its Data Offer
    Then the Gate input of the first sharing member answers a read over the EDC

  #h3. Test Objective:
  #
  #* Verify a sharing member's Gate answers a read of its output over the data offer it consumes it with.
  @BPDM @Smoke
  Scenario: Gate Output Access Over Its Data Offer
    Then the Gate output of the first sharing member answers a read over the EDC

  #h3. Test Objective:
  #
  #* Verify a further sharing member reaches its own Gate over its own connector.
  @TwoSharingMembers @BPDM
  Scenario: Second Sharing Member Gate Access Over Its Data Offers
    Then the Gate input of the second sharing member answers a read over the EDC
    And the Gate output of the second sharing member answers a read over the EDC

  #h3. Test Objective:
  #
  #* Verify the third sharing member reaches its Gate over the connector that provides the offers.
  @ThreeSharingMembers @BPDM
  Scenario: Third Sharing Member Gate Access Over Its Data Offers
    Then the Gate input of the third sharing member answers a read over the EDC
    And the Gate output of the third sharing member answers a read over the EDC
