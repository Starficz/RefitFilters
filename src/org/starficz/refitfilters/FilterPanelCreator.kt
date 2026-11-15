package org.starficz.refitfilters

import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.filterpanels.*
import org.starficz.refitfilters.ReflectionUtils.getMethodsMatching
import org.starficz.starui.Anchor
import org.starficz.starui.AreaCheckboxTheme
import org.starficz.starui.components.Theme
import org.starficz.starui.inject
import org.starficz.starui.injection
import org.starficz.starui.Size
import org.starficz.starui.StarUITheme
import org.starficz.starui.modifier.Modifier
import org.starficz.starui.modifier.anchor
import org.starficz.starui.vanilla.*

enum class PickerPanelType {
    Weapons,
    Fighters
}

object FilterPanelCreator {
    /** The search bar's injection key; it is always made, so it also says "this picker has its panels". */
    private const val SEARCH_KEY = "refitfilters:search"

    /** The picker's filter rows: ours (each injected under [key]) and vanilla's own filter bar. */
    private enum class Row(val key: String) {
        Search(SEARCH_KEY), Vanilla(""), WeaponTypes("refitfilters:weapon-types"),
        DamageRange("refitfilters:damage-range"), Sort("refitfilters:sort")
    }

    val rowWidth = 377f
    val filterRowHeight = 25f

    val weaponFilterData = WeaponFilterData()
    val fighterFilterData = FighterFilterData()


    /** The filter panels' checkboxes: the player faction's colours a step darker, in the small font. */
    private fun filterTheme(): StarUITheme {
        val bright = Global.getSettings().basePlayerColor
        val base = bright.darker()
        return StarUITheme(areaCheckbox = AreaCheckboxTheme(
            baseColor = base, darkColor = base.darker().darker(), brightColor = bright, font = Font.VICTOR_14
        ))
    }

    /** Whether the pickers get the sort row (the setting). Always last, on the list. */
    private fun hasSortRow() = RFSettings.sortRowEnabled

    /** The panels and their order, as the settings ask: vanilla's own filter bar is `null` here. */
    private fun panelOrder(pickerPanelType: PickerPanelType): List<Pair<String?, Int>> = listOfNotNull(
        "search" to RFSettings.ResetButtonSearchBarPanelOrder,
        null to RFSettings.VanillaWeaponAvailabilityWeaponSlotPanelOrder,
        if (pickerPanelType == PickerPanelType.Weapons) "types" to RFSettings.WeaponTypePanelOrder else null,
        "damage" to RFSettings.DamageTypeRangeSliderOrder
    ).filter { (_, order) -> order != 0 }.sortedBy { (_, order) -> order }

    /**
     * How much taller the picker gets from the injected panels, for vanilla's own height sum (see WeaponPickerPatch):
     * every panel row, the 1 px gaps between them (none below vanilla's bar), the sort row with the space above it,
     * and the list's gap below the last panel (6 px, or the sort row's own) where vanilla left 1 px below its bar.
     */
    fun injectedExtraHeight(pickerPanelType: PickerPanelType): Float {
        val order = panelOrder(pickerPanelType)
        val ownPanels = order.count { it.first != null }
        val sortRow = hasSortRow()
        if (ownPanels == 0 && !sortRow) return 0f
        val gaps = order.zipWithNext().count { (prev, _) -> prev.first != null }
        val sort = if (sortRow) SORT_ROW_GAP + SORT_PANEL_HEIGHT else 0f
        val listGap = if (sortRow) SORT_LIST_GAP else 6f
        return ownPanels * filterRowHeight + gaps + sort + listGap - 1f
    }

