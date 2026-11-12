package org.starficz.refitfilters

import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CutStyle
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.starui.compose.Size
import org.starficz.starui.compose.components.Button
import org.starficz.starui.compose.modifier.Modifier
import org.starficz.starui.compose.modifier.anchor
import org.starficz.starui.compose.runtime.DynamicInjection
import org.starficz.starui.compose.runtime.StarUIManager
import org.starficz.starui.core.Anchor
import org.starficz.starui.core.Font
import org.starficz.starui.core.children
import org.starficz.refitfilters.ReflectionUtils.getMethodsMatching
import org.starficz.refitfilters.ReflectionUtils.invoke

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
            font = Font.ORBITRON_12_CONDENSED,
            align = Alignment.MID,
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
            font = Font.ORBITRON_12_CONDENSED,
            align = Alignment.MID,
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