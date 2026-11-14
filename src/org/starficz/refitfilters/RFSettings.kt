package org.starficz.refitfilters

import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener

object RFSettings : LunaSettingsListener {
    var modID = "refitfilters"

    // The Radio settings' labels (LunaSettings.csv); before the values below, which are read through them.
    private val sortKeys = mapOf(
        "Name" to SortKey.NAME, "OP" to SortKey.OP, "Range" to SortKey.RANGE, "Damage" to SortKey.DMG,
        "DPS" to SortKey.DPS, "Flux Efficiency" to SortKey.EFF, "Refit Time" to SortKey.REFIT, "Wing Size" to SortKey.SIZE
    )
    private val sliderStats = mapOf(
        "Range" to SliderStat.RANGE, "DPS" to SliderStat.DPS, "Damage" to SliderStat.DMG,
        "Flux Efficiency" to SliderStat.EFF, "Refit Time" to SliderStat.REFIT, "Wing Size" to SliderStat.SIZE
    )
    private fun sortKey(id: String) = sortKeys[LunaSettings.getString(modID, id)] ?: SortKey.OP
    private fun descending(id: String) = LunaSettings.getString(modID, id) != "Ascending"
    private fun sliderStat(id: String) = sliderStats[LunaSettings.getString(modID, id)] ?: SliderStat.RANGE

    var searchBarBehaviour = LunaSettings.getString(modID, "refitfilters_searchbarBehaviour")!!
    var searchByDesignType = LunaSettings.getBoolean(modID, "refitfilters_searchByDesignType")!!

    var ResetButtonSearchBarPanelOrder = LunaSettings.getInt(modID, "refitfilters_resetButtonSearchBarPanelOrder")!!
    var VanillaWeaponAvailabilityWeaponSlotPanelOrder = LunaSettings.getInt(modID, "refitfilters_vanillaWeaponAvailabilityWeaponSlotPanelOrder")!!
    var WeaponTypePanelOrder = LunaSettings.getInt(modID, "refitfilters_weaponTypePanelOrder")!!
    var DamageTypeRangeSliderOrder = LunaSettings.getInt(modID, "refitfilters_damageTypeRangeSliderOrder")!!
    var sortRowEnabled = LunaSettings.getBoolean(modID, "refitfilters_sortRow")!!

    // what the lists are sorted by and the slider shows at start; RESET FILTERS goes back to them
    var weaponDefaultSort = sortKey("refitfilters_weaponDefaultSort")
    var weaponDefaultSortDescending = descending("refitfilters_weaponDefaultSortOrder")
    var fighterDefaultSort = sortKey("refitfilters_fighterDefaultSort")
    var fighterDefaultSortDescending = descending("refitfilters_fighterDefaultSortOrder")
    var weaponDefaultSlider = sliderStat("refitfilters_weaponDefaultSlider")
    var fighterDefaultSlider = sliderStat("refitfilters_fighterDefaultSlider")

    var weaponMaxRange = LunaSettings.getInt(modID, "refitfilters_weaponMaxRange")!!
    var weaponRangeIncrement = LunaSettings.getInt(modID, "refitfilters_weaponRangeIncrement")!!
    var weaponRangeTickSnapping = LunaSettings.getBoolean(modID, "refitfilters_weaponRangeTickSnapping")!!

    var fighterMaxRange = LunaSettings.getInt(modID, "refitfilters_fighterMaxRange")!!
    var fighterRangeIncrement = LunaSettings.getInt(modID, "refitfilters_fighterRangeIncrement")!!
    var fighterRangeTickSnapping = LunaSettings.getBoolean(modID, "refitfilters_fighterRangeTickSnapping")!!

    // the upper ends of the slider's other stats (StatScale)
    var weaponMaxDps = LunaSettings.getInt(modID, "refitfilters_weaponMaxDps")!!
    var weaponMaxDamage = LunaSettings.getInt(modID, "refitfilters_weaponMaxDamage")!!
    var weaponMaxEfficiency = LunaSettings.getDouble(modID, "refitfilters_weaponMaxEfficiency")!!
    var fighterMaxRefit = LunaSettings.getInt(modID, "refitfilters_fighterMaxRefit")!!
    var fighterMaxWingSize = LunaSettings.getInt(modID, "refitfilters_fighterMaxWingSize")!!

    //var enableAdditionalFilters = LunaSettings.getBoolean(modID, "refitfilters_enableAdditionalFilters")!!
    //var enableWeaponSimulation = LunaSettings.getBoolean(modID, "refitfilters_enableSimulation")!!

    override fun settingsChanged(modID: String) {
        if (modID == RFSettings.modID) {
            searchBarBehaviour = LunaSettings.getString(RFSettings.modID, "refitfilters_searchbarBehaviour")!!
            searchByDesignType = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_searchByDesignType")!!

            ResetButtonSearchBarPanelOrder = LunaSettings.getInt(RFSettings.modID, "refitfilters_resetButtonSearchBarPanelOrder")!!
            VanillaWeaponAvailabilityWeaponSlotPanelOrder = LunaSettings.getInt(RFSettings.modID, "refitfilters_vanillaWeaponAvailabilityWeaponSlotPanelOrder")!!
            WeaponTypePanelOrder = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponTypePanelOrder")!!
            DamageTypeRangeSliderOrder = LunaSettings.getInt(RFSettings.modID, "refitfilters_damageTypeRangeSliderOrder")!!
            sortRowEnabled = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_sortRow")!!

            weaponDefaultSort = sortKey("refitfilters_weaponDefaultSort")
            weaponDefaultSortDescending = descending("refitfilters_weaponDefaultSortOrder")
            fighterDefaultSort = sortKey("refitfilters_fighterDefaultSort")
            fighterDefaultSortDescending = descending("refitfilters_fighterDefaultSortOrder")
            weaponDefaultSlider = sliderStat("refitfilters_weaponDefaultSlider")
            fighterDefaultSlider = sliderStat("refitfilters_fighterDefaultSlider")

            weaponMaxRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponMaxRange")!!
            weaponRangeIncrement = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponRangeIncrement")!!
            weaponRangeTickSnapping = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_weaponRangeTickSnapping")!!

            fighterMaxRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterMaxRange")!!
            fighterRangeIncrement = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterRangeIncrement")!!
            fighterRangeTickSnapping = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_fighterRangeTickSnapping")!!

            weaponMaxDps = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponMaxDps")!!
            weaponMaxDamage = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponMaxDamage")!!
            weaponMaxEfficiency = LunaSettings.getDouble(RFSettings.modID, "refitfilters_weaponMaxEfficiency")!!
            fighterMaxRefit = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterMaxRefit")!!
            fighterMaxWingSize = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterMaxWingSize")!!

            //enableAdditionalFilters = LunaSettings.getBoolean(modID, "refitfilters_enableAdditionalFilters")!!
            //enableWeaponSimulation = LunaSettings.getBoolean(modID, "refitfilters_enableSimulation")!!
        }
    }
}