    /**
     * Adds the filter panels to a picker vanilla has just built (WeaponPickerPatch / FighterPickerPatch hook the
     * build). Vanilla has already filtered, sorted and sized the list with our filters, so only the panels are added
     * and the rows below them moved down.
     */
    fun injectFilters(innerPanel: UIPanelAPI, pickerPanel: UIPanelAPI, pickerPanelType: PickerPanelType) {
        val filterData = when(pickerPanelType){
            PickerPanelType.Weapons -> weaponFilterData
            PickerPanelType.Fighters -> fighterFilterData
        }

        // Once per inner panel: the positions below are not safe to apply twice (the align offsets read the current
        // top). A rebuild that clears the panel drops our panels too, and the hook that called this runs again.
        if (innerPanel.injection(SEARCH_KEY) != null) return
        val uiElements = innerPanel.children

        // vanilla's children, in the order its build adds them:
        // [currently mounted item, its caption,] filter bar, item list[, "No ... matching filter" label]
        val existingFiltersIndex = uiElements.indexOfFirst { it.getMethodsMatching("addItem").isNotEmpty() }
        if (existingFiltersIndex < 0) return

        val currentlySelected = uiElements.getOrNull(existingFiltersIndex-2)
        val currentlyMountedText = uiElements.getOrNull(existingFiltersIndex-1)
        val existingFilters = uiElements[existingFiltersIndex]
        val itemsList = uiElements.getOrNull(existingFiltersIndex+1) ?: return
        val noItems = uiElements.getOrNull(existingFiltersIndex+2)

        // The rows top to bottom (vanilla's filter bar among ours, by the settings), the sort row last. The search bar
        // and vanilla's bar are never off (their settings start at 1).
        val weaponTypes = RFSettings.WeaponTypePanelOrder != 0 && filterData is WeaponFilterData
        val damageRange = RFSettings.DamageTypeRangeSliderOrder != 0
        val rows = listOfNotNull(
            Row.Search to RFSettings.ResetButtonSearchBarPanelOrder,
            Row.Vanilla to RFSettings.VanillaWeaponAvailabilityWeaponSlotPanelOrder,
            if (weaponTypes) Row.WeaponTypes to RFSettings.WeaponTypePanelOrder else null,
            if (damageRange) Row.DamageRange to RFSettings.DamageTypeRangeSliderOrder else null
        ).sortedBy { (_, order) -> order }.map { (row, _) -> row } + listOfNotNull(if (hasSortRow()) Row.Sort else null)

        // Each of our rows is its own injection (a container of the row's exact size) under a key of ours, anchored
        // under the row before it: StarUI applies the anchor on every layout, so the rows stay in place. Vanilla's bar
        // is linked the same way with its own belowLeft, which vanilla also resolves on every layout.
        val theme = filterTheme()
        var previous: UIComponentAPI? = null
        for (row in rows) {
            val pad = when {
                row == Row.Sort -> SORT_ROW_GAP
                previous === existingFilters -> 0f
                else -> 1f
            }
            val prev = previous
            if (row == Row.Vanilla) {
                when {
                    prev != null -> existingFilters.position.belowLeft(prev, pad)
                    currentlyMountedText != null -> existingFilters.position.belowLeft(currentlySelected, 5f)
                    else -> {
                        existingFilters.yAlignOffset = innerPanel.top - existingFilters.top - 2f
                        existingFilters.xAlignOffset = 5f
                    }
                }
                previous = existingFilters
                continue
            }
            val anchor = when {
                prev != null -> Anchor.below.sibling(prev).matchingLeftEdge(pad)
                currentlyMountedText != null && currentlySelected != null ->
                    Anchor.below.sibling(currentlySelected).matchingLeftEdge(5f)
                else -> Anchor.inside.topLeft.ofParent(5f, -2f)
            }
            val height = if (row == Row.Sort) SORT_PANEL_HEIGHT else filterRowHeight
            previous = innerPanel.inject(row.key, Size.exact(rowWidth, height), Modifier.anchor(anchor)) {
                Theme(theme) {
                    when (row) {
                        Row.Search -> createSearchBarFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
                        Row.WeaponTypes -> createWeaponTypesFilterPanel(rowWidth, filterRowHeight, pickerPanel,
                            filterData as WeaponFilterData)
                        Row.DamageRange ->
                            createDamageTypeRangeSliderFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
                        Row.Sort -> createSortPanel(rowWidth, pickerPanel, filterData)
                        Row.Vanilla -> error("vanilla's bar is not injected")
                    }.position.inTL(0f, 0f)
                }
            }
        }
        val last = previous ?: return

        val listGap = if (rows.last() == Row.Sort) SORT_LIST_GAP else 6f
        itemsList.position.belowLeft(last, listGap)
        noItems?.position?.belowLeft(last, listGap)
    }

    /** How well a fighter wing matches the search: its best fuzzy score over name, manufacturer and source mod. */
    fun fighterSearchScore(search: String, spec: FighterWingSpecAPI): Int =
        maxOf(FuzzySearch.fuzzyMatch(search, spec.wingName).second,
            spec.variant?.hullSpec?.manufacturer?.let { FuzzySearch.fuzzyMatch(search, it).second } ?: 0,
            spec.sourceMod?.let { FuzzySearch.fuzzyMatch(search, it.name).second } ?: 0
        )

