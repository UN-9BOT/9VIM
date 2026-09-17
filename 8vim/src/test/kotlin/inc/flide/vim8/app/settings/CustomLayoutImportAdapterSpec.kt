package inc.flide.vim8.app.settings

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import arrow.core.left
import arrow.core.right
import inc.flide.vim8.ime.layout.AvailableLayouts
import inc.flide.vim8.ime.layout.CustomLayout
import inc.flide.vim8.ime.layout.EmbeddedLayout
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class CustomLayoutImportAdapterSpec : FunSpec({
    lateinit var contentResolver: ContentResolver
    lateinit var availableLayouts: AvailableLayouts
    lateinit var uri: Uri

    beforeTest {
        contentResolver = mockk()
        availableLayouts = mockk()
        uri = mockk()
    }

    test("uses the wildcard MIME contract for OpenDocument") {
        CustomLayoutImportAdapter.OPEN_DOCUMENT_MIME_TYPE shouldBe "*/*"
    }

    test("takes only a persistable read grant before delegating one import") {
        justRun {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val imported = EmbeddedLayout("en")
        every { availableLayouts.importLayout(CustomLayout(uri)) } returns imported.right()

        val result = CustomLayoutImportAdapter(contentResolver, availableLayouts).importLayout(uri)

        result shouldBe CustomLayoutImportResult.Success(imported)
        verify(exactly = 1) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        verify(exactly = 1) { availableLayouts.importLayout(CustomLayout(uri)) }
    }

    test("returns the domain error without a second state mutation") {
        justRun {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val error = ExceptionWrapperError(IllegalArgumentException("invalid layout"))
        every { availableLayouts.importLayout(CustomLayout(uri)) } returns error.left()

        val result = CustomLayoutImportAdapter(contentResolver, availableLayouts).importLayout(uri)

        result shouldBe CustomLayoutImportResult.Failure(error)
        verify(exactly = 1) { availableLayouts.importLayout(CustomLayout(uri)) }
    }

    test("returns a permission failure without delegating or mutating layout state") {
        val exception = SecurityException("provider denied access")
        every {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } throws exception

        val result = CustomLayoutImportAdapter(contentResolver, availableLayouts).importLayout(uri)

        result shouldBe CustomLayoutImportResult.Failure(ExceptionWrapperError(exception))
        verify(exactly = 0) { availableLayouts.importLayout(any()) }
    }
})
