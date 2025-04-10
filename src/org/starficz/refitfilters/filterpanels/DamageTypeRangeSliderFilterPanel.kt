package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11
import org.starficz.UIFramework.*
import org.starficz.UIFramework.Font
import org.starficz.UIFramework.anchorInTopLeftOfParent
import org.starficz.UIFramework.anchorToPreviousMatchingCenter
import org.starficz.UIFramework.onClick
import org.starficz.refitfilters.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt


fun UIPanelAPI.createDamageTypeRangeSliderFilterPanel(
    width: Float,
    height: Float,
    pickerPanel: UIPanelAPI,
    filterData: FilterData
): CustomPanelAPI {

    val minRange = if(filterData is WeaponFilterData) RFSettings.weaponMinRange else RFSettings.fighterMinRange
    val maxRange = if(filterData is WeaponFilterData) RFSettings.weaponMaxRange else RFSettings.fighterMaxRange
    val rangeIncrement = if(filterData is WeaponFilterData) RFSettings.weaponRangeIncrement else RFSettings.fighterRangeIncrement
    val rangeTickSnapping = if(filterData is WeaponFilterData) RFSettings.weaponRangeTickSnapping else RFSettings.fighterRangeTickSnapping

    val kineticIconPath = "graphics/ui/icons/damagetype_kinetic.png"
    val highExplosiveIconPath = "graphics/ui/icons/damagetype_high_explosive.png"
    val energyIconPath = "graphics/ui/icons/damagetype_energy.png"
    val fragmentationIconPath = "graphics/ui/icons/damagetype_fragmentation.png"

    val keColor = Color(199,182,158)
    val heColor = Color(208,52,56)
    val energyColor = Color(125, 194, 255)
    val fragColor = Color(255, 255, 131)

    return CustomPanel(width, height) { plugin ->
        val damageTypeGroup = ButtonGroup()

        AreaCheckbox("", keColor.darker(), keColor.darker().darker(), keColor,
            height, height, flag = filterData.kineticDamage, buttonGroup = damageTypeGroup) {
            glowBrightness = if (filterData.kineticDamage.isEnabled) 0.5f else 1f

            anchorInTopLeftOfParent()

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show kinetic damage.", 0f)
            }
            onClick { PickerPanelHelpers.filtersChanged(pickerPanel) }
        }

        Image(kineticIconPath, height, height) {

            anchorToPreviousMatchingCenter()

            if(filterData.kineticDamage.isEnabled){
                sprite.setAdditiveBlend()
                sprite.alphaMult = 0.9f
                sprite.color = keColor
            } else{
                sprite.setNormalBlend()
                sprite.alphaMult = 1f
                sprite.color = keColor.darker()
            }
        }

        AreaCheckbox("", heColor.darker(), heColor.darker().darker(), heColor,
            height, height, flag = filterData.heDamage, buttonGroup = damageTypeGroup) {

            glowBrightness = if (filterData.heDamage.isEnabled) 0.5f else 1f

            anchorRightOfPreviousMatchingTop(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show high explosive damage.", 0f)
            }
            onClick { PickerPanelHelpers.filtersChanged(pickerPanel) }
        }
        Image(highExplosiveIconPath, height, height) {
            anchorToPreviousMatchingCenter()

            if(filterData.heDamage.isEnabled){
                sprite.setAdditiveBlend()
                sprite.alphaMult = 0.9f
                sprite.color = heColor
            } else{
                sprite.setNormalBlend()
                sprite.alphaMult = 1f
                sprite.color = heColor.darker()
            }
        }

        AreaCheckbox("", energyColor.darker(), energyColor.darker().darker(), energyColor,
            height, height, flag = filterData.energyDamage, buttonGroup = damageTypeGroup) {
            glowBrightness = if (filterData.energyDamage.isEnabled) 0.5f else 1f

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show energy damage.", 0f)
            }
            onClick { PickerPanelHelpers.filtersChanged(pickerPanel) }
        }
        Image(energyIconPath, height, height) {
            anchorToPreviousMatchingCenter()

            if(filterData.energyDamage.isEnabled){
                sprite.setAdditiveBlend()
                sprite.alphaMult = 0.9f
                sprite.color = energyColor
            } else{
                sprite.setNormalBlend()
                sprite.alphaMult = 1f
                sprite.color = energyColor.darker()
            }
        }

        AreaCheckbox("", fragColor.darker(), fragColor.darker().darker(), fragColor,
            height, height, flag = filterData.fragDamage, buttonGroup = damageTypeGroup) {
            glowBrightness = if (filterData.fragDamage.isEnabled) 0.5f else 1f

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show fragmentation damage.", 0f)
            }
            onClick { PickerPanelHelpers.filtersChanged(pickerPanel) }
        }
        Image(fragmentationIconPath, height, height) {
            anchorToPreviousMatchingCenter()

            if(filterData.fragDamage.isEnabled){
                sprite.setAdditiveBlend()
                sprite.alphaMult = 0.9f
                sprite.color = fragColor
            } else{
                sprite.setNormalBlend()
                sprite.alphaMult = 1f
                sprite.color = fragColor.darker()
            }
        }

        Text("RANGE", Font.VICTOR_14, Misc.getTextColor()) {
            anchorRightOfPreviousMatchingMid(7f)
        }

        // range slider
        CustomPanel(right-previousComponent!!.right, height) { PanelPlugin ->
            anchorRightOfPreviousMatchingMid(0f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE, 370f){
                addPara("Filters based on non-modified base range.", 0f)
                addPara("Click anywhere within the slider to move the nearest point.", 0f)
                addPara("Setting the maximum to $maxRange makes the upper limit infinite.",
                    0f, Misc.getTextColor(), Misc.getHighlightColor(), "$maxRange")
            }

            var lowerRange = filterData.lowerRange
            var upperRange = filterData.upperRange

            var lowerNodeHovered = false
            var upperNodeHovered = false

            val nodeColor = Misc.getHighlightColor()
            val lineBrightColor = nodeColor.darker()
            val lineDarkColor = lineBrightColor.darker().darker()

            val pixelPerfect = true

            val minBarRightPad = 18f
            val minBarLeftPad = 12f
            val barYPad = 2f
            val barThickness = 2f
            val tickThickness = 2f
            val tickHeight = 5f
            val nodeSize = 4f

            val numOfTicks = ((maxRange - minRange) / rangeIncrement)
            val maxSliderRange = minRange + numOfTicks*rangeIncrement
            val sliderTickRanges = (0..numOfTicks).map { it*rangeIncrement + minRange }

            val tickSpacing = (this.width - minBarLeftPad - minBarRightPad - tickThickness) / numOfTicks.toFloat()
            val tickXOffsets = if (pixelPerfect) (0..numOfTicks).map { it*tickSpacing.toInt() }
            else (0..numOfTicks).map { (it*tickSpacing).roundToInt() }

            fun mapRangeToXOffset(range: Float): Float {
                // map the range from min/max ranges to min/max xPos
                val rangeXOffset: Float = range.linMap(minRange, maxSliderRange, tickXOffsets.first(), tickXOffsets.last())

                // snap xPos to the closest tick if that setting is on
                val nodeSnappedXOffset = tickXOffsets.minByOrNull { abs(rangeXOffset - it) }!!.toFloat()

                return if (rangeTickSnapping) nodeSnappedXOffset else rangeXOffset
            }

            fun mapXPosToRange(xPos: Float): Float {
                val nodeStartCenterXPos = left + minBarLeftPad + tickThickness/2
                // map the range from min/max xPos to min/max range
                val range: Float = xPos.linMap(tickXOffsets.first() + nodeStartCenterXPos,
                    tickXOffsets.last() + nodeStartCenterXPos, minRange, maxSliderRange)

                // snap range to the closest tick if that setting is on
                val snappedRange = sliderTickRanges.minByOrNull { abs(range - it) }!!

                return if (rangeTickSnapping) snappedRange.toFloat() else range
            }

            with(PanelPlugin){
                renderBelow { alphaMult ->
                    glColor(Color.BLACK, alphaMult)
                    GL11.glRectf(left, bottom-4, right, top)
                }
                render { alphaMult ->
                    val bottomOfBar = bottom + barYPad
                    val topOfBar = bottomOfBar + barThickness
                    val leftOfBar = left + minBarLeftPad
                    val rightOfBar = leftOfBar + tickXOffsets.last() + tickThickness

                    // draw dark line
                    glColor(lineDarkColor, alphaMult, false)
                    GL11.glRectf(leftOfBar, bottomOfBar, rightOfBar, topOfBar)

                    // draw tick marks
                    for (tickXOffset in tickXOffsets){
                        GL11.glRectf(leftOfBar + tickXOffset.toFloat(), topOfBar,
                            leftOfBar + tickXOffset + tickThickness, topOfBar + tickHeight)
                    }

                    // draw bright line and nodes
                    val nodeStartXPos = left + minBarLeftPad + tickThickness/2 - nodeSize/2
                    val lowerNodeXPos = mapRangeToXOffset(lowerRange) + nodeStartXPos
                    val upperNodeXPos = mapRangeToXOffset(upperRange) + nodeStartXPos

                    glColor(lineBrightColor, alphaMult, false)
                    GL11.glRectf(lowerNodeXPos + nodeSize, bottomOfBar, upperNodeXPos, topOfBar)

                    glColor(if(lowerNodeHovered) nodeColor.brighter() else nodeColor, alphaMult, false)
                    GL11.glRectf(lowerNodeXPos, bottomOfBar + barThickness/2 - nodeSize/2,
                        lowerNodeXPos + nodeSize, bottomOfBar + barThickness/2 + nodeSize/2)

                    glColor(if(upperNodeHovered) nodeColor.brighter() else nodeColor, alphaMult, false)
                    GL11.glRectf(upperNodeXPos, bottomOfBar + barThickness/2 - nodeSize/2,
                        upperNodeXPos + nodeSize, bottomOfBar + barThickness/2 + nodeSize/2)
                }

                onClick {
                    Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f)
                }

                onHover { event ->
                    val nodeStartCenterXPos = left + minBarLeftPad + tickThickness/2
                    val lowerNodeXPos = mapRangeToXOffset(lowerRange) + nodeStartCenterXPos
                    val upperNodeXPos = mapRangeToXOffset(upperRange) + nodeStartCenterXPos

                    if (abs(lowerNodeXPos - event.x.toFloat()) <= abs(upperNodeXPos - event.x.toFloat())) {
                        if(upperNodeHovered)
                            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 0.8f)
                        lowerNodeHovered = true
                        upperNodeHovered = false
                    } else{
                        if(lowerNodeHovered)
                            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 0.8f)
                        lowerNodeHovered = false
                        upperNodeHovered = true
                    }
                }

                onHoverExit {
                    lowerNodeHovered = false
                    upperNodeHovered = false
                }

                onHeld { event ->
                    val nodeStartCenterXPos = left + minBarLeftPad + tickThickness/2
                    val lowerNodeXPos = mapRangeToXOffset(lowerRange) + nodeStartCenterXPos
                    val upperNodeXPos = mapRangeToXOffset(upperRange) + nodeStartCenterXPos

                    if (abs(lowerNodeXPos - event.x.toFloat()) <= abs(upperNodeXPos - event.x.toFloat())) {
                        lowerRange = mapXPosToRange(event.x.toFloat())
                    } else{
                        upperRange = mapXPosToRange(event.x.toFloat())
                    }

                    if(abs(filterData.upperRange - upperRange) > rangeIncrement /4 ||
                        abs(filterData.lowerRange - lowerRange) > rangeIncrement /4) {
                        if (rangeTickSnapping) Global.getSoundPlayer().playUISound("ui_number_scrolling", 1f, 0.8f)
                        filterData.lowerRange = lowerRange
                        filterData.upperRange = upperRange
                        PickerPanelHelpers.filtersChanged(pickerPanel)
                    }
                }
                inputCaptureBottomPad = 4f
            }

            val startingTickXPos = minBarLeftPad + tickThickness/2 - 1
            val minRangeString = minRange.let { if (it < 10000) it.toString() else "${it / 1000}K" }
            Text(minRangeString, Font.VICTOR_14, Misc.getTextColor()) {
                anchorInTopLeftOfParent(startingTickXPos - position.width/2, 0f)
            }

            val oneThirdIndex = ((sliderTickRanges.size - 1)/3f).roundToInt()
            val oneThirdString = sliderTickRanges[oneThirdIndex].let { if (it < 10000) it.toString() else "${it / 1000}K" }
            Text(oneThirdString, Font.VICTOR_14, Misc.getTextColor()) {
                anchorInTopLeftOfParent(startingTickXPos + tickXOffsets[oneThirdIndex] - position.width/2, 0f)
            }

            val twoThirdIndex = ((sliderTickRanges.size - 1) * 2 / 3f).roundToInt()
            val twoThirdsString = sliderTickRanges[twoThirdIndex].let { if (it < 10000) it.toString() else "${it / 1000}K" }
            Text(twoThirdsString, Font.VICTOR_14, Misc.getTextColor()) {
                anchorInTopLeftOfParent(startingTickXPos + tickXOffsets[twoThirdIndex] - position.width/2, 0f)
            }

            val maxRangeString = sliderTickRanges.last().let { if (it < 10000) it.toString() else "${it / 1000}K" }
            Text(maxRangeString, Font.VICTOR_14, Misc.getTextColor()) {
                anchorInTopLeftOfParent(startingTickXPos + tickXOffsets.last() - position.width/2, 0f)
            }
        }
    }
}

