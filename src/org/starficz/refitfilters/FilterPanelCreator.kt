package org.starficz.refitfilters

import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.filterpanels.*
import org.starficz.refitfilters.ReflectionUtils.getMethodsMatching
import org.starficz.starui.AreaCheckboxTheme
import org.starficz.starui.components.Theme
import org.starficz.starui.runtime.StarUIPanelPlugin
import org.starficz.starui.StarUITheme
import org.starficz.starui.vanilla.*

enum class PickerPanelType {
    Weapons,
    Fighters
}

object FilterPanelCreator {
    val rowWidth = 377f
    val filterRowHeight = 25f
    val itemRowHeight = 78f

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

    /** Whether this picker gets the sort row: weapons only, when the setting allows it. Always last, on the list. */
    private fun hasSortRow(pickerPanelType: PickerPanelType) = pickerPanelType == PickerPanelType.Weapons && RFSettings.sortRowEnabled

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
        val sortRow = hasSortRow(pickerPanelType)
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

        val uiElements = innerPanel.children
        if (uiElements.any { it is CustomPanelAPI && it.plugin is StarUIPanelPlugin }) return

        // vanilla's children, in the order its build adds them:
        // [currently mounted item, its caption,] filter bar, item list[, "No ... matching filter" label]
        val existingFiltersIndex = uiElements.indexOfFirst { it.getMethodsMatching("addItem").isNotEmpty() }
        if (existingFiltersIndex < 0) return

        val currentlySelected = uiElements.getOrNull(existingFiltersIndex-2)
        val currentlyMountedText = uiElements.getOrNull(existingFiltersIndex-1)
        val existingFilters = uiElements[existingFiltersIndex]
        val itemsList = uiElements.getOrNull(existingFiltersIndex+1) ?: return
        val noItems = uiElements.getOrNull(existingFiltersIndex+2)

        lateinit var searchBarFilterPanel: CustomPanelAPI
        var weaponTypesFilterPanel: CustomPanelAPI? = null
        var damageTypeRangeSliderFilterPanel: CustomPanelAPI? = null
        var sortPanel: CustomPanelAPI? = null
        innerPanel.Theme(filterTheme()) {
            searchBarFilterPanel = createSearchBarFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
            if (RFSettings.WeaponTypePanelOrder != 0 && filterData is WeaponFilterData)
                weaponTypesFilterPanel = createWeaponTypesFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
            if (RFSettings.DamageTypeRangeSliderOrder != 0)
                damageTypeRangeSliderFilterPanel = createDamageTypeRangeSliderFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
            if (hasSortRow(pickerPanelType) && filterData is WeaponFilterData)
                sortPanel = createSortPanel(rowWidth, pickerPanel, filterData)
        }

        val activeFilterPanels = listOfNotNull(
            searchBarFilterPanel to RFSettings.ResetButtonSearchBarPanelOrder,
            existingFilters to RFSettings.VanillaWeaponAvailabilityWeaponSlotPanelOrder,
            weaponTypesFilterPanel?.let { it to RFSettings.WeaponTypePanelOrder },
            damageTypeRangeSliderFilterPanel?.let { it to RFSettings.DamageTypeRangeSliderOrder }
        ).filter { (_, order) -> order != 0 }.sortedBy { (_, order) -> order }.map { (panel, _) -> panel } +
            listOfNotNull(sortPanel)
        if (activeFilterPanels.isEmpty()) return

        activeFilterPanels.forEachIndexed { index, filterPanel ->
            if(index == 0){
                if (currentlyMountedText != null) {
                    filterPanel.position.belowLeft(currentlySelected, 5f)
                }
                else {
                    filterPanel.yAlignOffset = innerPanel.top - filterPanel.top - 2f
                    filterPanel.xAlignOffset = 5f
                }
            }
            else{
                val pad = if (filterPanel === sortPanel) SORT_ROW_GAP
                          else if (activeFilterPanels[index-1] === existingFilters) 0f else 1f
                filterPanel.position.belowLeft(activeFilterPanels[index-1], pad)
            }
        }

        val listGap = if (sortPanel != null) SORT_LIST_GAP else 6f
        itemsList.position.belowLeft(activeFilterPanels.last(), listGap)
        noItems?.position?.belowLeft(activeFilterPanels.last(), listGap)
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

            if (weaponSpec.maxRange < lowerRange.value && lowerRange.value.toInt() != RFSettings.weaponMinRange) return true
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

            if (fighterSpec.range < lowerRange.value && lowerRange.value.toInt() != RFSettings.fighterMinRange) return true
            if (fighterSpec.range > upperRange.value && upperRange.value.toInt() != RFSettings.fighterMaxRange) return true

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