package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.ReflectionUtils.getFieldsMatching
import org.starficz.refitfilters.ReflectionUtils.getFieldsWithMethodsMatching
import org.starficz.refitfilters.RFSettings
import org.starficz.starui.Alignment
import org.starficz.starui.Anchor
import org.starficz.starui.components.AreaCheckbox
import org.starficz.starui.components.CustomPanel
import org.starficz.starui.components.Row
import org.starficz.starui.components.Text
import org.starficz.starui.modifier.*
import org.starficz.starui.mutableStateOf
import org.starficz.starui.Size
import org.starficz.starui.vanilla.*
import java.awt.datatransfer.DataFlavor
import java.awt.Toolkit

fun UIPanelAPI.createSearchBarFilterPanel(
    targetWidth: Float,
    targetHeight: Float,
    pickerPanel: UIPanelAPI,
    filterData: FilterData
): CustomPanelAPI {
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
    val caretVisible = mutableStateOf(false)

    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    return Row(
        Size.exact(targetWidth, targetHeight), Modifier.refreshPickerOn(pickerPanel, filterData.currentSearch),
        verticalAlignment = Alignment.CenterVertically, spacing = 1f
    ) {
        // Kept checked for vanilla's lit look; colours and font: the filter theme.
        AreaCheckbox(
            "RESET FILTERS", size = Size.fillMaxHeight(width = 125f),
            modifier = Modifier.tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) {
                addPara("Reset all filters. You can also press CTRL + R or the Middle Mouse Button to do the same.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "CTRL + R", "Middle Mouse Button")
            }
        ).also { button ->
            button.isChecked = true
            button.onClick {
                resetFilters(pickerPanel)
                button.isChecked = true
            }
        }

        // The field's panel, for the key handlers (they get the event, not the panel).
        lateinit var searchField: CustomPanelAPI

        fun appendCharIfPossible(char: Char): Boolean {
            val hasRoomToAppend = measureTextSize(filterData.currentSearch.value + char).first < (searchField.width - 15f)
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

        /** Typing (not shortcuts, number keys or Enter, which belong to the game) goes into the search. */
        fun isTyping(event: InputEventAPI) = !event.isCtrlDown && !event.isAltDown && event.eventValue !in (2..11) &&
            event.eventValue != Keyboard.KEY_RETURN && event.eventValue != Keyboard.KEY_NUMPADENTER

        val blinkInterval = IntervalUtil(0.5f, 0.5f)

        // Takes every key press while the picker is open: no click to focus it first.
        val searchFieldModifier = Modifier
            .tooltip(450f, TooltipMakerAPI.TooltipLocation.ABOVE) {
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
            .background(Misc.getDarkPlayerColor().darker())
            .border(Misc.getDarkPlayerColor())
            .onAdvance { amount ->
                blinkInterval.advance(amount)
                if (blinkInterval.intervalElapsed()) caretVisible.value = !caretVisible.value
                if (Mouse.isButtonDown(2)) {
                    resetFilters(pickerPanel)
                }
            }
            .onKeyDown { event ->
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
                else if (isTyping(event)) {
                    if (appendCharIfPossible(event.eventChar)) event.consume()
                }
            }
            .onKeyHeld { event ->
                if (event.eventValue == Keyboard.KEY_BACK) {
                    if (event.isShiftDown) deleteAll(event)
                    else if (event.isCtrlDown) deleteLastWord(event)
                    else deleteCharIfPossible(event)
                    event.consume()
                } else if (isTyping(event)) {
                    if (appendCharIfPossible(event.eventChar)) event.consume()
                }
            }

        // Fills the rest of the row. Its builder reads the search text and the caret, so it re-runs (updating the
        // same native label in place) whenever either changes.
        searchField = CustomPanel(Size.fillMaxSize(), searchFieldModifier) {
            val caret = if (caretVisible.value) "|" else " "
            Text(
                text = "${filterData.currentSearch.value}$caret",
                color = Misc.getBasePlayerColor(),
                modifier = Modifier.anchor(Anchor.inside.centerLeft.ofParent(5f))
            )
        }
    }
}
