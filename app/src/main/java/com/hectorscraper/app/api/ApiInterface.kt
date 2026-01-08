package com.hectorscraper.app.api

import com.hectorscraper.app.utils.CategoryData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface ApiInterface {

    @Headers("Content-Type:application/json")
    @GET(".")
    suspend fun doRequestForCategory(): Response<CategoryData>
}