package org.starficz.refitfilters

import com.fs.starfarer.api.BaseModPlugin
import lunalib.lunaSettings.LunaSettings
import org.starficz.starui.compose.runtime.StarUIManager


class ModPlugin : BaseModPlugin() {
    override fun onApplicationLoad() {
        LunaSettings.addSettingsListener(RFSettings)
        // The weapon and fighter pickers are handled by WeaponPickerPatch / FighterPickerPatch (PatchLib hooks on
        // their builds).

        StarUIManager.registerDynamicInjection(PlayerCargoInjection())
        StarUIManager.registerDynamicInjection(EntityCargoInjection())
    }
}