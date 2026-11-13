package org.starficz.refitfilters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.ReflectionUtils.invoke
import org.starficz.starui.modifier.Modifier
import org.starficz.starui.modifier.onAdvance
import org.starficz.starui.UIState
import org.starficz.starui.vanilla.centerY
import org.starficz.starui.vanilla.height
import org.starficz.starui.vanilla.top
import org.starficz.starui.vanilla.yAlignOffset

object PickerPanelHelpers {
    fun filtersChanged(pickerPanel: UIPanelAPI){
        pickerPanel.invoke("notifyFilterChanged")
    }

    /**
     * On a filter panel: when any of [states] has changed, the picker re-filters on the panel's next advance. Not from
     * the input handler itself: vanilla's notifyFilterChanged() rebuilds the picker's inner panel, this panel included
     * (the picker patches add fresh filter panels during that rebuild), and throwing away the panel that is handling
     * the click in the middle of input dispatch is asking for trouble. Compares a snapshot ([UIState.peek] does not
     * subscribe anything), so nothing outlives the panel.
     */
    fun Modifier.refreshPickerOn(pickerPanel: UIPanelAPI, vararg states: UIState<*>): Modifier {
        var seen = states.map { it.peek() }
        return onAdvance {
            val now = states.map { it.peek() }
            if (now != seen) {
                seen = now
                filtersChanged(pickerPanel)
            }
        }
    }

    /**
     * Puts the picker's top 350 px above its slot (inside the screen), whatever its height. Measured from the slot
     * button, not the picker: vanilla centres the picker on the slot and then pushes it back inside the screen, so a
     * tall picker's centre is no longer the slot's, and pinning from it moved the top a few px whenever the filters
     * shrank the list below vanilla's push.
     */
    fun pinPickerPanelYPos(pickerPanel: UIPanelAPI) {
        // both pickers: getSlotButton(), whose position vanilla centres them on (0.98.5a-RC4)
        val slotCenterY = (pickerPanel.invoke("getSlotButton") as? UIComponentAPI)?.position?.centerY ?: pickerPanel.centerY
        val lowRange = 40f + pickerPanel.height
        val highRange = Global.getSettings().screenHeight - 40f
        val pickerFixedTop = if(lowRange < highRange) (slotCenterY + 350f).coerceIn(lowRange, highRange)
                             else Global.getSettings().screenHeight/2 + 350f
        pickerPanel.yAlignOffset += pickerFixedTop - pickerPanel.top
    }
}