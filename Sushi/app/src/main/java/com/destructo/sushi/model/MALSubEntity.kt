package com.destructo.sushi.model

import com.squareup.moshi.Json

class MALSubEntity(
    @Json(name = "image_url")
    val imageUrl: String,
    @Json(name = "mal_id")
    val malId: Int,
    @Json(name = "name")
    val name: String,
    @Json(name = "url")
    val url: String
)