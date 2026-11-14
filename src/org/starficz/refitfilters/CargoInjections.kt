package org.starficz.refitfilters

import com.fs.starfarer.api.ui.CutStyle
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.ReflectionUtils.invoke
import org.starficz.starui.Anchor
import org.starficz.starui.components.Button
import org.starficz.starui.components.Row
import org.starficz.starui.inject
import org.starficz.starui.modifier.anchor
import org.starficz.starui.modifier.Modifier
import org.starficz.starui.Size

/*
 * Cargo screen filters: work in progress, not active (nothing calls injectCargoFilters yet).
 *
 * To switch it on, add a PatchLib @After on the cargo grid's recreate(...) and call injectCargoFilters() on self:
 * recreate clearChildren()s the grid and builds it again, so the hook lands right after every rebuild (inject
 * also comes back by itself after a clear, so the grid's first build would do too). The grid's class is
 * obfuscated (0.98.5a-RC4: com.fs.starfarer.campaign.ui.void): match it by shape, subtype = UIPanelAPI with
 * methodMatches recreate (6 parameters) and getSpecial (0). Every cargo grid in the game is that class (trade,
 * storage, cargo pickers): to limit the buttons to the trade screen, check in the hook that self is the trade
 * panel's getPlayerCargoDisplay() or getEntityCargoDisplay().
 */
internal fun UIPanelAPI.injectCargoFilters() {
    // Find the "Other" button (the last top filter button before the scroll panel)
    val specialBtn = this.invoke("getSpecial") as? UIComponentAPI ?: return

    // the injected buttons sit to the right of "Other", bottom edges matched
    inject("refitfilters:cargo-filters", modifier = Modifier.anchor(Anchor.rightOf.sibling(specialBtn).matchingBottomEdge(1f))) {
        Row(spacing = 1f) {
            Button(
                text = "Filter 1",
                size = Size.exact(80f, 18f), // Matches vanilla 18f height
                cutStyle = CutStyle.TOP
            ) {
                // TODO: Add your filter toggle logic here
                // e.g. this.invoke("getFilteredCargo") and apply filters
            }
            Button(
                text = "Filter 2",
                size = Size.exact(80f, 18f),
                cutStyle = CutStyle.TOP
            ) {
                // TODO: Add your filter toggle logic here
            }
        }
    }
}
