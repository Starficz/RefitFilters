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

    var minRange = LunaSettings.getInt(modID, "refitfilters_minRange")!!
    var maxRange = LunaSettings.getInt(modID, "refitfilters_maxRange")!!
    var rangeIncrement = LunaSettings.getInt(modID, "refitfilters_rangeIncrement")!!
    var rangeTickSnapping = LunaSettings.getBoolean(modID, "refitfilters_rangeTickSnapping")!!

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

            minRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_minRange")!!
            maxRange = LunaSettings.getInt(RFSettings.modID, "refitfilters_maxRange")!!
            rangeIncrement = LunaSettings.getInt(RFSettings.modID, "refitfilters_rangeIncrement")!!
            rangeTickSnapping = LunaSettings.getBoolean(RFSettings.modID, "refitfilters_rangeTickSnapping")!!

            //enableAdditionalFilters = LunaSettings.getBoolean(modID, "refitfilters_enableAdditionalFilters")!!
            //enableWeaponSimulation = LunaSettings.getBoolean(modID, "refitfilters_enableSimulation")!!
        }
    }
}