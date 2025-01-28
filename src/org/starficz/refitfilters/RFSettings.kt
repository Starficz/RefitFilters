package org.starficz.refitfilters

import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener

object RFSettings : LunaSettingsListener
{

    var modID = "refitfilters"

    var searchBarBehaviour = LunaSettings.getString(modID, "refitfilters_searchbarBehaviour")
    var searchByDesignType = LunaSettings.getBoolean(modID, "refitfilters_searchByDesignType")
    var enableAdditionalFilters = LunaSettings.getBoolean(modID, "refitfilters_enableAdditionalFilters")

    var enableWeaponSimulation = LunaSettings.getBoolean(modID, "refitfilters_enableSimulation")


    override fun settingsChanged(modID: String) {
        if (modID == RFSettings.modID)
        {
            loadSettings()
        }
    }

    @JvmStatic
    fun loadSettings()
    {
        searchBarBehaviour = LunaSettings.getString(modID, "refitfilters_searchbarBehaviour")
        searchByDesignType = LunaSettings.getBoolean(modID, "refitfilters_searchByDesignType")
        enableAdditionalFilters = LunaSettings.getBoolean(modID, "refitfilters_enableAdditionalFilters")

        enableWeaponSimulation = LunaSettings.getBoolean(modID, "refitfilters_enableSimulation")

        if (!enableAdditionalFilters!!) {
            ModPlugin.projectileActive = true
            ModPlugin.beamActive = true
            ModPlugin.pdActive = true
            ModPlugin.nonpdActive = true
        }
    }
}