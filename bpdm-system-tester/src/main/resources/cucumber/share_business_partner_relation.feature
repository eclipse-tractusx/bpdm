Feature: Share Business Partner Relations (SHR)
    Scenario: Share new IsAlternativeHeadquarterFor relation (SHR-CAH)
        Given shared legal entity with external-ID 'LE-EX-SHR-CAH-1' and BPNL 'LE-BPNL-SHR-CAH-1'
        And shared legal entity with external-ID 'LE-EX-SHR-CAH-2' and BPNL 'LE-BPNL-SHR-CAH-2'
        When sharing relation with external-ID 'R-EX-SHR-CAH' of type 'IsAlternativeHeadquarterFor', source 'LE-EX-SHR-CAH-1' and target 'LE-EX-SHR-CAH-2'
        Then Pool has relation of type 'IsAlternativeHeadquarterFor', source 'LE-BPNL-SHR-CAH-1' and target 'LE-BPNL-SHR-CAH-2'
        And Gate has relation output with external-ID 'R-EX-SHR-CAH' of type of type 'IsAlternativeHeadquarterFor', source 'LE-BPNL-SHR-CAH-1' and target 'LE-BPNL-SHR-CAH-2'
        And Gate has business partner output with external-ID 'LE-EX-SHR-CAH-1' with relation of type 'IsAlternativeHeadquarterFor', source 'LE-BPNL-SHR-CAH-1' and target 'LE-BPNL-SHR-CAH-2'
        And Gate has business partner output with external-ID 'LE-EX-SHR-CAH-2' with relation of type 'IsAlternativeHeadquarterFor', source 'LE-BPNL-SHR-CAH-1' and target 'LE-BPNL-SHR-CAH-2'
                