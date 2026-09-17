package inc.flide.vim8.app.settings

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import arrow.core.Either
import inc.flide.vim8.ime.layout.AvailableLayouts
import inc.flide.vim8.ime.layout.CustomLayout
import inc.flide.vim8.ime.layout.Layout
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.ime.layout.models.error.LayoutError

sealed interface CustomLayoutImportResult {
    data class Success(val layout: Layout<*>) : CustomLayoutImportResult
    data class Failure(val error: LayoutError) : CustomLayoutImportResult
}

/**
 * Android SAF boundary for importing a custom layout.
 *
 * Provider access and permission errors are kept at this boundary. Layout
 * validation and all preference/registry mutations remain in AvailableLayouts.
 */
class CustomLayoutImportAdapter(
    private val contentResolver: ContentResolver,
    private val availableLayouts: AvailableLayouts
) {
    companion object {
        const val OPEN_DOCUMENT_MIME_TYPE = "*/*"
    }

    fun importLayout(uri: Uri): CustomLayoutImportResult {
        return runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            availableLayouts.importLayout(CustomLayout(uri))
        }.fold(
            onSuccess = { result: Either<LayoutError, Layout<*>> ->
                result.fold(
                    { error -> CustomLayoutImportResult.Failure(error) },
                    { layout -> CustomLayoutImportResult.Success(layout) }
                )
            },
            onFailure = { exception ->
                CustomLayoutImportResult.Failure(ExceptionWrapperError(exception))
            }
        )
    }
}
