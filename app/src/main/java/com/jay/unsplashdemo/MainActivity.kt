package com.jay.unsplashdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.jay.unsplashdemo.Util.Constant
import com.jay.unsplashdemo.di.viewmodel.ImageViewModel
import com.jay.unsplashdemo.model.ImageList
import com.jay.unsplashdemo.ui.theme.UnsplashDemoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var imageViewModel: ImageViewModel
    private val uriList = mutableListOf<ImageList>()
    private lateinit var imageCache: LruCache<String, Bitmap>

    // Ensures compatibility with Android S (API level 31)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Set the content of the activity with Compose
            UnsplashDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Composable content goes here
                    var dataObserved by remember { mutableStateOf(false) }

                    // Initialize ViewModel for fetching data
                    imageViewModel = ViewModelProvider(this)[ImageViewModel::class.java]
                    imageViewModel.imageListLiveData.observe(this) { imageList ->
                        // Observes changes in the image list LiveData
                        Log.d("LilList", imageList.toString())
                        if (imageList != null) {
                            uriList.clear()
                            // Populate the URI list for images
                            imageList.forEach {
                                val uri = it.urls.small
                                val name = it.description
                                uriList.add(ImageList(uri = uri, imageName = name))
                            }
                            dataObserved = true
                            Log.d("LilList", "Image List")
                        }
                    }
                    imageViewModel.errorMessageLiveData.observe(this) { errorMessage ->
                        // Observes error messages from ViewModel
                        Log.e("LilError", "Error Message: $errorMessage")
                    }
                    // Fetch random images from API
                    imageViewModel.fetchRandomImages(30, Constant.client_id)

                    // Initialize image cache
                    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
                    val cacheSize = maxMemory / 8
                    imageCache = object : LruCache<String, Bitmap>(cacheSize) {
                        override fun sizeOf(key: String, bitmap: Bitmap): Int {
                            // Returns the size of the bitmap for caching
                            return bitmap.byteCount / 1024
                        }
                    }

                    // Display images if data is observed and URI list is not empty
                    if (dataObserved && uriList.isNotEmpty()) {
                        DisplayImage(this, uriList = uriList)
                    } else {
                        Log.d("list", "Data not observed or Image List Empty")
                    }
                }
            }
        }
    }

    // Composable function to display images in a staggered grid
    @Composable
    private fun DisplayImage(context: Context, uriList: List<ImageList>) {
        LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(2)) {
            items(items = uriList) { item ->
                ListItems(context, item.uri, item.imageName)
            }
        }
    }

    // Composable function to display individual image items
    @Composable
    private fun ListItems(context: Context, uri: String?, imageName: String?) {
        var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var isLoading by remember { mutableStateOf(true) } // Track loading state

        // Coroutine launched to load images asynchronously
        LaunchedEffect(key1 = uri) {
            if (uri != null) {
                val cachedImage = loadImageFromCache(uri)
                if (cachedImage != null) {
                    // Image found in cache
                    imageBitmap = cachedImage
                    isLoading = false // Set loading state to false
                } else {
                    // Image not in cache, start downloading
                    val downloadedImage = downloadImage(uri)
                    if (downloadedImage != null) {
                        imageBitmap = downloadedImage
                        saveImageToCache(context, uri, downloadedImage)
                        isLoading = false
                    }
                }
            } else {
                Log.e("BigList", "URI is null")
            }
        }

        if (isLoading) {
            // Display progress indicator while loading
            Box(modifier = Modifier.fillMaxSize(), content = {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            })
        } else {
            // Image loaded, display it
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = imageName ?: "Image",
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        }
    }

    // Function to load image from cache
    private fun loadImageFromCache(uri: String): Bitmap? {
        val cachedBitmap = imageCache.get(uri)
        if (cachedBitmap != null) {
            Log.d("ImageCache", "Image loaded from cache: $uri")
            return cachedBitmap
        }

        // Load image from disk if not in cache
        val fileName = getFileName(uri)
        val cachedFile = File(fileName)
        return if (cachedFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(fileName)
            imageCache.put(uri, bitmap) // Cache the loaded bitmap
            Log.d("ImageCache", "Image loaded from disk: $uri")
            bitmap.copy(bitmap.config, bitmap.isMutable) // Avoids potential issues with mutability
        } else {
            null
        }
    }

    // Function to download image from URL using coroutines
    private suspend fun downloadImage(uri: String): Bitmap? = withContext(Dispatchers.IO) {
        val urlConnection = URL(uri).openConnection() as HttpURLConnection
        return@withContext try {
            urlConnection.connect()
            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = urlConnection.inputStream
                // Compressing Image by 10%
                val options = BitmapFactory.Options()
                options.inSampleSize = 1
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                bitmap
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            urlConnection.disconnect()
        }
    }

    // Function to save image to cache
    private fun saveImageToCache(context: Context, uri: String, bitmap: Bitmap) {
        val fileName = getFileName(uri)
        val file = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(file)

        // Apply compression to the bitmap before saving
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fileOutputStream)
        fileOutputStream.close()

        imageCache.put(uri, bitmap) // Cache the saved bitmap
    }

    // Helper function to get filename from URI
    private fun getFileName(uri: String): String {
        val segments = uri.split("/")
        return segments[segments.lastIndex]
    }
}
