package org.starficz.refitfilters

import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.starficz.UIFramework.ReflectionUtils.invoke
import org.starficz.UIFramework.*
import org.starficz.UIFramework.ReflectionUtils.getMethodsMatching
import org.starficz.UIFramework.allChildsWithMethod
import org.starficz.UIFramework.getChildrenCopy
import org.starficz.refitfilters.PickerPanelHelpers.setPickerPanelHeight
import org.starficz.refitfilters.PickerPanelHelpers.sortAndFilterList
import org.starficz.refitfilters.filterpanels.createDamageTypeRangeSliderFilterPanel
import org.starficz.refitfilters.filterpanels.createSearchBarFilterPanel
import org.starficz.refitfilters.filterpanels.createWeaponTypesFilterPanel
import java.util.Comparator

data class PickerPanelOffset(
    var pickerTop: Float = Float.POSITIVE_INFINITY,
)

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

    fun modifyFilterPanels(coreUI: UIPanelAPI, openedFromCampaign: Boolean, docked: Boolean){
        modifyFilterPanel(coreUI, PickerPanelType.Weapons)
        modifyFilterPanel(coreUI, PickerPanelType.Fighters)
    }

    fun modifyFilterPanel(coreUI: UIPanelAPI, pickerPanelType: PickerPanelType) {
        // get the weaponDialogPanel that hasnt been modified if possible, relevant for clicking between weapon slots
        val panelMethodName = when(pickerPanelType){
            PickerPanelType.Weapons -> "getPickedWeaponSpec"
            PickerPanelType.Fighters -> "getPickedFighterSpec"
        }
        val filterData = when(pickerPanelType){
            PickerPanelType.Weapons -> weaponFilterData
            PickerPanelType.Fighters -> fighterFilterData
        }

        val pickerPanel = coreUI.allChildsWithMethod(panelMethodName).firstOrNull { wdp ->
            val innerPanel = wdp.invoke("getInnerPanel") as? UIPanelAPI
            innerPanel?.getChildrenCopy()?.any { it is CustomPanelAPI && it.plugin is ExtendableCustomUIPanelPlugin } == false
        } as? UIPanelAPI ?: return

        val innerPanel = pickerPanel.invoke("getInnerPanel") as? UIPanelAPI ?: return

        val uiElements = innerPanel.getChildrenCopy()
        if (uiElements.any { it is CustomPanelAPI && it.plugin is ExtendableCustomUIPanelPlugin }) return // return if added

        val existingFiltersIndex = uiElements.indexOfFirst { it.getMethodsMatching("addItem").isNotEmpty() }

        val currentlySelected = uiElements.getOrNull(existingFiltersIndex-2)
        val currentlyMountedText = uiElements.getOrNull(existingFiltersIndex-1)
        val existingFilters = uiElements[existingFiltersIndex]
        val itemsList = uiElements[existingFiltersIndex+1] // scroller should always exist
        var noItems = uiElements.getOrNull(existingFiltersIndex+2)

        // sort list after we have found it
        val sortedSpecPairs = when(pickerPanelType){
            PickerPanelType.Weapons -> {
                val searchString = weaponFilterData.currentSearch
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
                val searchString = fighterFilterData.currentSearch
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

        // add the filter panels if required
        val searchBarFilterPanel =
            innerPanel.createSearchBarFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData)
        val weaponTypesFilterPanel = if(RFSettings.WeaponTypePanelOrder != 0 && filterData is WeaponFilterData)
            innerPanel.createWeaponTypesFilterPanel(rowWidth, filterRowHeight, pickerPanel, filterData) else null
        val damageTypeRangeSliderFilterPanel = if(RFSettings.DamageTypeRangeSliderOrder != 0)
            innerPanel.createDamageTypeRangeSliderFilterPanel( rowWidth, filterRowHeight, pickerPanel, filterData) else null

        // remake the "No x matching filter" panel if we are the ones that filtered out all the items
        if (sortedSpecPairs.isEmpty() && noItems == null) {
            noItems = innerPanel.CustomPanel(rowWidth, itemRowHeight){
                val itemText = when(pickerPanelType) { PickerPanelType.Weapons -> "weapons" else -> "fighters" }
                Text(" No $itemText matching filter", color = Misc.getBasePlayerColor()) {
                    anchorInCenterOfParent()
                }
            }
        }

        // get all the active filter panels
        val activeFilterPanels = listOfNotNull(
            searchBarFilterPanel to RFSettings.ResetButtonSearchBarPanelOrder,
            existingFilters to RFSettings.VanillaWeaponAvailabilityWeaponSlotPanelOrder,
            weaponTypesFilterPanel?.let { it to RFSettings.WeaponTypePanelOrder }, // Only add if non-null
            damageTypeRangeSliderFilterPanel?.let { it to RFSettings.DamageTypeRangeSliderOrder } // Only add if non-null
        ).filter { (_, order) -> order != 0 }.sortedBy { (_, order) -> order }.map { (panel, _) -> panel }

        /*
        * To the people reading this, as much progress as I have made with the UIFramework, interfacing with vanilla
        * still requires a lot of pixel peeping and magic numbers behind the scenes to make stuff line up perfectly.
        */

        // calculate the height
        val itemsListHeight = when(sortedSpecPairs.size) {
            in 0..6 -> itemRowHeight * sortedSpecPairs.size
            else -> itemRowHeight * (6 + if(currentlySelected == null) 1 else 0) // additional 1 weapon shown in list if nothing selected
        }

        var pickerHeight = itemsListHeight + 12f +
                innerPanel.getChildrenCopy().filter { it !== itemsList }.sumOf { it.height.toDouble() }.toFloat()

        // position all the panels correctly, make sure to special case the first element based on if we have a weapon selected or not
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

//        if (openedFromCampaign && RFSettings.enableWeaponSimulation!!) {
//            addSimulationTrigger(mainElement) TODO: actually finally implement the weapon sim
//        }
    }

    fun weaponFiltered(weaponSpec: WeaponSpecAPI, filterData: WeaponFilterData): Boolean{
        with(filterData){

            if (weaponSpec.damageType == DamageType.KINETIC && kineticDamage.isFiltered) return true
            if (weaponSpec.damageType == DamageType.HIGH_EXPLOSIVE && heDamage.isFiltered) return true
            if (weaponSpec.damageType == DamageType.ENERGY && energyDamage.isFiltered) return true
            if (weaponSpec.damageType == DamageType.FRAGMENTATION && fragDamage.isFiltered) return true

            if (!weaponSpec.isBeam && projectileWeapons.isFiltered) return true
            if (weaponSpec.isBeam && beamWeapons.isFiltered) return true

            val weaponIsPD = weaponSpec.aiHints.any { it == WeaponAPI.AIHints.PD || it == WeaponAPI.AIHints.PD_ALSO }

            if (weaponIsPD && pdWeapons.isFiltered) return true
            if (!weaponIsPD && nonpdWeapons.isFiltered) return true

            if (weaponSpec.usesAmmo() && ammoWeapons.isFiltered) return true
            if (!weaponSpec.usesAmmo() && nonAmmoWeapons.isFiltered) return true

            if (weaponSpec.maxRange < lowerRange && lowerRange.toInt() != RFSettings.weaponMinRange) return true
            if (weaponSpec.maxRange > upperRange && upperRange.toInt() != RFSettings.weaponMaxRange) return true

            if (RFSettings.searchBarBehaviour != "Sort" && currentSearch.isNotEmpty()) {
                val searchByDesignType = RFSettings.searchByDesignType

                val matchesName = FuzzySearch.fuzzyMatch(currentSearch, weaponSpec.weaponName).second >= 110
                val matchesDesignType = FuzzySearch.fuzzyMatch(currentSearch, weaponSpec.manufacturer).second >= 140 ||
                    weaponSpec.sourceMod?.let { FuzzySearch.fuzzyMatch(currentSearch, it.name).second >= 140 } ?: false

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
                && kineticDamage.isFiltered) return true
            if (DamageType.HIGH_EXPLOSIVE in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && heDamage.isFiltered) return true
            if (DamageType.ENERGY in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && energyDamage.isFiltered) return true
            if (DamageType.FRAGMENTATION in fighterSpec.variant.fittedWeaponSlots.map { fighterSpec.variant.getWeaponSpec(it).damageType }
                && fragDamage.isFiltered) return true

            if (fighterSpec.range < lowerRange && lowerRange.toInt() != RFSettings.fighterMinRange) return true
            if (fighterSpec.range > upperRange && upperRange.toInt() != RFSettings.fighterMaxRange) return true

            if (RFSettings.searchBarBehaviour != "Sort" && currentSearch.isNotEmpty()) {
                val searchByDesignType = RFSettings.searchByDesignType

                val matchesName = FuzzySearch.fuzzyMatch(currentSearch, fighterSpec.wingName).second >= 75
                val matchesDesignType = fighterSpec.sourceMod?.let { FuzzySearch.fuzzyMatch(currentSearch, it.name).second >= 80 } ?: false ||
                        fighterSpec.variant?.hullSpec?.manufacturer?.let { FuzzySearch.fuzzyMatch(currentSearch, it).second >= 80 } ?: false
                if (!matchesName && !searchByDesignType || searchByDesignType && !matchesName && !matchesDesignType) {
                    return true
                }
            }
        }
        return false
    }
}
