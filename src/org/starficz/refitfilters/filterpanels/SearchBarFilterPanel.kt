package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
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
import org.starficz.staruiframework.*
import org.starficz.refitfilters.ReflectionUtils.getFieldsMatching
import org.starficz.refitfilters.ReflectionUtils.getFieldsWithMethodsMatching
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.PickerPanelHelpers
import org.starficz.refitfilters.RFSettings
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

fun UIPanelAPI.createSearchBarFilterPanel(
    targetWidth: Float,
    targetHeight: Float,
    pickerPanel: UIPanelAPI,
    filterData: FilterData
): CustomPanelAPI {
    val brightColor = Global.getSettings().basePlayerColor
    val baseColor = brightColor.darker()
    val bgColor = baseColor.darker().darker()

    fun resetFilters(pickerPanel: UIPanelAPI) {
        val filterButtonPanel = pickerPanel.getFieldsWithMethodsMatching(numOfMethodParams = 2, methodName = "setChecked")
            .firstOrNull()?.get(pickerPanel)

        val filterButtons = filterButtonPanel?.getFieldsMatching(type = List::class.java)
            ?.mapNotNull { it.get(filterButtonPanel) as? List<*> }
            ?.firstOrNull { it.any { item -> item is ButtonAPI } } as List<ButtonAPI>?

        filterButtons?.forEach { if(it.isEnabled && !it.isChecked) it.isChecked = true }
        filterData.reset()
    }

    return HorizontalStackLayout(spacing = 1f, alignment = Alignment.LMID, minWidth = targetWidth, minHeight = targetHeight) {

        var needsRefresh = false
        Plugin {
            advance {
                if (needsRefresh) {
                    PickerPanelHelpers.filtersChanged(pickerPanel)
                    needsRefresh = false
                }
            }
        }

        filterData.currentSearch.onChange { needsRefresh = true }

        AreaCheckbox(125f, -1f, "RESET FILTERS", baseColor, bgColor, brightColor, font = Font.VICTOR_14) {
            isChecked = true
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) {
                addPara("Reset all filters. You can also press CTRL + R or the Middle Mouse Button to do the same.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "CTRL + R", "Middle Mouse Button")
            }
            onClick {
                resetFilters(pickerPanel)
                this.isChecked = true
            }
        }

        // neg width is flex
        CustomPanel(-1f, -1f) {

            // Pass the anchor directly into the DSL element to avoid double-anchoring!
            val searchText = Text(
                text = "${filterData.currentSearch.value} ",
                color = Misc.getBasePlayerColor(),
                anchor = Anchor.inside.centerLeft.ofParent(xMargin = 5f)
            )

            Tooltip(450f, TooltipMakerAPI.TooltipLocation.ABOVE) {
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

            Plugin {
                renderBelow { alphaMult ->
                    glColor(Misc.getDarkPlayerColor().darker(), alphaMult, false)
                    GL11.glRectf(left, bottom, right, top)
                    glColor(Misc.getDarkPlayerColor(), alphaMult, false)
                    drawBorder(left + 1f, bottom + 1f, right - 1f, top - 1f)

                    // Re-enable textures for safety!
                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                }

                val blinkInterval = IntervalUtil(0.5f, 0.5f)
                var blink = false

                advance { amount ->
                    blinkInterval.advance(amount)
                    if (blinkInterval.intervalElapsed()) {
                        blink = !blink
                        if (blink) searchText.text = "${filterData.currentSearch.value} "
                        else searchText.text = "${filterData.currentSearch.value}|"
                    }
                    if (Mouse.isButtonDown(2)) {
                        resetFilters(pickerPanel)
                    }
                }

                fun appendCharIfPossible(char: Char): Boolean {
                    val hasRoomToAppend = searchText.computeTextWidth(searchText.text) < (width - 15f)
                    val isValidChar = when (char) {
                        '\u0000', '%', '$' -> false
                        else -> true
                    }

                    if (isValidChar && hasRoomToAppend) {
                        playSound("ui_typer_type")
                        filterData.currentSearch.value += char
                        return true
                    } else {
                        playSound("ui_typer_buzz")
                        return false
                    }
                }

                fun deleteCharIfPossible(event: InputEventAPI) {
                    if (filterData.currentSearch.value.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        filterData.currentSearch.value = filterData.currentSearch.value.dropLast(1)
                    }
                }

                fun deleteLastWord(event: InputEventAPI) {
                    if (filterData.currentSearch.value.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        val trimmed = filterData.currentSearch.value.trim()
                        val words = trimmed.split(Regex("\\s+"))
                        filterData.currentSearch.value = when {
                            words.size <= 1 -> ""
                            else -> words.dropLast(1).joinToString(" ")
                        }
                    }
                }

                fun deleteAll(event: InputEventAPI) {
                    if (filterData.currentSearch.value.isNotEmpty()) {
                        event.consume()
                        playSound("ui_typer_type")
                        filterData.currentSearch.value = ""
                    }
                }

                onKeyDown { event ->
                    if (event.eventValue == Keyboard.KEY_ESCAPE) deleteAll(event)
                    else if (event.eventValue == Keyboard.KEY_BACK) {
                        if (event.isShiftDown) deleteAll(event)
                        else if (event.isCtrlDown) deleteLastWord(event)
                        else deleteCharIfPossible(event)
                    }
                    else if (event.eventValue == Keyboard.KEY_V && event.isCtrlDown) {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
                        for (char in clipboard) {
                            appendCharIfPossible(char)
                        }
                        event.consume()
                    }
                    else if (event.eventValue == Keyboard.KEY_R && event.isCtrlDown) {
                        event.consume()
                        resetFilters(pickerPanel)
                    }
                    else if (!event.isCtrlDown && !event.isAltDown && event.eventValue !in (2..11) &&
                        event.eventValue != Keyboard.KEY_RETURN && event.eventValue != Keyboard.KEY_NUMPADENTER) {
                        if (appendCharIfPossible(event.eventChar)) {
                            event.consume()
                        }
                    }
                }

                onKeyHeld { event ->
                    if (event.eventValue == Keyboard.KEY_BACK) {
                        if (event.isShiftDown) deleteAll(event)
                        else if (event.isCtrlDown) deleteLastWord(event)
                        else deleteCharIfPossible(event)
                        event.consume()
                    } else if (!event.isCtrlDown && !event.isAltDown && event.eventValue !in (2..11) &&
                        event.eventValue != Keyboard.KEY_RETURN && event.eventValue != Keyboard.KEY_NUMPADENTER) {
                        if (appendCharIfPossible(event.eventChar)) {
                            event.consume()
                        }
                    }
                }
            }
        }
    }
}