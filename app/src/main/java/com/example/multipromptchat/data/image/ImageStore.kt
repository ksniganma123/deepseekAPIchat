package com.example.multipromptchat.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 图片落盘 + 压缩 + 转 data URL。
 *
 * 规则（与 README / 界面提示保持一致）：
 * - 源文件上限 [MAX_SOURCE_BYTES]（8 MB），超过直接拒绝，不解码；
 * - 明确支持 JPEG / PNG / WebP；其它格式（如 HEIC）若系统能解码会自动转成 JPEG，不能解码则提示不支持；
 * - 统一压成最长边 [MAX_EDGE]（或调用方指定）的 JPEG，质量逐级下降，直到 ≤ [MAX_STORED_BYTES]；
 * - 落盘位置：filesDir/<dir>/<uuid>.jpg，路径入库，重启后仍可显示。
 */
object ImageStore {

    const val MAX_SOURCE_BYTES = 8L * 1024 * 1024
    const val MAX_STORED_BYTES = 1L * 1024 * 1024
    const val MAX_EDGE = 1280

    const val DIR_CHAT = "chat_images"
    const val DIR_WALLPAPER = "wallpaper"

    private const val JPEG_QUALITY = 84
    private val QUALITY_STEPS = intArrayOf(84, 70, 55, 40)
    private val SUPPORTED_MIME = setOf("image/jpeg", "image/png", "image/webp")

    enum class Failure { UNSUPPORTED_FORMAT, TOO_LARGE, DECODE_FAILED, IO_ERROR }

    sealed interface Result {
        data class Success(
            val file: File,
            val width: Int,
            val height: Int,
            val bytes: Long,
        ) : Result

        data class Error(val failure: Failure) : Result
    }

    /** 支持的格式说明（界面/文档共用）。 */
    val supportedFormatLabel: String = "JPEG · PNG · WebP"

    suspend fun import(
        context: Context,
        uri: Uri,
        dirName: String,
        maxEdge: Int = MAX_EDGE,
    ): Result = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = runCatching { resolver.getType(uri) }.getOrNull()

        val declaredSize = runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        }.getOrDefault(-1L)
        if (declaredSize > MAX_SOURCE_BYTES) return@withContext Result.Error(Failure.TOO_LARGE)

        // 1) 先读边界，判断能否解码 & 算采样率
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsOk = runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }.isSuccess
        if (!boundsOk || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            val unsupported = mime != null && mime !in SUPPORTED_MIME
            return@withContext Result.Error(
                if (unsupported) Failure.UNSUPPORTED_FORMAT else Failure.DECODE_FAILED
            )
        }

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        // 2) 按目标边长算采样率，避免大图 OOM
        val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longEdge / (sample * 2) >= maxEdge) sample *= 2

        val decoded = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            }
        }.getOrNull() ?: return@withContext Result.Error(Failure.DECODE_FAILED)

        val oriented = runCatching { applyOrientation(decoded, orientation) }.getOrElse { decoded }
        val scaled = runCatching { scaleToMaxEdge(oriented, maxEdge) }.getOrElse { oriented }

        // 3) 压缩落盘，逐级降质量直到体积达标
        val dir = File(context.filesDir, dirName).apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        var stored = false
        for (quality in QUALITY_STEPS) {
            val ok = runCatching {
                target.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
            }.getOrDefault(false)
            if (!ok) break
            if (target.length() in 1..MAX_STORED_BYTES) {
                stored = true
                break
            }
        }
        if (!stored) {
            target.delete()
            return@withContext Result.Error(Failure.IO_ERROR)
        }

        Result.Success(
            file = target,
            width = scaled.width,
            height = scaled.height,
            bytes = target.length(),
        )
    }

    /** 读文件转成 OpenAI 兼容的 data URL；失败返回 null。 */
    fun toDataUrl(file: File): String? = runCatching {
        if (!file.isFile || file.length() == 0L) return@runCatching null
        val bytes = file.readBytes()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }.getOrNull()

    fun deleteQuietly(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.isFile }?.delete() }
    }

    fun fileOf(path: String): File = File(path)

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longEdge.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            maxOf(1, (bitmap.width * ratio).toInt()),
            maxOf(1, (bitmap.height * ratio).toInt()),
            true,
        )
    }
}
