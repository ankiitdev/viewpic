package ankiitdev.viewpic.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import ankiitdev.viewpic.repo.CacheRepository
import ankiitdev.viewpic.utils.CacheConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class Viewpic private constructor(context: Context, cacheSize: Int) {
    private val cache = CacheRepository(context, cacheSize)

    suspend fun displayImage(url: String, imageview: ImageView, placeholder: Int?) {
        val bitmap = cache.getImage(url)
        bitmap?.let {
            imageview.setImageBitmap(it)
            return
        } ?: run {
            imageview.tag = url
            if (placeholder != null) imageview.setImageResource(placeholder)
            withContext(Dispatchers.IO) {
                downloadImages(url, imageview, cache)
            }
        }
    }

    private suspend fun downloadImages(url: String, imageView: ImageView, cache: CacheRepository) {
        var bitmap: Bitmap? = null
        val imageQuality = 90
        try {
            withContext(Dispatchers.IO) {
                val imageUrl = URL(url)
                val conn: HttpURLConnection = imageUrl.openConnection() as HttpURLConnection
                val bitmapBody = BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                bitmap = imageQuality.compressBitmap(bitmapBody)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmap?.let {
            if (imageView.tag == url) {
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(it)
                }
            }
            cache.addImage(url, it)
        }
    }

    private fun Int.compressBitmap(bitmap: Bitmap?): Bitmap? {
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, this, stream)
        val byteArray = stream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun clearCache() {
        cache.clearImages()
    }

    companion object {
        fun getInstance(context: Context, cacheSize: Int = CacheConfig.defaultCacheSize): Viewpic {
            val instance: Viewpic by lazy { Viewpic(context, cacheSize) }
            return instance
        }
    }
}