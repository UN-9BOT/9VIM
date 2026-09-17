package inc.flide.vim8.ime.layout

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import arrow.core.None
import arrow.core.left
import arrow.core.right
import arrow.core.some
import inc.flide.vim8.arbitraries.Arbitraries
import inc.flide.vim8.cache
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.ime.layout.models.info
import inc.flide.vim8.ime.layout.models.yaml.versions.common.name
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.property.arbitrary.next
import io.mockk.clearMocks
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.apache.commons.codec.digest.DigestUtils

private class TrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
    var closed = false

    override fun close() {
        closed = true
        super.close()
    }
}

class LayoutSpec : FunSpec({
    lateinit var context: Context
    lateinit var androidResources: Resources
    lateinit var androidContentResolver: ContentResolver
    lateinit var cache: Cache
    lateinit var inputStream: InputStream
    val layoutLoader = mockk<LayoutLoader>()

    beforeSpec {
        mockkStatic(DigestUtils::class)
        mockkStatic(Context::cache)
        context = mockk(relaxed = true) {
            every { cache() } answers { lazy { cache } }
            every { resources } answers { androidResources }
            every { contentResolver } answers { androidContentResolver }
            every { packageName } returns ""
        }
        every { DigestUtils.md5Hex(any<InputStream>()) } returns ""
        every { DigestUtils.md5Hex(any<ByteArray>()) } returns ""
    }

    beforeTest {
        androidResources = mockk()
        androidContentResolver = mockk()
        cache = mockk(relaxed = true) {
            every { load(any()) } returns None
        }
        inputStream = ByteArrayInputStream(byteArrayOf())
    }

    context("Embedded layout") {

        beforeTest {
            every { androidResources.getIdentifier(any(), any(), any()) } returns 0
        }

        context("loading InputStream") {
            test("the resource is found") {
                every { androidResources.openRawResource(any()) } returns inputStream
                EmbeddedLayout("test").inputStream(context) shouldBeRight inputStream
            }

            test("the resource is not found") {
                val exception = Exception("resource not found")
                every { androidResources.openRawResource(any()) } throws exception
                EmbeddedLayout("test").inputStream(context) shouldBeLeft ExceptionWrapperError(
                    exception
                )
            }
        }

        test("loadKeyboardData") {
            val layout = spyk(EmbeddedLayout("en"))
            every { layout.inputStream(any()) } returns inputStream.right()
            val keyboardData = Arbitraries.arbKeyboardData.next()
            every {
                layoutLoader.loadKeyboardData(any())
            } returns keyboardData.right()
            layout.loadKeyboardData(layoutLoader, context) shouldBeRight KeyboardData.info.name.set(
                keyboardData,
                "English"
            )
        }
    }

    context("Custom layout") {
        val uri = mockk<Uri>()
        afterTest { clearMocks(uri) }
        context("loading InputStream") {
            test("the resource is found") {
                every { androidContentResolver.openInputStream(any()) } returns inputStream
                CustomLayout(uri).inputStream(context) shouldBeRight inputStream
            }

            test("the resource is not found") {
                val exception = Exception("resource not found")
                every { androidContentResolver.openInputStream(any()) } throws exception
                CustomLayout(uri).inputStream(context) shouldBeLeft ExceptionWrapperError(
                    exception
                )
            }

            test("md5 closes the provider stream") {
                val stream = TrackingInputStream("content".toByteArray())
                every { androidContentResolver.openInputStream(any()) } returns stream

                CustomLayout(uri).md5(context).getOrNull() shouldBe ""
                stream.closed shouldBe true
            }
        }

        context("loadKeyboardData") {
            val layout = spyk(CustomLayout(uri))
            every { layout.inputStream(any()) } returns inputStream.right()
            val keyboardData = Arbitraries.arbKeyboardData.next()
            every { layoutLoader.loadKeyboardData(any()) } returns keyboardData.right()

            test("scheme is a file") {
                every { uri.scheme } returns "file"
                every { uri.lastPathSegment } returns "file.yaml"
                layout.loadKeyboardData(
                    layoutLoader,
                    context
                ) shouldBeRight KeyboardData.info.name.set(
                    keyboardData,
                    "file.yaml"
                )
            }

            test("scheme is a content") {
                every { uri.scheme } returns "content"
                val cursor = mockk<Cursor>(relaxed = true)
                every {
                    androidContentResolver.query(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                    )
                } returns cursor
                every { cursor.count } returns 1
                every { cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME) } returns 0
                every { cursor.getString(any()) } returns "content.yaml"
                every { layoutLoader.loadKeyboardData(any()) } returns keyboardData.right()
                layout.loadKeyboardData(
                    layoutLoader,
                    context
                ) shouldBeRight KeyboardData.info.name.set(
                    keyboardData,
                    "content.yaml"
                )
            }
            test("reads one closed snapshot for digest and parser") {
                val uri = Uri.parse("content://layouts/snapshot")
                val first = TrackingInputStream("first".toByteArray())
                val second = TrackingInputStream("second".toByteArray())
                val layout = spyk(CustomLayout(uri))
                val keyboardData = KeyboardData(characterSets = listOf(listOf(null)))
                var parsed = ""

                every { layout.inputStream(any()) } returnsMany listOf(
                    first.right(),
                    second.right()
                )
                every { DigestUtils.md5Hex(any<InputStream>()) } answers {
                    firstArg<InputStream>().readBytes().decodeToString()
                }
                every { DigestUtils.md5Hex(any<ByteArray>()) } returns "snapshot"
                every { cache.load(any()) } returns None
                every { layoutLoader.loadKeyboardData(any()) } answers {
                    parsed = firstArg<InputStream>().readBytes().decodeToString()
                    keyboardData.right()
                }

                layout.loadKeyboardData(layoutLoader, context) shouldBeRight keyboardData

                parsed shouldBe "first"
                first.closed shouldBe true
                second.closed shouldBe false
            }

            test("derives URI metadata after reading raw cached data") {
                val first = spyk(CustomLayout(Uri.parse("file:///first.yaml")))
                val second = spyk(CustomLayout(Uri.parse("file:///second.yaml")))
                val keyboardData = KeyboardData(characterSets = listOf(listOf(null)))
                val cached = mutableMapOf<String, KeyboardData>()

                every { first.inputStream(any()) } returns
                    TrackingInputStream("same".toByteArray()).right()
                every { second.inputStream(any()) } returns
                    TrackingInputStream("same".toByteArray()).right()
                every { DigestUtils.md5Hex(any<ByteArray>()) } returns "same-md5"
                every { cache.load(any()) } answers {
                    cached[firstArg()]?.some() ?: None
                }
                every { cache.add(any(), any()) } answers {
                    cached[firstArg()] = secondArg()
                }
                every { layoutLoader.loadKeyboardData(any()) } returns keyboardData.right()

                first.loadKeyboardData(layoutLoader, context) shouldBeRight KeyboardData.info.name.set(
                    keyboardData,
                    "first.yaml"
                )
                second.loadKeyboardData(layoutLoader, context) shouldBeRight KeyboardData.info.name.set(
                    keyboardData,
                    "second.yaml"
                )
            }

            test("reparses changed bytes for the same URI") {
                val layout = spyk(CustomLayout(Uri.parse("content://layouts/changed")))
                val first = TrackingInputStream("first".toByteArray())
                val second = TrackingInputStream("second".toByteArray())
                val keyboardData = KeyboardData(characterSets = listOf(listOf(null)))
                val parsed = mutableListOf<String>()

                every { layout.inputStream(any()) } returnsMany listOf(
                    first.right(),
                    second.right()
                )
                every { DigestUtils.md5Hex(any<ByteArray>()) } returnsMany listOf(
                    "first-md5",
                    "second-md5"
                )
                every { cache.load(any()) } returns None
                every { cache.add(any(), any()) } answers {
                    /* The cache is not needed to prove digest invalidation. */
                }
                every { layoutLoader.loadKeyboardData(any()) } answers {
                    parsed += firstArg<InputStream>().readBytes().decodeToString()
                    keyboardData.right()
                }

                layout.loadKeyboardData(layoutLoader, context) shouldBeRight keyboardData
                layout.loadKeyboardData(layoutLoader, context) shouldBeRight keyboardData

                parsed shouldBe listOf("first", "second")
                first.closed shouldBe true
                second.closed shouldBe true
            }

            test("closes the snapshot when the parser returns a typed error") {
                val uri = Uri.parse("content://layouts/parser-failure")
                val stream = TrackingInputStream("invalid".toByteArray())
                val layout = spyk(CustomLayout(uri))
                val error = ExceptionWrapperError(Exception("parser failure"))

                every { layout.inputStream(any()) } returns stream.right()
                every { DigestUtils.md5Hex(any<ByteArray>()) } returns "invalid"
                every { cache.load(any()) } returns None
                every { layoutLoader.loadKeyboardData(any()) } returns error.left()

                layout.loadKeyboardData(layoutLoader, context) shouldBeLeft error
                stream.closed shouldBe true
            }
        }
    }

    afterSpec {
        clearStaticMockk(DigestUtils::class, Context::class)
    }
})
