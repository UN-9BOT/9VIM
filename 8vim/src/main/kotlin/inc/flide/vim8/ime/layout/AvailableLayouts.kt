package inc.flide.vim8.ime.layout

import android.content.Context
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrNone
import arrow.core.left
import arrow.core.right
import inc.flide.vim8.AppPrefs
import inc.flide.vim8.appPreferenceModel
import inc.flide.vim8.ime.layout.models.KeyboardData
import inc.flide.vim8.ime.layout.models.error.ExceptionWrapperError
import inc.flide.vim8.ime.layout.models.error.LayoutError

class AvailableLayouts(private val layoutLoader: LayoutLoader, private val context: Context) {
    private val prefs: AppPrefs by appPreferenceModel()
    private val defaultIndex: Int
    private val embeddedLayoutsSize: Int
    private val layoutsWithKeyboardData: MutableMap<Layout<*>, String> = linkedMapOf()

    val displayNames: List<String>
        get() = layoutsWithKeyboardData.values.toList()
    var index = -1
        private set

    init {
        val embeddedLayoutsWithName = embeddedLayouts(layoutLoader, context)
        embeddedLayoutsSize = embeddedLayoutsWithName.size
        layoutsWithKeyboardData.putAll(embeddedLayoutsWithName)
        defaultIndex =
            layoutsWithKeyboardData.keys.indexOf(prefs.layout.current.default as EmbeddedLayout)
        reloadCustomLayouts()

        prefs.layout.custom.history.observe {
            if (it.size > layoutsWithKeyboardData.size - embeddedLayoutsSize) {
                reloadCustomLayouts()
            }
        }
    }

    private fun sameIdentity(left: Layout<*>, right: Layout<*>): Boolean = when {
        left is EmbeddedLayout && right is EmbeddedLayout -> left.path == right.path
        left is CustomLayout && right is CustomLayout ->
            left.path.toString() == right.path.toString()

        else -> false
    }

    private fun isCustomPath(layout: Layout<*>, path: String): Boolean =
        layout is CustomLayout && layout.path.toString() == path

    private fun hasLayout(layout: Layout<*>): Boolean =
        layoutsWithKeyboardData.keys.any { sameIdentity(it, layout) }

    private fun removeCustomLayout(path: String) {
        layoutsWithKeyboardData
            .keys
            .filter { isCustomPath(it, path) }
            .toList()
            .forEach { layoutsWithKeyboardData.remove(it) }

        val historyPref = prefs.layout.custom.history
        val history = LinkedHashSet(historyPref.get())
        if (history.remove(path)) {
            historyPref.set(history)
        }

        if (isCustomPath(prefs.layout.previousValid.get(), path)) {
            prefs.layout.previousValid.set(prefs.layout.previousValid.default)
        }
    }

    private fun removeStaleLayout(path: String) {
        val current = prefs.layout.current.get()
        removeCustomLayout(path)
        if (isCustomPath(current, path)) {
            restorePreviousValidOrDefault()
        }
        findIndex()
    }

    private fun rememberPreviousValid(current: Layout<*>, next: Layout<*>) {
        if (!sameIdentity(current, next) && hasLayout(current)) {
            prefs.layout.previousValid.set(current)
        }
    }

    private fun updateHistory(path: String) {
        val history = LinkedHashSet(prefs.layout.custom.history.get())
        history.remove(path)
        prefs.layout.custom.history.set(LinkedHashSet<String>().apply {
            add(path)
            addAll(history)
        })
    }

    private fun upsert(layout: Layout<*>, keyboardData: KeyboardData) {
        layoutsWithKeyboardData
            .keys
            .filter { sameIdentity(it, layout) }
            .toList()
            .forEach { layoutsWithKeyboardData.remove(it) }
        layoutsWithKeyboardData[layout] = keyboardData.toString()
    }

    private fun emptyLayoutError(): LayoutError = ExceptionWrapperError(
        IllegalArgumentException("The layout requires at least one layer")
    )

    private fun restorePreviousValidOrDefault() {
        val previous = prefs.layout.previousValid.get()
        val restored = if (hasLayout(previous)) {
            previous
                .loadKeyboardData(layoutLoader, context)
                .getOrNone()
                .filterNot { it.totalLayers == 0 }
        } else {
            arrow.core.None
        }

        restored
            .onSome { keyboardData ->
                upsert(previous, keyboardData)
                prefs.layout.current.set(previous)
            }
            .onNone {
                if (previous is CustomLayout) {
                    removeCustomLayout(previous.path.toString())
                }
                prefs.layout.current.set(prefs.layout.current.default)
            }
    }

