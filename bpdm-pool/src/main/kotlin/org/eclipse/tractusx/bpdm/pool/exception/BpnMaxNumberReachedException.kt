package org.eclipse.tractusx.bpdm.pool.exception

class BpnMaxNumberReachedException (
    currentMax: Long
):RuntimeException("Maximum number of BPNs reached: $currentMax")