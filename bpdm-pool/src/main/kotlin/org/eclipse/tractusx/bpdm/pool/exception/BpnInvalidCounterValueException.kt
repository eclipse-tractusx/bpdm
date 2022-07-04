package org.eclipse.tractusx.bpdm.pool.exception

class BpnInvalidCounterValueException (
    counterValue: String?
):RuntimeException("The value of the BPN counter is invalid: $counterValue")