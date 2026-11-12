package org.starficz.refitfilters

import com.fs.starfarer.api.plugins.AutofitPlugin
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import patchlib.api.context.AfterContext
import patchlib.api.context.MethodCallContext
import patchlib.api.match.ClassMatch
import patchlib.api.match.MethodMatch
import patchlib.api.match.MethodType
import patchlib.api.match.StaticType
import patchlib.api.patch.After
import patchlib.api.patch.Patch
import patchlib.api.patch.RedirectMethodCall
import patchlib.api.query.MethodQuery
import java.util.Collections

/**
 * Refit Filters inside vanilla's fighter picker; the same approach as [WeaponPickerPatch]. Here the build method has a
 * readable name, `updateUI()` (run by the constructor and by `notifyFilterChanged()`), so the hooks and redirect hosts
 * name it directly.
 *
 * Targets are the readable class and method names plus one shape match; all inferred (not API), so re-check them
 * after a game update. Seen on 0.98.5a-RC4 Windows: filter method `o00000(List)`, filter bar class
 * `com.fs.starfarer.coreui.refit.while` (obfuscated names change between builds).
 */
@Patch(target = ClassMatch(typeName = "com.fs.starfarer.coreui.refit.FighterPickerDialog"))
object FighterPickerPatch {
    private val GET_INNER_PANEL = MethodQuery.create().methodName("getInnerPanel").parameterCount(0).build()
    private val SET_CHECKED = MethodQuery.create().methodName("setChecked").parameterCount(2).build()

    @JvmStatic
    @After(target = MethodMatch(methodName = "updateUI", parameterCount = 0, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE))
    fun afterBuild(context: AfterContext) {
        val picker = context.self as? UIPanelAPI ?: return
        val innerPanel = context.getMethod(GET_INNER_PANEL).call() as? UIPanelAPI ?: return
        FilterPanelCreator.injectFilters(innerPanel, picker, PickerPanelType.Fighters)
        // Vanilla trims the weapon picker's width by 3 px when a weapon is mounted but never trims the fighter
        // picker, which leaves a wider margin left of the rows; Refit Filters has always narrowed it by 5 px.
        // Runs after every build, and the build sets the width afresh, so it never accumulates.
        picker.position.width -= 5f
    }

    /** Vanilla's filter step (the only instance method taking and returning a List); ours runs on what it kept. */
    // 0.98.5a-RC4 Windows: private List o00000(List)
    @JvmStatic
    @After(target = MethodMatch(parameterTypes = [java.util.List::class], returnType = java.util.List::class, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE))
    fun afterVanillaFilter(context: AfterContext) {
        val items = context.returnValue as? List<*> ?: return
        val data = FilterPanelCreator.fighterFilterData
        val kept = items.filterTo(ArrayList()) { item ->
            val spec = (item as? AutofitPlugin.AvailableFighter)?.wingSpec
            spec == null || !FilterPanelCreator.fighterFiltered(spec, data)
        }
        if (kept.size != items.size) context.returnValue = kept
    }

    /** Search ranking after vanilla's sort (stable: vanilla's order breaks ties). */
    @JvmStatic
    @RedirectMethodCall(
        target = MethodMatch(methodName = "updateUI", parameterCount = 0, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE),
        call = MethodMatch(methodName = "sort", parameterCount = 2),
        owner = ClassMatch(type = Collections::class)
    )
    fun afterVanillaSort(context: MethodCallContext) {
        context.call()
        context.setResult(null)
        val search = FilterPanelCreator.fighterFilterData.currentSearch.value
        if (RFSettings.searchBarBehaviour == "Filter" || search.isBlank()) return
        @Suppress("UNCHECKED_CAST")
        val items = context.getCallArg(0) as? MutableList<Any?> ?: return
        items.sortWith(compareByDescending { item ->
            (item as? AutofitPlugin.AvailableFighter)?.wingSpec?.let { FilterPanelCreator.fighterSearchScore(search, it) } ?: 0
        })
    }

    /** Our panels' height goes into vanilla's `filterBar.getHeight() + 1` (see [WeaponPickerPatch.filterBarHeight]). */
    @JvmStatic
    @RedirectMethodCall(
        target = MethodMatch(methodName = "updateUI", parameterCount = 0, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE),
        call = MethodMatch(methodName = "getHeight", parameterCount = 0),
        owner = ClassMatch(subtype = UIComponentAPI::class)
    )
    fun filterBarHeight(context: MethodCallContext) {
        val height = context.call() as Float
        val receiver = context.callReceiver
        val isFilterBar = receiver is UIPanelAPI && receiver !is LabelAPI && context.hasMethod(SET_CHECKED, receiver)
        context.setResult(if (isFilterBar) height + FilterPanelCreator.injectedExtraHeight(PickerPanelType.Fighters) else height)
    }

    /** Keeps the picker's top where it was while its height changes with the filters. */
    @JvmStatic
    @After(target = MethodMatch(methodName = "updateYPosition", parameterCount = 0, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE))
    fun afterUpdateYPosition(context: AfterContext) {
        val picker = context.self as? UIPanelAPI ?: return
        PickerPanelHelpers.pinPickerPanelYPos(picker)
    }
}
