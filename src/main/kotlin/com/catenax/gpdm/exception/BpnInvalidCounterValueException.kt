package com.catenax.gpdm.exception

class BpnInvalidCounterValueException (
    counterValue: String?
):RuntimeException("The value of the BPN counter is invalid: $counterValue")