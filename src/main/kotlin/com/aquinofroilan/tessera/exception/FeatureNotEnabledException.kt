package com.aquinofroilan.tessera.exception

class FeatureNotEnabledException(
    message: String = "Feature is not enabled for your organization's billing plan",
) : RuntimeException(message)
