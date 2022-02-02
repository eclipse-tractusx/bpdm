package com.catenax.gpdm.exception

class BpnMaxNumberReachedException (
    currentMax: Long
):RuntimeException("Maximum number of BPNs reached: $currentMax")