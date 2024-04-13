package com.jay.unsplashdemo.di.repository

import android.net.http.HttpException
import android.os.Build
import androidx.annotation.RequiresExtension
import com.jay.unsplashdemo.di.api.ImageInstance
import com.jay.unsplashdemo.model.response.UnsplashImage
import java.io.IOException
import javax.inject.Singleton

@Singleton
class ImageRepository {
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    suspend fun getRandomImages(count: Int, clientId: String): List<UnsplashImage>? {
        val response = try {
            ImageInstance.api.getRandomImages(count, clientId)
        } catch (e: IOException) {
            null
        } catch (e: HttpException) {
            null
        } catch (e: Exception) {
            null
        }

        return response?.takeIf { it.isSuccessful }?.body()
    }
}