package com.jay.unsplashdemo.di.api

import com.jay.unsplashdemo.Util.NetworkModule
import okhttp3.OkHttpClient

object ImageInstance {

    val api: ImageAPI by lazy {
        NetworkModule.provideRetrofit(OkHttpClient())
            .create(ImageAPI::class.java)
    }
}