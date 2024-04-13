package com.jay.unsplashdemo.di.application

import android.app.Application
import com.jay.unsplashdemo.di.repository.ImageRepository
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UnsplashApp : Application() {
    companion object {
        lateinit var imageRepository: ImageRepository
            private set
    }

    override fun onCreate() {
        super.onCreate()
        imageRepository = ImageRepository()

    }
}