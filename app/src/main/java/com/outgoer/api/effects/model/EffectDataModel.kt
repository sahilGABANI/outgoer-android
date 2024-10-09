package com.outgoer.api.effects.model

data class EffectResponse(
    val effectId: Int,
    val effectName: String,
    val effectFileName: String,
    val effectImageName: Int,
    val type: String
)