    /** How well a weapon matches the search: its best fuzzy score over name, manufacturer and source mod. */
    fun weaponSearchScore(search: String, spec: WeaponSpecAPI): Int =
        maxOf(FuzzySearch.fuzzyMatch(search, spec.weaponName).second,
            FuzzySearch.fuzzyMatch(search, spec.manufacturer).second,
            spec.sourceMod?.let { FuzzySearch.fuzzyMatch(search, it.name).second } ?: 0
        )

    fun weaponFiltered(weaponSpec: WeaponSpecAPI, filterData: WeaponFilterData): Boolean{
        with(filterData){
            if (weaponSpec.damageType == DamageType.KINETIC && !kineticDamage.value) return true
            if (weaponSpec.damageType == DamageType.HIGH_EXPLOSIVE && !heDamage.value) return true
            if (weaponSpec.damageType == DamageType.ENERGY && !energyDamage.value) return true
            if (weaponSpec.damageType == DamageType.FRAGMENTATION && !fragDamage.value) return true

            if (!weaponSpec.isBeam && !projectileWeapons.value) return true
            if (weaponSpec.isBeam && !beamWeapons.value) return true

            val weaponIsPD = weaponSpec.aiHints.any { it == WeaponAPI.AIHints.PD || it == WeaponAPI.AIHints.PD_ALSO }

            if (weaponIsPD && !pdWeapons.value) return true
            if (!weaponIsPD && !nonpdWeapons.value) return true

            if (weaponSpec.usesAmmo() && !ammoWeapons.value) return true
            if (!weaponSpec.usesAmmo() && !nonAmmoWeapons.value) return true

            if (weaponSpec.maxRange < lowerRange.value) return true
            if (weaponSpec.maxRange > upperRange.value && upperRange.value.toInt() != RFSettings.weaponMaxRange) return true

            if (WeaponStats.outside(WeaponStats.dps(weaponSpec), dpsLower.value, dpsUpper.value, StatScale.DPS)) return true
            if (WeaponStats.outside(WeaponStats.damagePerShot(weaponSpec), dmgLower.value, dmgUpper.value, StatScale.DMG)) return true
            if (WeaponStats.outside(WeaponStats.efficiency(weaponSpec), effLower.value, effUpper.value, StatScale.EFF)) return true

            if (RFSettings.searchBarBehaviour != "Sort" && currentSearch.value.isNotEmpty()) {
                val searchByDesignType = RFSettings.searchByDesignType

                val matchesName = FuzzySearch.fuzzyMatch(currentSearch.value, weaponSpec.weaponName).second >= 110
                val matchesDesignType = FuzzySearch.fuzzyMatch(currentSearch.value, weaponSpec.manufacturer).second >= 140 ||
                        weaponSpec.sourceMod?.let { FuzzySearch.fuzzyMatch(currentSearch.value, it.name).second >= 140 } ?: false

                if (!matchesName && !searchByDesignType || searchByDesignType && !matchesName && !matchesDesignType) {
                    return true
                }
            }
        }
        return false
    }

    fun fighterFiltered(fighterSpec: FighterWingSpecAPI, filterData: FighterFilterData): Boolean{
        with(filterData){
            if (DamageType.KINETIC in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && !kineticDamage.value) return true
            if (DamageType.HIGH_EXPLOSIVE in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && !heDamage.value) return true
            if (DamageType.ENERGY in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && !energyDamage.value) return true
            if (DamageType.FRAGMENTATION in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && !fragDamage.value) return true

            if (fighterSpec.range < lowerRange.value) return true
            if (fighterSpec.range > upperRange.value && upperRange.value.toInt() != RFSettings.fighterMaxRange) return true

            if (WeaponStats.outside(WingStats.refitTime(fighterSpec), refitLower.value, refitUpper.value, StatScale.REFIT)) return true
            if (WeaponStats.outside(WingStats.size(fighterSpec), sizeLower.value, sizeUpper.value, StatScale.SIZE)) return true

            if (RFSettings.searchBarBehaviour != "Sort" && currentSearch.value.isNotEmpty()) {
                val searchByDesignType = RFSettings.searchByDesignType

                val matchesName = FuzzySearch.fuzzyMatch(currentSearch.value, fighterSpec.wingName).second >= 75
                val matchesDesignType = fighterSpec.sourceMod?.let { FuzzySearch.fuzzyMatch(currentSearch.value, it.name).second >= 80 } ?: false ||
                        fighterSpec.variant?.hullSpec?.manufacturer?.let { FuzzySearch.fuzzyMatch(currentSearch.value, it).second >= 80 } ?: false
                if (!matchesName && !searchByDesignType || searchByDesignType && !matchesName && !matchesDesignType) {
                    return true
                }
            }
        }
        return false
    }
}