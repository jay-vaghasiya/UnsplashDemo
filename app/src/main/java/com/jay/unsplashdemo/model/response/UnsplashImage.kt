package com.jay.unsplashdemo.model.response

import com.google.gson.annotations.SerializedName

data class UnsplashImage(
    val color: String,
    @SerializedName("created_at")
    val createdAt: String,
    val description: String,
    val height: Int,
    val id: String,
    val likes: Int,
    @SerializedName("updated_at") val updatedAt: String,
    val urls: Urls,
    val user: User,
    val width: Int
)