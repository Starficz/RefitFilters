package org.starficz.refitfilters

import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.starficz.staruiframework.*
import org.starficz.refitfilters.ReflectionUtils.getMethodsMatching
import org.starficz.refitfilters.PickerPanelHelpers.setPickerPanelHeight
import org.starficz.refitfilters.PickerPanelHelpers.sortAndFilterList
import org.starficz.refitfilters.filterpanels.createDamageTypeRangeSliderFilterPanel
import org.starficz.refitfilters.filterpanels.createSearchBarFilterPanel
import org.starficz.refitfilters.filterpanels.createWeaponTypesFilterPanel
import java.util.Comparator

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


    fun injectFilters(innerPanel: UIPanelAPI, pickerPanel: UIPanelAPI, pickerPanelType: PickerPanelType) {
        val filterData = when(pickerPanelType){
            PickerPanelType.Weapons -> weaponFilterData
            PickerPanelType.Fighters -> fighterFilterData
        }

        val uiElements = innerPanel.getChildrenCopy()
        if (uiElements.any { it is CustomPanelAPI && it.plugin is StarUIPanelPlugin }) return

        val existingFiltersIndex = uiElements.indexOfFirst { it.getMethodsMatching("addItem").isNotEmpty() }

        val currentlySelected = uiElements.getOrNull(existingFiltersIndex-2)
        val currentlyMountedText = uiElements.getOrNull(existingFiltersIndex-1)
        val existingFilters = uiElements[existingFiltersIndex]
        val itemsList = uiElements[existingFiltersIndex+1]
        var noItems = uiElements.getOrNull(existingFiltersIndex+2)

        val sortedSpecPairs = when(pickerPanelType){
            PickerPanelType.Weapons -> {
                val searchString = weaponFilterData.currentSearch.value
                val comparator: Comparator<Pair<Any, WeaponSpecAPI>> = compareByDescending { (_, spec) ->
                    maxOf(FuzzySearch.fuzzyMatch(searchString, spec.weaponName).second,
                        FuzzySearch.fuzzyMatch(searchString, spec.manufacturer).second,
                        spec.sourceMod?.let { FuzzySearch.fuzzyMatch(searchString, it.name).second } ?: 0
                    )
                }

                sortAndFilterList(itemsList, WeaponSpecAPI::class.java, ::weaponFiltered, weaponFilterData,
                    comparator, searchString, RFSettings.searchBarBehaviour)
            }
            PickerPanelType.Fighters -> {
                val searchString = fighterFilterData.currentSearch.value
                val comparator: Comparator<Pair<Any, FighterWingSpecAPI>> = compareByDescending { (_, spec) ->
                    maxOf(FuzzySearch.fuzzyMatch(searchString, spec.wingName).second,
                        spec.variant?.hullSpec?.manufacturer?.let { FuzzySearch.fuzzyMatch(searchString, it).second } ?: 0,
                        spec.sourceMod?.let { FuzzySearch.fuzzyMatch(searchString, it.name).second } ?: 0
                    )
                }

                sortAndFilterList(itemsList, FighterWingSpecAPI::class.java, ::fighterFiltered, fighterFilterData,
                    comparator, searchString, RFSettings.searchBarBehaviour)
            }
        }

        val searchBarFilterPanel =
            innerPanel.createSearchBarFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
        val weaponTypesFilterPanel = if(RFSettings.WeaponTypePanelOrder != 0 && filterData is WeaponFilterData)
            innerPanel.createWeaponTypesFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData) else null
        val damageTypeRangeSliderFilterPanel = if(RFSettings.DamageTypeRangeSliderOrder != 0)
            innerPanel.createDamageTypeRangeSliderFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData) else null

        if (sortedSpecPairs.isEmpty() && noItems == null) {
            noItems = innerPanel.CustomPanel(rowWidth, itemRowHeight) {
                val itemText = when(pickerPanelType) { PickerPanelType.Weapons -> "weapons" else -> "fighters" }
                Text(" No $itemText matching filter", color = Misc.getBasePlayerColor(), anchor = Anchor.inside.center.ofParent())
            }
        }

        val activeFilterPanels = listOfNotNull(
            searchBarFilterPanel to RFSettings.ResetButtonSearchBarPanelOrder,
            existingFilters to RFSettings.VanillaWeaponAvailabilityWeaponSlotPanelOrder,
            weaponTypesFilterPanel?.let { it to RFSettings.WeaponTypePanelOrder },
            damageTypeRangeSliderFilterPanel?.let { it to RFSettings.DamageTypeRangeSliderOrder }
        ).filter { (_, order) -> order != 0 }.sortedBy { (_, order) -> order }.map { (panel, _) -> panel }

        val itemsListHeight = when(sortedSpecPairs.size) {
            in 0..6 -> itemRowHeight * sortedSpecPairs.size
            else -> itemRowHeight * (6 + if(currentlySelected == null) 1 else 0)
        }

        var pickerHeight = itemsListHeight + 12f +
                innerPanel.getChildrenCopy().filter { it !== itemsList }.sumOf { it.height.toDouble() }.toFloat()

        activeFilterPanels.forEachIndexed { index, filterPanel ->
            if(index == 0){
                if (currentlyMountedText != null) {
                    filterPanel.position.belowLeft(currentlySelected, 5f)
                    pickerHeight += 3
                }
                else {
                    filterPanel.yAlignOffset = innerPanel.top - filterPanel.top - 2f
                    filterPanel.xAlignOffset = 5f
                }
            }
            else{
                val pad = if(activeFilterPanels[index-1] === existingFilters) 0f else 1f
                filterPanel.position.belowLeft(activeFilterPanels[index-1], pad)
            }
        }

        itemsList.position.belowLeft(activeFilterPanels.last(), 6f)
        noItems?.position?.belowLeft(activeFilterPanels.last(), 6f)

        if (pickerPanelType == PickerPanelType.Fighters) pickerPanel.width -= 5f

        setPickerPanelHeight(pickerHeight, pickerPanel)
        innerPanel.yAlignOffset = pickerPanel.top - innerPanel.top
    }

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