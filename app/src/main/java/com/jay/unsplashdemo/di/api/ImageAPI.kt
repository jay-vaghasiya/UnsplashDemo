package com.jay.unsplashdemo.di.api

import com.jay.unsplashdemo.model.response.UnsplashImage
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ImageAPI {

    @GET("/photos/random")
    suspend fun getRandomImages(
        @Query("count") count: Int,
        @Query("client_id") clientId: String
    ): Response<List<UnsplashImage>>
}