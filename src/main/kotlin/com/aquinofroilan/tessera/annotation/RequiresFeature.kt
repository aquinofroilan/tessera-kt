package com.aquinofroilan.tessera.annotation

import com.aquinofroilan.tessera.domain.organization.model.FeatureFlag

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresFeature(
    val value: FeatureFlag = FeatureFlag.CUSTOM,
    val key: String = "",
)
