package org.starficz.refitfilters

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings


class ModPlugin : BaseModPlugin() {
    override fun onApplicationLoad() {
        LunaSettings.addSettingsListener(RFSettings)
    }

    override fun onGameLoad(newGame: Boolean) {
        Global.getSector().addTransientScript(CampaignUIAdderScript())
    }
}