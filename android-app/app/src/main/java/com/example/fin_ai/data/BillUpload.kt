package com.example.fin_ai.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

/** Builds the multipart upload for /api/transactions/scan from a picked or captured file. */
object BillUpload {

    /** The API (and Groq) caps uploads at 4 MB. */
    private const val MAX_BYTES = 4 * 1024 * 1024
    private const val MAX_IMAGE_SIDE = 1600

    class UnsupportedBillException(message: String) : Exception(message)

    fun prepare(resolver: ContentResolver, uri: Uri): MultipartBody.Part {
        val mime = resolver.getType(uri).orEmpty()
        val name = displayName(resolver, uri)

        val (bytes, type, fileName) = when {
            mime == "application/pdf" -> {
                val pdf = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw UnsupportedBillException("Couldn't open that file.")
                Triple(pdf, mime, name ?: "bill.pdf")
            }
            // Re-encode every image: phone photos are huge, may be HEIC, and carry EXIF
            // rotation the server would otherwise have to deal with.
            mime.startsWith("image/") || mime.isEmpty() ->
                Triple(compressImage(resolver, uri), "image/jpeg", (name ?: "bill").substringBeforeLast('.') + ".jpg")
            else -> throw UnsupportedBillException("Unsupported file. Pick a photo or a PDF.")
        }

        if (bytes.size > MAX_BYTES) throw UnsupportedBillException("File is larger than 4 MB.")
        return MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(type.toMediaType()))
    }

    private fun compressImage(resolver: ContentResolver, uri: Uri): ByteArray {
        val bitmap = decodeScaled(resolver, uri)
            ?: throw UnsupportedBillException("Couldn't read that image.")
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun decodeScaled(resolver: ContentResolver, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF orientation and decodes HEIC on supported devices.
            val source = ImageDecoder.createSource(resolver, uri)
            return runCatching {
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
                    if (scale < 1f) {
                        decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull()
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? =
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
}
