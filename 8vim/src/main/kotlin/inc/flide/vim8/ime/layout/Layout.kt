package inc.flide.vim8.ime.layout

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.flatten
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.none
import arrow.core.raise.catch
import arrow.core.right
import arrow.core.some
import inc.flide.vim8.R
import inc.flide.vim8.cache
import inc.flide.vim8.datastore.model.PreferenceSerDe
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.ime.layout.models.error.LayoutError
import inc.flide.vim8.ime.layout.models.info
import inc.flide.vim8.ime.layout.models.yaml.versions.common.name
import inc.flide.vim8.lib.android.tryOrNull
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import org.apache.commons.codec.digest.DigestUtils

private val isoCodes = Locale.getISOLanguages().toSet()

fun embeddedLayouts(
    layoutLoader: LayoutLoader,
    context: Context
): List<Pair<EmbeddedLayout, String>> = (R.raw::class.java.fields)
    .filter { isoCodes.contains(it.name) }
    .flatMap { field ->
        EmbeddedLayout(field.name)
            .let { layout ->
                layout
                    .loadKeyboardData(layoutLoader, context)
                    .getOrNone()
                    .filterNot {
                        it.totalLayers == 0
                    }
                    .map { layout to it.toString() }
            }.toList()
    }.sortedBy { it.second }

interface Layout<T> {
    val path: T
    fun inputStream(context: Context): Either<LayoutError, InputStream>
    fun md5(context: Context): Option<String>
    fun defaultName(context: Context): String
}

private fun InputStream.readSnapshot(): Either<LayoutError, ByteArray> = try {
    use { it.readBytes().right() }
} catch (exception: Throwable) {
    ExceptionWrapperError(exception).left()
}

private fun <T> Layout<T>.cacheKey(
    snapshot: ByteArray,
    context: Context
): Either<LayoutError, String> = try {
    when (this) {
        is CustomLayout -> DigestUtils.md5Hex(snapshot).right()
        else -> md5(context).toEither { ExceptionWrapperError(Exception("MD5")) }
    }
} catch (exception: Throwable) {
    ExceptionWrapperError(exception).left()
}

private fun parseSnapshot(
    snapshot: ByteArray,
    layoutLoader: LayoutLoader
): Either<LayoutError, KeyboardData> = try {
    ByteArrayInputStream(snapshot).use { layoutLoader.loadKeyboardData(it) }
} catch (exception: Throwable) {
    ExceptionWrapperError(exception).left()
}

fun <T> Layout<T>.loadKeyboardData(
    layoutLoader: LayoutLoader,
    context: Context
): Either<LayoutError, KeyboardData> = inputStream(context)
    .flatMap { it.readSnapshot() }
    .flatMap { snapshot ->
        cacheKey(snapshot, context).flatMap { cacheKey ->
            val cache by context.cache()
            cache.load(cacheKey).fold({
                parseSnapshot(snapshot, layoutLoader)
                    .onRight { cache.add(cacheKey, it) }
            }, { it.right() })
        }
    }
    .map { keyboardData ->
        KeyboardData.info.name.modify(keyboardData) { name ->
            name.ifEmpty { defaultName(context) }
        }
    }

fun String.toCustomLayout(): CustomLayout {
    return CustomLayout(Uri.parse(this))
}

object LayoutSerDe : PreferenceSerDe<Layout<*>> {
    override fun serialize(editor: SharedPreferences.Editor, key: String, value: Layout<*>) {
        val encodedValue = when (value) {
            is EmbeddedLayout -> "e${value.path}"
            is CustomLayout -> "c${value.path}"
            else -> null
        }

        editor.putString(key, encodedValue)
    }

    override fun deserialize(
        sharedPreferences: SharedPreferences,
        key: String,
        default: Layout<*>
    ): Layout<*> {
        return tryOrNull { sharedPreferences.getString(key, null) }
            ?.let { deserialize(it) } ?: default
    }

    override fun deserialize(value: Any?): Layout<*>? {
        return value?.toString()?.let {
            if (it.length >= 2) {
                val path = it.substring(1)
                when (it[0]) {
                    'e' -> EmbeddedLayout(path)
                    'c' -> CustomLayout(Uri.parse(path))
                    else -> null
                }
            } else {
                null
            }
        }
    }
}

data class EmbeddedLayout(override val path: String) : Layout<String> {
    @SuppressLint("DiscouragedApi")
    override fun inputStream(context: Context): Either<LayoutError, InputStream> {
        val resources = context.resources
        val resourceId =
            resources.getIdentifier(path, "raw", context.packageName)
        return catch({
            resources.openRawResource(resourceId).right()
        }) { e: Throwable -> ExceptionWrapperError(e).left() }
    }

    override fun md5(context: Context): Option<String> = path.some()
    override fun defaultName(context: Context): String {
        val locale = Locale(path)
        return Locale.forLanguageTag(path).getDisplayName(locale)
            .replaceFirstChar { it.titlecase(locale) }
    }
}

data class CustomLayout(override val path: Uri) : Layout<Uri> {
    @SuppressLint("Recycle")
    override fun inputStream(context: Context): Either<LayoutError, InputStream> = catch({
        context.contentResolver.openInputStream(path)!!.right()
    }) { e: Throwable -> ExceptionWrapperError(e).left() }

    override fun md5(context: Context): Option<String> = try {
        inputStream(context)
            .getOrNone()
            .map { stream -> stream.use { DigestUtils.md5Hex(it) } }
    } catch (_: Throwable) {
        none()
    }

    override fun defaultName(context: Context): String = Option.catch {
        Option.fromNullable(path.scheme)
            .flatMap {
                when (it) {
                    "file" -> Option.fromNullable(path.lastPathSegment)
                    "content" -> Option.fromNullable(
                        context.contentResolver.query(
                            path,
                            null,
                            null,
                            null,
                            null
                        )?.let { cursor ->
                            var result = ""
                            if (cursor.count != 0) {
                                val columnIndex =
                                    cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                                cursor.moveToFirst()
                                result = cursor.getString(columnIndex)
                            }
                            cursor.close()
                            result
                        }
                    )

                    else -> none()
                }
            }
    }.flatten().getOrElse { "" }
}
