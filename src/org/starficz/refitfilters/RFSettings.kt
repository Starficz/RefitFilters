package org.starficz.refitfilters

import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener

object RFSettings : LunaSettingsListener {
    var modID = "refitfilters"

    var searchBarBehaviour = LunaSettings.getString(modID, "refitfilters_searchbarBehaviour")!!
    var searchByDesignType = LunaSettings.getBoolean(modID, "refitfilters_searchByDesignType")!!

    var ResetButtonSearchBarPanelOrder = LunaSettings.getInt(modID, "refitfilters_resetButtonSearchBarPanelOrder")!!
    var VanillaWeaponAvailabilityWeaponSlotPanelOrder = LunaSettings.getInt(modID, "refitfilters_vanillaWeaponAvailabilityWeaponSlotPanelOrder")!!
    var WeaponTypePanelOrder = LunaSettings.getInt(modID, "refitfilters_weaponTypePanelOrder")!!
    var DamageTypeRangeSliderOrder = LunaSettings.getInt(modID, "refitfilters_damageTypeRangeSliderOrder")!!

    var weaponMinRange = LunaSettings.getInt(modID, "refitfilters_weaponMinRange")!!
    var weaponMaxRange = LunaSettings.getInt(modID, "refitfilters_weaponMaxRange")!!
    var weaponRangeIncrement = LunaSettings.getInt(modID, "refitfilters_weaponRangeIncrement")!!
    var weaponRangeTickSnapping = LunaSettings.getBoolean(modID, "refitfilters_weaponRangeTickSnapping")!!

    var fighterMinRange = LunaSettings.getInt(modID, "refitfilters_fighterMinRange")!!
    var fighterMaxRange = LunaSettings.getInt(modID, "refitfilters_fighterMaxRange")!!
    var fighterRangeIncrement = LunaSettings.getInt(modID, "refitfilters_fighterRangeIncrement")!!
    var fighterRangeTickSnapping = LunaSettings.getBoolean(modID, "refitfilters_fighterRangeTickSnapping")!!

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

            weaponMinRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponMinRange")!!
            weaponMaxRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponMaxRange")!!
            weaponRangeIncrement = LunaSettings.getInt(RFSettings.modID, "refitfilters_weaponRangeIncrement")!!
            weaponRangeTickSnapping = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_weaponRangeTickSnapping")!!

            fighterMinRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterMinRange")!!
            fighterMaxRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterMaxRange")!!
            fighterRangeIncrement = LunaSettings.getInt(RFSettings.modID, "refitfilters_fighterRangeIncrement")!!
            fighterRangeTickSnapping = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_fighterRangeTickSnapping")!!

            //enableAdditionalFilters = LunaSettings.getBoolean(modID, "refitfilters_enableAdditionalFilters")!!
            //enableWeaponSimulation = LunaSettings.getBoolean(modID, "refitfilters_enableSimulation")!!
        }
    }
}