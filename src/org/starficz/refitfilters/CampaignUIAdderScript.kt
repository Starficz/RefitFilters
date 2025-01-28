package org.starficz.refitfilters

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import org.starficz.refitfilters.ReflectionUtils
import org.starficz.refitfilters.getChildrenCopy

class CampaignUIAdderScript : EveryFrameScript{

    @Transient var dockedPanel: CustomPanelAPI? = null

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

        var core: UIPanelAPI? = null

        var docked = false

        //Try to check if a dialog is open, and grab the CoreUI from it (Relevant for when refit is opened while docked to a colony, etc)
        val dialog = ReflectionUtils.invoke("getEncounterDialog", state)
        if (dialog != null)
        {
            docked = true
            core = ReflectionUtils.invoke("getCoreUI", dialog) as UIPanelAPI?
        }

        //If no dialog is open, just grab the UI Root from the CampaignState
        if (core == null) {
            core = ReflectionUtils.invoke("getCore", state) as UIPanelAPI?
        }

        //Return if neithr exists
        if (core == null) return

        //Look for the weapon picker dialog at the root of the current UI tree
        val weaponDialogPanels = core.getChildrenCopy().filter { ReflectionUtils.hasMethodOfName("getPickedWeaponSpec", it) }

        for(weaponDialogPanel in weaponDialogPanels){
            if (weaponDialogPanel is UIPanelAPI) {
                val innerWeaponPanel = ReflectionUtils.invoke("getInnerPanel", weaponDialogPanel) as UIPanelAPI
                if (dockedPanel == null || dockedPanel!! !in innerWeaponPanel.getChildrenCopy()) {
                    val creator = PanelCreator(weaponDialogPanel, true, docked)
                    dockedPanel = creator.init()
                }
            }
        }
    }
}