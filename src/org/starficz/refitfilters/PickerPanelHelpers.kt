package org.starficz.refitfilters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.UIFramework.*
import org.starficz.UIFramework.ReflectionUtils.get
import org.starficz.UIFramework.ReflectionUtils.getFieldsMatching
import org.starficz.UIFramework.ReflectionUtils.invoke

object PickerPanelHelpers {
    fun filtersChanged(pickerPanel: UIPanelAPI){
        pickerPanel.invoke("notifyFilterChanged")
    }

    fun setPickerPanelHeight(height: Float, pickerPanel: UIPanelAPI) {
        val knownFloatNames = listOf("PAD", "ITEM_WIDTH", "ITEM_HEIGHT", "origXAlignOffset")
        val knownFloatValues = listOf(382f, 385f, 0f)

        val holoHeightField = pickerPanel.getFieldsMatching(type = Float::class.java)
            .singleOrNull { it.name !in knownFloatNames && it.get(pickerPanel) !in knownFloatValues }
            ?: throw Exception("Unable to differentiate weaponPickerDialog's obf float fields")

        holoHeightField.set(pickerPanel, height)
        pickerPanel.height = height

        // pin the weapons list in place
        pinPickerPanelYPos(pickerPanel)
    }

    fun pinPickerPanelYPos(pickerPanel: UIPanelAPI) {
        val lowRange = 40f + pickerPanel.height
        val highRange = Global.getSettings().screenHeight - 40f
        val pickerFixedTop = if(lowRange < highRange) (pickerPanel.centerY + 350f).coerceIn(lowRange, highRange)
                             else Global.getSettings().screenHeight/2 + 350f
        pickerPanel.yAlignOffset += pickerFixedTop - pickerPanel.top
    }

    fun <T: Any, R: FilterData> sortAndFilterList(
        uiList: UIComponentAPI,
        specClass: Class<T>,
        filterFunction: (T, R) -> Boolean,
        filterData: R,
        comparator: Comparator<Pair<Any, T>>,
        searchTerm: String,
        searchBehaviour: String // e.g., "Filter", "SortAndFilter", etc.
    ): List<Pair<Any, T>> {
        val individualItems = uiList.invoke("getItems") as List<*>

        val specPairs: List<Pair<Any, T>> = individualItems.mapNotNull { item ->
            val tooltip = item!!.invoke("getTooltip")!!
            item to tooltip.get(type = specClass) as T
        }

        var processedSpecPairs = specPairs.filter { (_, spec) ->!filterFunction(spec, filterData) }

        if (searchBehaviour != "Filter" && searchTerm.isNotBlank()) {
            processedSpecPairs = processedSpecPairs.sortedWith(comparator)
        }

        uiList.invoke("clear")
        for (pair in processedSpecPairs) {
            uiList.invoke("addItem", pair.first)
        }

        return processedSpecPairs
    }
}