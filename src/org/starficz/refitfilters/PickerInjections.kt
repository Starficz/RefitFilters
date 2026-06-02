package org.starficz.refitfilters

import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver
import org.starficz.staruiframework.DynamicInjection
import org.starficz.staruiframework.StarUIManager
import org.starficz.refitfilters.ReflectionUtils.invoke
import org.starficz.staruiframework.allChildrenWithMethod

private fun getPopupRootPanel(): UIPanelAPI? {
    val state = AppDriver.getInstance().currentState
    return if (state is CampaignState) {
        (state.invoke("getEncounterDialog")?.invoke("getCoreUI") ?: state.invoke("getCore")) as? UIPanelAPI
    } else if (state is TitleScreenState) {
        state.invoke("getScreenPanel") as? UIPanelAPI
    } else {
        null
    }
}


class WeaponPickerInjection : DynamicInjection {
    override val id = "RefitFilters_WeaponPicker"

    override fun findTarget(): UIPanelAPI? {
        val root = getPopupRootPanel() ?: return null
        val picker = root.allChildrenWithMethod("getPickedWeaponSpec").firstOrNull() as? UIPanelAPI
        return picker?.invoke("getInnerPanel") as? UIPanelAPI
    }

    override fun UIPanelAPI.injectUI() {
        val root = getPopupRootPanel() ?: return
        val picker = root.allChildrenWithMethod("getPickedWeaponSpec").firstOrNull() as? UIPanelAPI ?: return
        FilterPanelCreator.injectFilters(this, picker, PickerPanelType.Weapons)
    }
}

class FighterPickerInjection : DynamicInjection {
    override val id = "RefitFilters_FighterPicker"

    override fun findTarget(): UIPanelAPI? {
        val root = getPopupRootPanel() ?: return null
        if (StarUIManager.currentCoreUITabID != CoreUITabId.REFIT) return null
        val picker = root.allChildrenWithMethod("getPickedFighterSpec").firstOrNull() as? UIPanelAPI
        return picker?.invoke("getInnerPanel") as? UIPanelAPI
    }

    override fun UIPanelAPI.injectUI() {
        val root = getPopupRootPanel() ?: return
        val picker = root.allChildrenWithMethod("getPickedFighterSpec").firstOrNull() as? UIPanelAPI ?: return
        FilterPanelCreator.injectFilters(this, picker, PickerPanelType.Fighters)
    }
}