package org.starficz.refitfilters

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver
import org.starficz.UIFramework.ReflectionUtils.invoke


class CombatUIAdderScript : BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val state = AppDriver.getInstance().currentState
        if (state !is TitleScreenState) return

        val core = state.invoke("getScreenPanel") as? UIPanelAPI ?: return

        FilterPanelCreator.modifyFilterPanel(core, openedFromCampaign = false, docked = false)
    }
}