package org.starficz.refitfilters

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Tags
import java.util.HashSet

class ModPlugin : BaseModPlugin() {
    companion object {
        var kineticActive = true
        var heActive = true
        var energyActive = true
        var fragActive = true
        var projectileActive = true
        var beamActive = true
        var pdActive = true
        var nonpdActive = true
        var lowerRange = 0
        var upperRange = 1500
        val minRange = 0
        val maxRange = 1500
    }

    override fun onGameLoad(newGame: Boolean) {
        Global.getSector().addTransientScript(CampaignUIAdderScript())
    }
}