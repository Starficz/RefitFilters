package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import org.starficz.UIFramework.*
import org.starficz.UIFramework.ReflectionUtils.getFieldsMatching
import org.starficz.UIFramework.ReflectionUtils.getFieldsWithMethodsMatching
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.PickerPanelHelpers
import org.starficz.refitfilters.RFSettings
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor


fun UIPanelAPI.createSearchBarFilterPanel(
    width: Float,
    height: Float,
    pickerPanel: UIPanelAPI,
    filterData: FilterData
): CustomPanelAPI {

    val brightColor = Global.getSettings().basePlayerColor
    val baseColor = brightColor.darker()
    val bgColor = baseColor.darker().darker()

    fun resetFilters(pickerPanel: UIPanelAPI) {
        val filterButtonPanel = pickerPanel.getFieldsWithMethodsMatching("setChecked", numOfMethodParams = 2)
            .firstOrNull()?.get(pickerPanel)

        val filterButtons = filterButtonPanel?.getFieldsMatching(type = List::class.java)
            ?.mapNotNull { it.get(filterButtonPanel) as? List<*> }
            ?.firstOrNull { it.any { item -> item is ButtonAPI } } as List<ButtonAPI>?

        filterButtons?.forEach { if(it.isEnabled && !it.isChecked) it.isChecked = true }
        filterData.reset()
    }

    return CustomPanel(width, height) {
        AreaCheckbox("RESET FILTERS", baseColor, bgColor, brightColor, 125f, height, Font.VICTOR_14) {
            isChecked = true
            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE, 300f) {
                addPara("Reset all filters. You can also press CTRL + R or the Middle Mouse Button to do the same.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "CTRL + R", "Middle Mouse Button")
            }
            onClick {
                resetFilters(pickerPanel)
                this.isChecked = true
                PickerPanelHelpers.filtersChanged(pickerPanel)
            }
        }

        // Custom TextField instead of vanilla TextField as a vanilla's implementation needs focus to work,
        // and having focus consumes all key presses, even escape. This makes the desired behaviour for this text field
        // of always being active, but allows for exiting the weapon picker panel and filter hotkeys impossible.
        // Also implements all the reset keys as this is a convenient place that already captures all input.
        CustomPanel(right-previousComponent!!.right-1, height) { plugin ->
            anchorRightOfPreviousMatchingMid(1f)

            val searchText = Text(filterData.currentSearch + " ", color = Misc.getBasePlayerColor()){
                anchorInLeftMiddleOfParent(5f)
            }

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE, 450f){
                val sortExtra = if (RFSettings.searchBarBehaviour == "Sort") "[x]" else ""
                val filterExtra = if (RFSettings.searchBarBehaviour == "Filter") "[x]" else ""
                val sortAndFilterExtra = if (RFSettings.searchBarBehaviour == "Sort & Filter") "[x]" else ""
                addPara("An automatically selected searchbar.", 0f)
                addPara("Press either ESC or Shift + Backspace to clear all of its contents.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "ESC", "Shift + Backspace")
                addPara("The searchbar has different modes that can be changed to within the mods configs, the [x] displays which one is currently active.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "x")
                addSpacer(10f)
                addPara("Sort - Sort the list based on the best match. $sortExtra", 0f,
                    Misc.getTextColor(), Misc.getHighlightColor(), "Sort", "x")
                addPara("Filter - Remove entries that do not match the prompt enough. $filterExtra", 0f,
                    Misc.getTextColor(), Misc.getHighlightColor(), "Filter", "x")
                addPara("Sort & Filter - Combined behaviour of the above. $sortAndFilterExtra", 0f,
                    Misc.getTextColor(), Misc.getHighlightColor(), "Sort & Filter", "x")
            }

            with(plugin){
                renderBelow { alphaMult ->
                    glColor(Misc.getDarkPlayerColor().darker(), alphaMult, false)
                    GL11.glRectf(left, bottom, right, top)
                    glColor(Misc.getDarkPlayerColor(), alphaMult, false)
                    drawBorder(left+1f, bottom+1f, right-1f, top-1f)
                }

                val blinkInterval = IntervalUtil(0.5f, 0.5f)
                var blink = false
                var needsRefresh = false

                advance { amount ->
                    blinkInterval.advance(amount)
                    if (blinkInterval.intervalElapsed()) {
                        blink = !blink
                        if (blink) searchText.text = "${filterData.currentSearch} "
                        else searchText.text = "${filterData.currentSearch}|"
                    }
                    if (Mouse.isButtonDown(2)) {
                        resetFilters(pickerPanel)
                        needsRefresh = true
                    }
                    // need to defer refresh to here as refreshing in plugin event listeners directly
                    // causes some weird race condition crash only on some machines
                    if (needsRefresh) {
                        needsRefresh = false
                        PickerPanelHelpers.filtersChanged(pickerPanel)
                    }
                }

                fun appendCharIfPossible(char: Char): Boolean{
                    val hasRoomToAppend = searchText.computeTextWidth(searchText.text) < (width - 20)
                    val isValidChar = when(char){
                        '\u0000' -> false
                        '%' -> false
                        '$' -> false
                        else -> true
                    }

                    if (isValidChar && hasRoomToAppend){
                        playSound("ui_typer_type")
                        filterData.currentSearch += char
                        needsRefresh = true
                        return true
                    } else{
                        playSound("ui_typer_buzz")
                        return false
                    }
                }

                fun deleteCharIfPossible(event: InputEventAPI){
                    if (filterData.currentSearch.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        filterData.currentSearch = filterData.currentSearch.dropLast(1)
                        needsRefresh = true
                    }
                }

                fun deleteLastWord(event: InputEventAPI) {
                    if (filterData.currentSearch.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        val trimmed = filterData.currentSearch.trim()
                        val words = trimmed.split(Regex("\\s+"))
                        filterData.currentSearch = when {
                            words.size <= 1 -> ""
                            else -> words.dropLast(1).joinToString(" ")
                        }
                        needsRefresh = true
                    }
                }

                fun deleteAll(event: InputEventAPI) {
                    if (filterData.currentSearch.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        filterData.currentSearch = ""
                        needsRefresh = true
                    }
                }

                onKeyDown { event ->
                    if (event.eventValue == Keyboard.KEY_ESCAPE) deleteAll(event)
                    else if (event.eventValue == Keyboard.KEY_BACK) {
                        if (event.isShiftDown) deleteAll(event)
                        else if (event.isCtrlDown) deleteLastWord(event)
                        else deleteCharIfPossible(event)
                    }
                    else if (event.eventValue == Keyboard.KEY_V && event.isCtrlDown){
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
                        for (char in clipboard) {
                            appendCharIfPossible(char)
                        }
                        event.consume()
                    }
                    else if (event.eventValue == Keyboard.KEY_R && event.isCtrlDown){
                        event.consume()
                        resetFilters(pickerPanel)
                        needsRefresh = true
                    }
                    else if (!event.isCtrlDown && !event.isAltDown && event.eventValue !in (2..11) &&
                        event.eventValue != Keyboard.KEY_RETURN && event.eventValue != Keyboard.KEY_NUMPADENTER)
                    {
                        if(appendCharIfPossible(event.eventChar)){
                            event.consume()
                        }
                    }
                }
            }
        }
    }
}
