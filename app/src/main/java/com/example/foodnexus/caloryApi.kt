package com.example.foodnexus

import com.example.foodnexus.Models.NutritionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface caloryApi {
    @GET("v1_1/search/{food}")
    suspend fun getFoodInfo(
        @Path("food") food: String,

        // Required Nutritionix parameters:
        @Query("results") results: String = "0:1",               // <start>:<end> (fetch first result)
        @Query("appId") appId: String,
        @Query("appKey") appKey: String,
        @Query("fields") fields: String = "item_name,nf_calories"
    ): Response<NutritionResponse>
}
