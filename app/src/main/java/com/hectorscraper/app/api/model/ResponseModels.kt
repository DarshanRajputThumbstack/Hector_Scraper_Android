package com.hectorscraper.app.api.model

import com.google.gson.annotations.SerializedName

data class MainResponseModel<T>(
    @SerializedName("statusCode") val StatusCode: Int,
    @SerializedName("message") val Message: String,
    @SerializedName("data") val Data: T,
)