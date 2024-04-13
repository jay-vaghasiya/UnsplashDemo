package com.jay.unsplashdemo.model.response

import com.google.gson.annotations.SerializedName

data class User(
    val name: String,
    @SerializedName("portfolio_url") val portfolioUrl: String?,
    @SerializedName("profile_image") val profileImage: ProfileImage,
    val username: String
)