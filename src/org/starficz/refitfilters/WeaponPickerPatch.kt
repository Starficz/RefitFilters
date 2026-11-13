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
 * Refit Filters inside vanilla's weapon picker, at the moments vanilla builds it (no polling).
 *
 * The picker has one private build method. It is run by the constructor and by `notifyFilterChanged()`; it clears the
 * inner panel, builds vanilla's filter bar, filters the available weapons (a private `List -> List` method), sorts
 * them, adds up its own height (including `filterBar.getHeight() + 1`), adds one item per weapon and sizes itself.
 * So: our filters go into vanilla's filter step, the search ranking goes after vanilla's sort, our panels' height
 * goes into vanilla's height sum, and the panels are added after each build. Vanilla then lays out, sizes, borders
 * and labels ("No weapons matching filter") exactly what is shown.
 *
 * Targets are the readable class name and readable method names plus two shape matches; all inferred (not API), so
 * re-check them after a game update. Seen on 0.98.5a-RC4 Windows: build method `interface()`, filter method `new(List)`,
 * filter bar class `com.fs.starfarer.coreui.refit.while` (these obfuscated names change between builds).
 */
@Patch(target = ClassMatch(typeName = "com.fs.starfarer.coreui.refit.WeaponPickerDialog"))
object WeaponPickerPatch {
    private val GET_INNER_PANEL = MethodQuery.create().methodName("getInnerPanel").parameterCount(0).build()
    private val SET_CHECKED = MethodQuery.create().methodName("setChecked").parameterCount(2).build()

    private fun injectPanels(context: AfterContext) {
        val picker = context.self as? UIPanelAPI ?: return
        val innerPanel = context.getMethod(GET_INNER_PANEL).call() as? UIPanelAPI ?: return
        FilterPanelCreator.injectFilters(innerPanel, picker, PickerPanelType.Weapons)
    }

    @JvmStatic
    @After(target = MethodMatch(methodType = MethodType.CONSTRUCTOR))
    fun afterConstructed(context: AfterContext) = injectPanels(context)

    @JvmStatic
    @After(target = MethodMatch(methodName = "notifyFilterChanged", parameterCount = 0, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE))
    fun afterFilterChanged(context: AfterContext) = injectPanels(context)

    /** Vanilla's filter step (the only instance method taking and returning a List); ours runs on what it kept. */
    // 0.98.5a-RC4 Windows: private List new(List)
    @JvmStatic
    @After(target = MethodMatch(parameterTypes = [java.util.List::class], returnType = java.util.List::class, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE))
    fun afterVanillaFilter(context: AfterContext) {
        val items = context.returnValue as? List<*> ?: return
        val data = FilterPanelCreator.weaponFilterData
        // an ArrayList: vanilla removes entries from it afterwards
        val kept = items.filterTo(ArrayList()) { item ->
            val spec = (item as? AutofitPlugin.AvailableWeapon)?.spec
            spec == null || !FilterPanelCreator.weaponFiltered(spec, data)
        }
        if (kept.size != items.size) context.returnValue = kept
    }

    /**
     * Vanilla sorts after filtering, so the sort row's order and the search ranking go after its sort (both stable:
     * the order before them breaks ties).
     * Host: the class's no-arg void methods; only the build method calls Collections.sort.
     */
    // 0.98.5a-RC4 Windows: host is private void interface()
    @JvmStatic
    @RedirectMethodCall(
        target = MethodMatch(parameterCount = 0, returnTypeName = "void", methodType = MethodType.METHOD, staticType = StaticType.INSTANCE),
        call = MethodMatch(methodName = "sort", parameterCount = 2),
        owner = ClassMatch(type = Collections::class)
    )
    fun afterVanillaSort(context: MethodCallContext) {
        context.call()
        context.setResult(null)
        @Suppress("UNCHECKED_CAST")
        val items = context.getCallArg(0) as? MutableList<Any?> ?: return
        val data = FilterPanelCreator.weaponFilterData
        // the sort row's order (none when the row is off: vanilla's stays), then the search ranking on top of it
        if (RFSettings.sortRowEnabled) WeaponStats.sort(items, data.sortKey.value, data.sortDescending.value)
        val search = data.currentSearch.value
        if (RFSettings.searchBarBehaviour == "Filter" || search.isBlank()) return
        items.sortWith(compareByDescending { item ->
            (item as? AutofitPlugin.AvailableWeapon)?.spec?.let { FilterPanelCreator.weaponSearchScore(search, it) } ?: 0
        })
    }

    /**
     * Vanilla adds `filterBar.getHeight() + 1` to the picker's height; the filter bar is the receiver with a
     * two-argument setChecked. Adding our panels there makes vanilla's size, border and tooltip placement include
     * them. Other getHeight() calls in the host methods pass through unchanged.
     */
    @JvmStatic
    @RedirectMethodCall(
        target = MethodMatch(parameterCount = 0, returnTypeName = "void", methodType = MethodType.METHOD, staticType = StaticType.INSTANCE),
        call = MethodMatch(methodName = "getHeight", parameterCount = 0),
        owner = ClassMatch(subtype = UIComponentAPI::class)
    )
    fun filterBarHeight(context: MethodCallContext) {
        val height = context.call() as Float
        val receiver = context.callReceiver
        val isFilterBar = receiver is UIPanelAPI && receiver !is LabelAPI && context.hasMethod(SET_CHECKED, receiver)
        context.setResult(if (isFilterBar) height + FilterPanelCreator.injectedExtraHeight(PickerPanelType.Weapons) else height)
    }

    /** Keeps the picker's top where it was while its height changes with the filters. */
    @JvmStatic
    @After(target = MethodMatch(methodName = "updateYPosition", parameterCount = 0, methodType = MethodType.METHOD, staticType = StaticType.INSTANCE))
    fun afterUpdateYPosition(context: AfterContext) {
        val picker = context.self as? UIPanelAPI ?: return
        PickerPanelHelpers.pinPickerPanelYPos(picker)
    }
}
