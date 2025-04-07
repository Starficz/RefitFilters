package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import org.starficz.refitfilters.FilterPanelCreator.weaponDialogPanel
import org.starficz.UIFramework.ReflectionUtils.invoke
import org.starficz.UIFramework.*
import org.starficz.UIFramework.Font
import org.starficz.UIFramework.anchorInLeftMiddleOfParent
import org.starficz.UIFramework.anchorRightOfPreviousMatchingMid
import org.starficz.UIFramework.onClick
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.FilterDataClass
import org.starficz.refitfilters.RFSettings
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor


fun UIPanelAPI.createSearchBarFilterPanel(width: Float, height: Float): CustomPanelAPI {

    val brightColor = Global.getSettings().basePlayerColor
    val baseColor = brightColor.darker()
    val bgColor = baseColor.darker().darker()

    fun resetFilters() {
        if (FilterData != FilterDataClass()) {
            FilterData = FilterDataClass()
            weaponDialogPanel.invoke("notifyFilterChanged")
        }
    }

    return CustomPanel(width, height) {
        AreaCheckbox("RESET FILTERS", baseColor, bgColor, brightColor, 125f, height, Font.VICTOR_14) {
            isChecked = true
            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE, 300f) {
                addPara("Reset all filters. You can also press CTRL + R or the Middle Mouse Button to do the same.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "CTRL + R", "Middle Mouse Button")
            }
            onClick { resetFilters()  }
        }
        CustomPanel(right-previousComponent!!.right-1, height) { plugin ->
            anchorRightOfPreviousMatchingMid(1f)

            val searchText = Text(FilterData.currentSearch + " ", color = Misc.getBasePlayerColor()){
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

                advance { amount ->
                    blinkInterval.advance(amount)
                    if (blinkInterval.intervalElapsed()) {
                        blink = !blink
                        if (blink) searchText.text = "${FilterData.currentSearch} "
                        else searchText.text = "${FilterData.currentSearch}|"
                    }
                    if (Mouse.isButtonDown(2)) { resetFilters() }
                }

                fun appendCharIfPossible(char: Char){
                    val hasRoomToAppend = searchText.computeTextWidth(searchText.text) < (width - 20)
                    val isValidChar = when(char){
                        '\u0000' -> false
                        '%' -> false
                        '$' -> false
                        else -> true
                    }

                    if (isValidChar && hasRoomToAppend){
                        playSound("ui_typer_type")
                        FilterData.currentSearch += char
                    } else{
                        playSound("ui_typer_buzz")
                    }
                }

                fun deleteCharIfPossible(event: InputEventAPI){
                    if (FilterData.currentSearch.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        FilterData.currentSearch = FilterData.currentSearch.dropLast(1)
                        weaponDialogPanel.invoke("notifyFilterChanged")
                    }
                }

                fun deleteLastWord(event: InputEventAPI) {
                    if (FilterData.currentSearch.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        val trimmed = FilterData.currentSearch.trim()
                        val words = trimmed.split(Regex("\\s+"))
                        FilterData.currentSearch = when {
                            words.size <= 1 -> ""
                            else -> words.dropLast(1).joinToString(" ")
                        }
                        weaponDialogPanel.invoke("notifyFilterChanged")
                    }
                }

                fun deleteAll(event: InputEventAPI) {
                    if (FilterData.currentSearch.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        FilterData.currentSearch = ""
                        weaponDialogPanel.invoke("notifyFilterChanged")
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
                        weaponDialogPanel.invoke("notifyFilterChanged")
                    }
                    else if (event.eventValue == Keyboard.KEY_R && event.isCtrlDown){
                        event.consume()
                        resetFilters()
                    }
                    else if (!event.isCtrlDown && !event.isAltDown && event.eventValue !in (2..11) &&
                        event.eventValue != Keyboard.KEY_RETURN && event.eventValue != Keyboard.KEY_NUMPADENTER)
                    {
                        appendCharIfPossible(event.eventChar)
                        event.consume()
                        weaponDialogPanel.invoke("notifyFilterChanged")
                    }
                }
            }
        }
    }
}
