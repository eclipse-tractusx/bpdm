Feature: Share Business Partner Relations (SHR)
    Scenario: SHR-CAH
        Share new IsAlternativeHeadquarterFor relation

        Given shared legal entity with external-ID 'LE-1' and BPNL 'BPNL-1' finished sharing
        And shared legal entity with external-ID 'LE-2' and BPNL 'BPNL-2'
        When sharing relation with external-ID 'RE-1' of type 'IsAlternativeHeadquarterFor', source 'LE-2' and target 'LE-1'
        Then Pool has relation of type 'IsAlternativeHeadquarterFor', source 'BPNL-2' and target 'BPNL-1'
        And Gate has relation output with external-ID 'RE-1' of type of type 'IsAlternativeHeadquarterFor', source 'BPNL-2' and target 'BPNL-1'
        And Gate has relation changelog entry with external-ID 'RE-1' with type 'CREATE'

    Scenario: SHR-CAHO
        Share new IsAlternativeHeadquarterFor relation with wrong order

        Given shared legal entity with external-ID 'LE-1' and BPNL 'BPNL-1'
        And shared legal entity with external-ID 'LE-2' and BPNL 'BPNL-2'
        When sharing relation with external-ID 'RE-1' of type 'IsAlternativeHeadquarterFor', source 'LE-1' and target 'LE-2'
        Then Pool has relation of type 'IsAlternativeHeadquarterFor', source 'BPNL-2' and target 'BPNL-1'
        And Gate has relation output with external-ID 'RE-1' of type of type 'IsAlternativeHeadquarterFor', source 'BPNL-2' and target 'BPNL-1'

