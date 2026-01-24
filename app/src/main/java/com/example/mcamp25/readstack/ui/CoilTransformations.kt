package com.example.mcamp25.readstack.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import coil.size.Size
import coil.transform.Transformation
import androidx.core.graphics.createBitmap
import kotlin.math.abs

class SharpenAndContrastTransformation(
    private val contrast: Float = 1.25f,
    private val brightness: Float = -5f,
    private val saturation: Float = 1.15f,
    private val sharpenAmount: Float = 3.0f,
    private val threshold: Int = 35
) : Transformation {

    override val cacheKey: String = "SharpenAndContrast_v14_Balance(c=$contrast,b=$brightness,s=$saturation,a=$sharpenAmount,t=$threshold)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)


        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val idx = yOffset + x
                
                val c = pixels[idx]
                val rC = (c shr 16) and 0xFF
                val gC = (c shr 8) and 0xFF
                val bC = c and 0xFF

                // 8-neighbor analysis
                val neighbors = intArrayOf(
                    pixels[idx - width - 1], pixels[idx - width], pixels[idx - width + 1],
                    pixels[idx - 1],         /* center */         pixels[idx + 1],
                    pixels[idx + width - 1], pixels[idx + width], pixels[idx + width + 1]
                )

                var sumR = 0f; var sumG = 0f; var sumB = 0f
                for (n in neighbors) {
                    sumR += (n shr 16) and 0xFF
                    sumG += (n shr 8) and 0xFF
                    sumB += n and 0xFF
                }
                
                val avgR = sumR / 8f
                val avgG = sumG / 8f
                val avgB = sumB / 8f

                val diffR = rC - avgR
                val diffG = gC - avgG
                val diffB = bC - avgB


                val variance = abs(diffR) + abs(diffG) + abs(diffB)

                // Adaptive Protection: Quad-luma curve to prevent white-out
                val luma = (rC * 0.299f + gC * 0.587f + bC * 0.114f) / 255f
                val protection = (1.0f - luma * luma).coerceIn(0.3f, 1.0f)

                // Only sharpen if the detail is strong enough to be text.
                // Uses a smooth transition to avoid "grainy" artifacts.
                val factor = if (variance > threshold) {
                    ((variance - threshold) / 20f).coerceIn(0f, 1f) * sharpenAmount * protection
                } else 0f

                val resR = rC + factor * diffR
                val resG = gC + factor * diffG
                val resB = bC + factor * diffB

                resultPixels[idx] = (0xFF shl 24) or 
                                   (resR.toInt().coerceIn(0, 255) shl 16) or 
                                   (resG.toInt().coerceIn(0, 255) shl 8) or 
                                   resB.toInt().coerceIn(0, 255)
            }
        }

        // Copy borders
        for (i in 0 until pixels.size) {
            if (resultPixels[i] == 0) resultPixels[i] = pixels[i]
        }

        val sharpenedBitmap = Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
        val output = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        val t = (1.0f - contrast) / 2.0f * 255.0f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, t + brightness,
            0f, contrast, 0f, 0f, t + brightness,
            0f, 0f, contrast, 0f, t + brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        cm.postConcat(satMatrix)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(sharpenedBitmap, 0f, 0f, paint)

        sharpenedBitmap.recycle()
        return output
    }
}


fun String.toHighResBookUrl(): String {
    // 1. Clean and upgrade to HTTPS
    val cleanUrl = this.replace("http:", "https:")
                      .replace("&edge=curl", "")
                      .replace("edge=curl", "")

    // Extract Book Image ID

    val idRegex = Regex("id=([^&]+)")
    val matchResult = idRegex.find(cleanUrl)
    val bookImgId = matchResult?.groupValues?.get(1)

    return if (bookImgId != null) {
        // 3. Construct Custom FIFE URL (w800-h1200 for 1080p-like quality)
        "https://books.google.com/books/content?id=$bookImgId&printsec=frontcover&img=1&zoom=1&w=800&h=1200&source=gbs_api"
    } else {
        // Fallback to original cleaned URL
        cleanUrl
    }
}
