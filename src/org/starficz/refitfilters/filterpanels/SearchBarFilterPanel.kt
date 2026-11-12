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
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.RFSettings
import org.starficz.refitfilters.ReflectionUtils.getFieldsMatching
import org.starficz.refitfilters.ReflectionUtils.getFieldsWithMethodsMatching
import org.starficz.starui.compose.Axis
import org.starficz.starui.compose.LayoutSize
import org.starficz.starui.compose.PanelSize
import org.starficz.starui.compose.Size
import org.starficz.starui.compose.components.AreaCheckbox
import org.starficz.starui.compose.components.CustomPanel
import org.starficz.starui.compose.components.Row
import org.starficz.starui.compose.components.Text
import org.starficz.starui.compose.layout.VerticalAlignment
import org.starficz.starui.compose.modifier.Modifier
import org.starficz.starui.compose.modifier.anchor
import org.starficz.starui.compose.modifier.plugin
import org.starficz.starui.compose.modifier.tooltip
import org.starficz.starui.compose.runtime.UIState
import org.starficz.starui.core.Anchor
import org.starficz.starui.core.Font
import org.starficz.starui.core.bottom
import org.starficz.starui.core.left
import org.starficz.starui.core.measureTextSize
import org.starficz.starui.core.onClick
import org.starficz.starui.core.right
import org.starficz.starui.core.top
import org.starficz.starui.core.width
import org.starficz.starui.drawBorder
import org.starficz.starui.playSound
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

    // The caret blinks by flipping this state; the search text below reads it, so StarUI redraws the label.
    val caretVisible = UIState(false)

    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    return Row(LayoutSize.exact(targetWidth, targetHeight), verticalAlignment = VerticalAlignment.Center, spacing = 1f) {
        refreshPickerOn(pickerPanel, filterData.currentSearch)

        AreaCheckbox(
            "RESET FILTERS", Size.mixed(Axis.Exact(125f), Axis.Fill()),
            Modifier.tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) {
                addPara("Reset all filters. You can also press CTRL + R or the Middle Mouse Button to do the same.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "CTRL + R", "Middle Mouse Button")
            },
            baseColor, bgColor, brightColor, font = Font.VICTOR_14
        ).also { button ->
            button.isChecked = true
            button.onClick {
                resetFilters(pickerPanel)
                button.isChecked = true
            }
        }

        val searchFieldModifier = Modifier.tooltip(450f, TooltipMakerAPI.TooltipLocation.ABOVE) {
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
        }.plugin {
            renderBelow { alphaMult ->
                glColor(Misc.getDarkPlayerColor().darker(), alphaMult, false)
                GL11.glRectf(left, bottom, right, top)
                glColor(Misc.getDarkPlayerColor(), alphaMult, false)
                drawBorder(left + 1f, bottom + 1f, right - 1f, top - 1f)

                // Re-enable textures for safety!
                GL11.glEnable(GL11.GL_TEXTURE_2D)
            }

            val blinkInterval = IntervalUtil(0.5f, 0.5f)

            advance { amount ->
                blinkInterval.advance(amount)
                if (blinkInterval.intervalElapsed()) caretVisible.value = !caretVisible.value
                if (Mouse.isButtonDown(2)) {
                    resetFilters(pickerPanel)
                }
            }

            fun appendCharIfPossible(char: Char): Boolean {
                val hasRoomToAppend = measureTextSize(filterData.currentSearch.value + char).first < (width - 15f)
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

        // Fills the rest of the row. Its builder reads the search text and the caret, so it re-runs (updating the
        // same native label in place) whenever either changes.
        CustomPanel(PanelSize.fill(), searchFieldModifier) {
            val caret = if (caretVisible.value) "|" else " "
            Text(
                text = "${filterData.currentSearch.value}$caret",
                color = Misc.getBasePlayerColor(),
                modifier = Modifier.anchor(Anchor.inside.centerLeft.ofParent(5f))
            )
        }
    }
}
