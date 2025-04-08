package org.starficz.refitfilters

import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.loading.specs.BaseWeaponSpec
import org.starficz.UIFramework.ReflectionUtils.get
import org.starficz.UIFramework.ReflectionUtils.hasMethodOfName
import org.starficz.UIFramework.ReflectionUtils.invoke
import org.starficz.UIFramework.ReflectionUtils.getFieldsMatching
import org.starficz.UIFramework.*
import org.starficz.UIFramework.allChildsWithMethod
import org.starficz.UIFramework.getChildrenCopy
import org.starficz.refitfilters.filterpanels.createDamageTypeRangeSliderFilterPanel
import org.starficz.refitfilters.filterpanels.createSearchBarFilterPanel
import org.starficz.refitfilters.filterpanels.createWeaponTypesFilterPanel

object FilterPanelCreator {
    val rowWidth = 377f
    val filterRowHeight = 25f
    var pickerYPos = Float.POSITIVE_INFINITY
    var startingYOffset = 0f
    lateinit var weaponDialogPanel: UIPanelAPI

    fun filtersChanged(){
        weaponDialogPanel.invoke("notifyFilterChanged")
    }

    fun modifyFilterPanel(coreUI: UIPanelAPI, openedFromCampaign: Boolean, docked: Boolean) {
        // get the weaponDialogPanel that hasnt been modified if possible, relevant for clicking between weapon slots
        weaponDialogPanel = coreUI.allChildsWithMethod("getPickedWeaponSpec").filter { wdp ->
            val innerWeaponPanel = wdp.invoke("getInnerPanel") as? UIPanelAPI
            innerWeaponPanel?.getChildrenCopy()?.any { it is CustomPanelAPI && it.plugin is ExtendableCustomUIPanelPlugin } == false
        }.firstOrNull() as? UIPanelAPI ?: return

        val innerWeaponPanel = weaponDialogPanel.invoke("getInnerPanel") as? UIPanelAPI ?: return

        val uiElements = innerWeaponPanel.getChildrenCopy()
        if (uiElements.any { it is CustomPanelAPI && it.plugin is ExtendableCustomUIPanelPlugin }) return // return if added

        val existingFiltersIndex = uiElements.indexOfFirst { it.hasMethodOfName("addItem") }

        val currentlySelected = uiElements.getOrNull(existingFiltersIndex-2)
        val currentlyMountedText = uiElements.getOrNull(existingFiltersIndex-1)
        val existingFilters = uiElements[existingFiltersIndex]
        val weaponsList = uiElements[existingFiltersIndex+1] // weaponsList should always exist
        var noWeaponsOrNull = uiElements.getOrNull(existingFiltersIndex+2)

        // sort list after we have found it
        val sortedWeaponSpecPairs = sortAndFilterWeaponsList(weaponsList)

        // add the filter panels if required
        val searchBarFilterPanel =
            innerWeaponPanel.createSearchBarFilterPanel(rowWidth, filterRowHeight)
        val weaponTypesFilterPanel = if(RFSettings.WeaponTypePanelOrder != 0)
            innerWeaponPanel.createWeaponTypesFilterPanel(rowWidth, filterRowHeight) else null
        val damageTypeRangeSliderFilterPanel = if(RFSettings.DamageTypeRangeSliderOrder != 0)
            innerWeaponPanel.createDamageTypeRangeSliderFilterPanel(rowWidth, filterRowHeight) else null

        // remake the "No weapons matching filter" panel if we are the ones that filtered out all the weapons
        if (sortedWeaponSpecPairs.isEmpty() && noWeaponsOrNull == null) {
            noWeaponsOrNull = innerWeaponPanel.CustomPanel(rowWidth, 78f){
                Text(" No weapons matching filter", color = Misc.getBasePlayerColor()) {
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
        val weaponsListHeight = when(sortedWeaponSpecPairs.size) {
            in 0..6 -> 78f * sortedWeaponSpecPairs.size
            else -> 78f * (6 + if(currentlySelected == null) 1 else 0) // additional 1 weapon shown in list if nothing selected
        }

        var weaponPickerHeight = weaponsListHeight + 12f +
            innerWeaponPanel.getChildrenCopy().filter { it !== weaponsList }.sumOf { it.height.toDouble() }.toFloat()

        // position all the panels correctly, make sure to special case the first element based on if we have a weapon selected or not
        activeFilterPanels.forEachIndexed { index, filterPanel ->
            if(index == 0){
                if (currentlyMountedText != null) {
                    filterPanel.position.belowLeft(currentlySelected, 5f)
                    weaponPickerHeight += 3
                }
                else {
                    filterPanel.yAlignOffset = innerWeaponPanel.top - filterPanel.top - 2f
                    filterPanel.xAlignOffset = 5f
                }
            }
            else{
                val pad = if(activeFilterPanels[index-1] === existingFilters) 0f else 1f
                filterPanel.position.belowLeft(activeFilterPanels[index-1], pad)
            }
        }

        weaponsList.position.belowLeft(activeFilterPanels.last(), 6f)
        noWeaponsOrNull?.position?.belowLeft(activeFilterPanels.last(), 6f)

        setWeaponPickerPanelHeight(weaponPickerHeight)
        innerWeaponPanel.yAlignOffset = weaponDialogPanel.top - innerWeaponPanel.top

//        if (openedFromCampaign && RFSettings.enableWeaponSimulation!!) {
//            addSimulationTrigger(mainElement) TODO: actually finally implement the weapon sim
//        }
    }

    fun setWeaponPickerPanelHeight(height: Float) {
        val knownFloatNames = listOf("PAD", "ITEM_WIDTH", "ITEM_HEIGHT", "origXAlignOffset")
        val knownFloatValues = listOf(382f, 385f, 0f)

        val holoHeightField = weaponDialogPanel.getFieldsMatching(type = Float::class.java)
            .singleOrNull { it.name !in knownFloatNames && it.get(weaponDialogPanel) !in knownFloatValues }
            ?: throw Exception("Unable to differentiate weaponPickerDialog's obf float fields")

        holoHeightField.set(weaponDialogPanel, height)
        weaponDialogPanel.height = height

        // pin the weapons list in place
        pinWeaponPickerYPos()
    }

    fun pinWeaponPickerYPos(){
        if(weaponDialogPanel.y - pickerYPos < 0){
            pickerYPos = weaponDialogPanel.y
            startingYOffset = weaponDialogPanel.yAlignOffset
        }
        if(pickerYPos != weaponDialogPanel.y){
            weaponDialogPanel.yAlignOffset = startingYOffset + (weaponDialogPanel.y - pickerYPos)
        }
    }

    fun sortAndFilterWeaponsList(weaponsList: UIComponentAPI): List<Pair<Any, WeaponSpecAPI>> {
        val individualWeapons = (weaponsList.invoke("getItems") as List<*>).toMutableList()

        // map each weapon spec onto their UIpanel
        val weaponSpecPairs = individualWeapons.mapNotNull { weapon ->
            val weaponTooltip = weapon!!.invoke("getTooltip")!!
            weapon to weaponTooltip.get(type=BaseWeaponSpec::class.java) as WeaponSpecAPI
        }

        // filter and fuzzy sort the list
        var sortedWeaponSpecPairs = weaponSpecPairs.filter { !weaponFiltered(it.second) }
        if(RFSettings.searchBarBehaviour != "Filter"){
            sortedWeaponSpecPairs = sortedWeaponSpecPairs.sortedWith(
                compareByDescending {
                    FuzzySearch.fuzzyMatch(FilterData.currentSearch, it.second.weaponName).second
                }
            )
        }

        // clear the list and re-add the sorted list
        weaponsList.invoke("clear")
        for (weapon in sortedWeaponSpecPairs){
            weaponsList.invoke("addItem", weapon.first)
        }

        return sortedWeaponSpecPairs
    }

    fun weaponFiltered(weaponSpec: WeaponSpecAPI): Boolean{
        with(FilterData){

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

            if (weaponSpec.maxRange < lowerRange && lowerRange.toInt() != RFSettings.minRange) return true
            if (weaponSpec.maxRange > upperRange && upperRange.toInt() != RFSettings.maxRange) return true

            if (RFSettings.searchBarBehaviour != "Sort" && currentSearch.isNotEmpty()) {
                val searchByDesignType = RFSettings.searchByDesignType!!

                val matchesName = FuzzySearch.fuzzyMatch(currentSearch, weaponSpec.weaponName).second >= 75
                val matchesDesignType = FuzzySearch.fuzzyMatch(currentSearch, weaponSpec.manufacturer).second >= 80

                if (!matchesName && !searchByDesignType || searchByDesignType && !matchesName && !matchesDesignType) {
                    return true
                }
            }
        }

        return false
    }
}