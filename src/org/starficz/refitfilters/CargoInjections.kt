package org.starficz.refitfilters

import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.CutStyle
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.ReflectionUtils.invoke
import org.starficz.starui.Anchor
import org.starficz.starui.components.Button
import org.starficz.starui.DynamicInjection
import org.starficz.starui.modifier.anchor
import org.starficz.starui.modifier.Modifier
import org.starficz.starui.Size
import org.starficz.starui.StarUIManager
import org.starficz.starui.vanilla.children

abstract class BaseCargoInjection : DynamicInjection {
    // Determine whether to fetch the player's side or the market's side
    abstract val displayMethodName: String

    override fun findTarget(): UIPanelAPI? {
        if (StarUIManager.currentCoreUITabID != CoreUITabId.CARGO) return null

        val root = StarUIManager.currentCoreUITabPanel ?: return null

        val tradePanel = root.invoke("getTradePanel") as? UIPanelAPI ?: return null

        val cargoPanel = tradePanel.invoke(displayMethodName) as? UIPanelAPI ?: return null

        if (cargoPanel.children.size <= 4) return null

        return cargoPanel
    }

    override fun UIPanelAPI.injectUI() {
        // Find the "Other" button (which is the last top filter button before the scroll panel)
        val specialBtn = this.invoke("getSpecial") as? UIComponentAPI ?: return

        // Create the first injected button, anchoring it 3 pixels to the right of "Other"
        val customFilter1 = Button(
            text = "Filter 1",
            size = Size.exact(80f, 18f), // Matches vanilla 18f height
            modifier = Modifier.anchor(Anchor.rightOf.sibling(specialBtn).matchingBottomEdge(1f)),
            cutStyle = CutStyle.TOP
        ) {
            // TODO: Add your filter toggle logic here
            // e.g. this.invoke("getFilteredCargo") and apply filters
        }

        // Create the second injected button, anchoring it 3 pixels to the right of the first injected button
        val customFilter2 = Button(
            text = "Filter 2",
            size = Size.exact(80f, 18f),
            modifier = Modifier.anchor(Anchor.rightOf.sibling(customFilter1).matchingBottomEdge(1f)),
            cutStyle = CutStyle.TOP
        ) {
            // TODO: Add your filter toggle logic here
        }
    }
}

class PlayerCargoInjection : BaseCargoInjection() {
    override val id = "RefitFilters_PlayerCargo"
    override val displayMethodName = "getPlayerCargoDisplay"
}

class EntityCargoInjection : BaseCargoInjection() {
    override val id = "RefitFilters_EntityCargo"
    override val displayMethodName = "getEntityCargoDisplay"
}