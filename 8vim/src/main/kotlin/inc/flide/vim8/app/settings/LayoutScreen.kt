package inc.flide.vim8.app.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import inc.flide.vim8.R
import inc.flide.vim8.app.availableLayouts
import inc.flide.vim8.datastore.ui.Preference
import inc.flide.vim8.datastore.ui.PreferenceGroup
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.lib.compose.Dialog
import inc.flide.vim8.lib.compose.Screen
import inc.flide.vim8.lib.compose.stringRes
import inc.flide.vim8.lib.util.DialogsHelper.showAlert

@Composable
fun LayoutScreen() = Screen {
    title = stringRes(R.string.settings__layouts__title)
    previewFieldVisible = true

    val fileSelector = fileSelector()

    content {
        PreferenceGroup {
            Dialog {
                title = stringRes(R.string.select_preferred_keyboard_layout_dialog_title)
                index = { availableLayouts.get()?.index ?: 0 }
                items = { availableLayouts.get()?.displayNames ?: emptyList() }
                onConfirm { availableLayouts.get()?.selectLayout(it) }
                Preference(
                    title = stringRes(R.string.settings__layouts__select__title),
                    summary = stringRes(R.string.settings__layouts__select__summary),
                    onClick = { show() },
                    trailing = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                )
            }
            Preference(
                title = stringRes(R.string.settings__layouts__load_custom__title),
                summary = stringRes(R.string.settings__layouts__load_custom__summary),
                onClick = { fileSelector() },
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun fileSelector(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) {
        if (it == null) {
            return@rememberLauncherForActivityResult
        }
        val layouts = availableLayouts.get() ?: return@rememberLauncherForActivityResult
        when (
            val result = CustomLayoutImportAdapter(
                context.contentResolver,
                layouts
            ).importLayout(it)
        ) {
            is CustomLayoutImportResult.Success -> Unit
            is CustomLayoutImportResult.Failure -> {
                val title = if (result.error is ExceptionWrapperError) {
                    R.string.dialog__error__title
                } else {
                    R.string.dialog__yaml__error__title
                }
                showAlert(context, title, result.error.message)
            }
        }
    }
    return { launcher.launch(arrayOf(CustomLayoutImportAdapter.OPEN_DOCUMENT_MIME_TYPE)) }
}
