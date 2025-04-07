package org.starficz.refitfilters

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import org.starficz.UIFramework.ReflectionUtils.invoke


class CampaignUIAdderScript : EveryFrameScript{

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {

        if (!Global.getSector().isPaused) return //Return if not paused
        if (Global.getSector().campaignUI.currentCoreTab != CoreUITabId.REFIT) return //Return if not Refit

        val state = AppDriver.getInstance().currentState
        if (state !is CampaignState) return

        var docked = false

        //Try to check if a dialog is open, and grab the CoreUI from it (Relevant for when refit is opened while docked to a colony, etc)
        val dialog = state.invoke("getEncounterDialog")
        val core = if (dialog != null) {
            docked = true
            dialog.invoke("getCoreUI") as? UIPanelAPI
        }
        else { state.invoke("getCore") as? UIPanelAPI} ?: return

        FilterPanelCreator.modifyFilterPanel(core, true, docked)
    }
}