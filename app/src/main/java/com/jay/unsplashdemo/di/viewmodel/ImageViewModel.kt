package com.jay.unsplashdemo.di.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jay.unsplashdemo.di.application.UnsplashApp
import com.jay.unsplashdemo.model.response.UnsplashImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    val _imageListLiveData = MutableLiveData<List<UnsplashImage>?>()
    val imageListLiveData: LiveData<List<UnsplashImage>?> = _imageListLiveData
    val _errorMessageLiveData = MutableLiveData<String?>()
    val errorMessageLiveData: LiveData<String?> = _errorMessageLiveData

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    fun fetchRandomImages(count: Int, clientId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val imageList = UnsplashApp.imageRepository.getRandomImages(count, clientId)

            if (imageList != null) {
                _imageListLiveData.postValue(imageList)
            } else {
                _errorMessageLiveData.postValue("Failed to fetch random images")
            }
        }
    }
}