    fun reloadCustomLayouts() {
        listCustomLayoutHistory()
        findIndex()
    }

    /**
     * Validates and imports a custom URI as one state transition.
     *
     * URI strings are the durable identity; content digests are only used by
     * [Layout.loadKeyboardData] as a cache key.
     */
    fun importLayout(layout: CustomLayout): Either<LayoutError, Layout<*>> {
        val path = layout.path.toString()
        val knownLayout = prefs.layout.custom.history.get().contains(path) || hasLayout(layout)
        return layout.loadKeyboardData(layoutLoader, context).flatMap { keyboardData ->
            if (keyboardData.totalLayers == 0) {
                emptyLayoutError().left()
            } else {
                val current = prefs.layout.current.get()
                upsert(layout, keyboardData)
                updateHistory(path)
                rememberPreviousValid(current, layout)
                prefs.layout.current.set(layout)
                findIndex()
                layout.right()
            }
        }.onLeft {
            if (knownLayout) {
                removeStaleLayout(path)
            }
        }
    }

    /**
     * Compatibility wrapper for the old picker call site. New integrations
     * should consume [importLayout] so they can display the typed failure.
     */
    fun updateKeyboardData(layout: Layout<*>): Boolean {
        return if (layout is CustomLayout) {
            importLayout(layout).fold({ false }, { true })
        } else {
            layout.loadKeyboardData(layoutLoader, context)
                .flatMap { keyboardData ->
                    if (keyboardData.totalLayers == 0) {
                        emptyLayoutError().left()
                    } else {
                        val current = prefs.layout.current.get()
                        upsert(layout, keyboardData)
                        rememberPreviousValid(current, layout)
                        prefs.layout.current.set(layout)
                        findIndex()
                        layout.right()
                    }
                }
                .fold({ false }, { true })
        }
    }

    fun selectLayout(which: Int) {
        val layout = layoutsWithKeyboardData.keys.elementAtOrNull(which) ?: return
        layout.loadKeyboardData(layoutLoader, context)
            .flatMap { keyboardData ->
                if (keyboardData.totalLayers == 0) {
                    emptyLayoutError().left()
                } else {
                    val current = prefs.layout.current.get()
                    upsert(layout, keyboardData)
                    rememberPreviousValid(current, layout)
                    prefs.layout.current.set(layout)
                    layout.right()
                }
            }
            .onLeft {
                if (layout is CustomLayout) {
                    removeStaleLayout(layout.path.toString())
                }
            }
            .onRight { findIndex() }
    }

    private fun listCustomLayoutHistory() {
        val uris = LinkedHashSet(prefs.layout.custom.history.get())
        val current = prefs.layout.current.get()
        val customLayouts = layoutsWithKeyboardData.keys
            .filterIsInstance<CustomLayout>()
            .toList()
        customLayouts
            .filter { customLayout -> !uris.contains(customLayout.path.toString()) }
            .forEach { layoutsWithKeyboardData.remove(it) }

        val loaded = uris.mapNotNull { path ->
            val layout = path.toCustomLayout()
            layout.loadKeyboardData(layoutLoader, context)
                .getOrNone()
                .filterNot { it.totalLayers == 0 }
                .map { layout to it }
                .getOrNull()
        }
        val validUris = loaded.map { it.first.path.toString() }.toSet()
        val staleUris = uris - validUris

        layoutsWithKeyboardData.keys
            .filterIsInstance<CustomLayout>()
            .toList()
            .forEach { layoutsWithKeyboardData.remove(it) }
        loaded.forEach { (layout, keyboardData) -> upsert(layout, keyboardData) }
        prefs.layout.custom.history.set(
            LinkedHashSet(uris.filter { validUris.contains(it) })
        )

        staleUris.forEach { path ->
            if (isCustomPath(current, path)) {
                removeStaleLayout(path)
            } else {
                removeCustomLayout(path)
            }
        }
        if (current is CustomLayout && !validUris.contains(current.path.toString())) {
            removeStaleLayout(current.path.toString())
        }
    }

    private fun findIndex() {
        val current = prefs.layout.current.get()
        index = layoutsWithKeyboardData.keys.indexOfFirst { sameIdentity(it, current) }
        if (index == -1) {
            index = defaultIndex
            val defaultLayout = prefs.layout.current.default
            if (!sameIdentity(current, defaultLayout)) {
                prefs.layout.current.set(defaultLayout)
            }
        }
    }
}
