package com.destructo.jikanplay.model.common


import com.squareup.moshi.Json

data class Prop(
    @Json(name = "from")
    val date: Date?=null,
    @Json(name = "to")
    val to: Date?=null
)