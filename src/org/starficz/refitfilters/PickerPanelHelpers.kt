package org.starficz.refitfilters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.ReflectionUtils.invoke
import org.starficz.starui.compose.runtime.StarUIPanelPlugin
import org.starficz.starui.compose.runtime.UIState
import org.starficz.starui.core.centerY
import org.starficz.starui.core.height
import org.starficz.starui.core.top
import org.starficz.starui.core.yAlignOffset

object PickerPanelHelpers {
    fun filtersChanged(pickerPanel: UIPanelAPI){
        pickerPanel.invoke("notifyFilterChanged")
    }

    /**
     * Call inside a filter panel's builder: when any of [states] changes, the picker re-filters on this panel's next
     * advance. Not from the input handler itself: vanilla's notifyFilterChanged() rebuilds the picker's inner panel,
     * this panel included (the picker patches add fresh filter panels during that rebuild), and throwing away the
     * panel that is handling the click in the middle of input dispatch is asking for trouble.
     */
    fun CustomPanelAPI.refreshPickerOn(pickerPanel: UIPanelAPI, vararg states: UIState<*>) {
        var needsRefresh = false
        states.forEach { it.onChange { needsRefresh = true } }
        (plugin as StarUIPanelPlugin).advance {
            if (needsRefresh) {
                needsRefresh = false
                filtersChanged(pickerPanel)
            }
        }
    }

    fun pinPickerPanelYPos(pickerPanel: UIPanelAPI) {
        val lowRange = 40f + pickerPanel.height
        val highRange = Global.getSettings().screenHeight - 40f
        val pickerFixedTop = if(lowRange < highRange) (pickerPanel.centerY + 350f).coerceIn(lowRange, highRange)
                             else Global.getSettings().screenHeight/2 + 350f
        pickerPanel.yAlignOffset += pickerFixedTop - pickerPanel.top
    }
}