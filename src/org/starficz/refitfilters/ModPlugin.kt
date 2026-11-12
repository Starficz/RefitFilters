package org.starficz.refitfilters

import com.fs.starfarer.api.BaseModPlugin
import lunalib.lunaSettings.LunaSettings
import org.starficz.starui.compose.runtime.StarUIManager


class ModPlugin : BaseModPlugin() {
    override fun onApplicationLoad() {
        LunaSettings.addSettingsListener(RFSettings)
        StarUIManager.registerDynamicInjection(WeaponPickerInjection())
        StarUIManager.registerDynamicInjection(FighterPickerInjection())

        StarUIManager.registerDynamicInjection(PlayerCargoInjection())
        StarUIManager.registerDynamicInjection(EntityCargoInjection())
    }
}