package org.starficz.refitfilters

import com.fs.starfarer.api.BaseModPlugin
import lunalib.lunaSettings.LunaSettings
import org.starficz.starui.compose.runtime.StarUIManager


class ModPlugin : BaseModPlugin() {
    override fun onApplicationLoad() {
        LunaSettings.addSettingsListener(RFSettings)
        // The weapon picker is handled by WeaponPickerPatch (PatchLib hooks on its build).
        StarUIManager.registerDynamicInjection(FighterPickerInjection())

        StarUIManager.registerDynamicInjection(PlayerCargoInjection())
        StarUIManager.registerDynamicInjection(EntityCargoInjection())
    }
}