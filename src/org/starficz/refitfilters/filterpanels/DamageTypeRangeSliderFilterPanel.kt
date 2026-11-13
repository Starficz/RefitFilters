package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.linMap
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.RFSettings
import org.starficz.refitfilters.WeaponFilterData
import org.starficz.starui.Alignment
import org.starficz.starui.Anchor
import org.starficz.starui.components.*
import org.starficz.starui.modifier.*
import org.starficz.starui.Size
import org.starficz.starui.TextAlign
import org.starficz.starui.UIState
import org.starficz.starui.vanilla.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

fun UIPanelAPI.createDamageTypeRangeSliderFilterPanel(
    targetWidth: Float,
    targetHeight: Float,
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

    val refresh = with(filterData) {
        Modifier.refreshPickerOn(pickerPanel, kineticDamage, heDamage, energyDamage, fragDamage, lowerRange, upperRange)
    }

    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    return Row(Size.exact(targetWidth, targetHeight), refresh, verticalAlignment = Alignment.CenterVertically, spacing = 1f) {
        val damageTypeGroup = ButtonGroup()

        fun createDamageTypeButton(iconPath: String, typeColor: Color, filterState: UIState<Boolean>, tooltipText: String) {
            // Reads filterState, so this box re-runs when the type is toggled: the checkbox and the icon stacked on
            // it are the same native objects each time (StarUI reuses them), only their look is updated.
            Box(Size.exact(targetHeight, targetHeight)) {
                val enabled = filterState.value
                val btn = AreaCheckbox(
                    "", filterState, Size.exact(targetHeight, targetHeight),
                    Modifier.tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara(tooltipText, 0f) },
                    baseColor = typeColor.darker(), darkColor = typeColor.darker().darker(), brightColor = typeColor,
                    buttonGroup = damageTypeGroup
                )
                val img = Image(iconPath, Size.exact(targetHeight, targetHeight))

                btn.glowBrightness = if (enabled) 0.5f else 1f
                if (enabled) {
                    img.sprite.setAdditiveBlend()
                    img.sprite.alphaMult = 0.9f
                    img.sprite.color = typeColor
                } else {
                    img.sprite.setNormalBlend()
                    img.sprite.alphaMult = 1f
                    img.sprite.color = typeColor.darker()
                }
            }
        }

        createDamageTypeButton(kineticIconPath, keColor, filterData.kineticDamage, "Show kinetic damage.")
        createDamageTypeButton(highExplosiveIconPath, heColor, filterData.heDamage, "Show high explosive damage.")
        createDamageTypeButton(energyIconPath, energyColor, filterData.energyDamage, "Show energy damage.")
        createDamageTypeButton(fragmentationIconPath, fragColor, filterData.fragDamage, "Show fragmentation damage.")

        Text("RANGE",
            modifier = Modifier.padding(horizontal = 6f, vertical = 0f),
            font = Font.VICTOR_14,
            color = Misc.getTextColor(),
            textAlign = TextAlign.Center
        )

        RangeSlider(filterData, minRange, maxRange, rangeIncrement, rangeTickSnapping)
    }
}

/**
 * Two handles on one bar of ticks [rangeIncrement] apart; a click or drag moves the handle nearer the cursor. Drawn
 * by hand: StarUI's Slider is vanilla's single-value slider.
 */
private fun UIPanelAPI.RangeSlider(filterData: FilterData, minRange: Int, maxRange: Int, rangeIncrement: Int, rangeTickSnapping: Boolean) {
    var lowerNodeHovered = false
    var upperNodeHovered = false

    val nodeColor = Misc.getHighlightColor()
    val lineBrightColor = nodeColor.darker()
    val lineDarkColor = lineBrightColor.darker().darker()

    val minBarRightPad = 18f
    val minBarLeftPad = 12f
    val barYPad = 2f
    val barThickness = 2f
    val tickThickness = 2f
    val tickHeight = 5f
    val nodeSize = 4f

    val numOfTicks = ((maxRange - minRange) / rangeIncrement)
    val maxSliderRange = minRange + numOfTicks * rangeIncrement
    val sliderTickRanges = (0..numOfTicks).map { it * rangeIncrement + minRange }

    // Whole pixels, so every tick is drawn the same width.
    fun tickXOffsets(width: Float): List<Int> {
        val spacing = (width - minBarLeftPad - minBarRightPad - tickThickness) / numOfTicks.toFloat()
        return (0..numOfTicks).map { (it * spacing).toInt() }
    }

    fun mapRangeToXOffset(range: Float, offsets: List<Int>): Float {
        val rangeXOffset: Float = range.linMap(minRange, maxSliderRange, offsets.first(), offsets.last())
        val nodeSnappedXOffset = offsets.minByOrNull { abs(rangeXOffset - it) }!!.toFloat()
        return if (rangeTickSnapping) nodeSnappedXOffset else rangeXOffset
    }

    fun mapXPosToRange(xPos: Float, nodeStartCenterXPos: Float, offsets: List<Int>): Float {
        val range: Float = xPos.linMap(offsets.first() + nodeStartCenterXPos,
            offsets.last() + nodeStartCenterXPos, minRange, maxSliderRange)
        val snappedRange = sliderTickRanges.minByOrNull { abs(range - it) }!!
        return if (rangeTickSnapping) snappedRange.toFloat() else range
    }

    // The panel, for the mouse handlers (they get the event, not the panel).
    lateinit var slider: CustomPanelAPI

    /** Whether the lower handle is the one nearer [event] (a tie goes to the lower one). */
    fun lowerIsNearer(event: InputEventAPI, offsets: List<Int>): Boolean {
        val nodeStartCenterXPos = slider.left + minBarLeftPad + tickThickness / 2
        val lowerNodeXPos = mapRangeToXOffset(filterData.lowerRange.value, offsets) + nodeStartCenterXPos
        val upperNodeXPos = mapRangeToXOffset(filterData.upperRange.value, offsets) + nodeStartCenterXPos
        return abs(lowerNodeXPos - event.x) <= abs(upperNodeXPos - event.x)
    }

    // Four tick labels (each with its tick's index), placed once the slider's width is known (onLayout).
    val labelledTicks = listOf(0, ((sliderTickRanges.size - 1) / 3f).roundToInt(),
        ((sliderTickRanges.size - 1) * 2 / 3f).roundToInt(), sliderTickRanges.size - 1)
    val tickLabels = mutableListOf<Pair<LabelAPI, Int>>()

    val sliderModifier = Modifier
        .tooltip(370f, TooltipMakerAPI.TooltipLocation.ABOVE) {
            addPara("Filters based on non-modified base range.", 0f)
            addPara("Click anywhere within the slider to move the nearest point.", 0f)
            addPara("Setting the maximum to $maxRange makes the upper limit infinite.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "$maxRange")
        }
        // The black backing reaches 4 px below the panel (onRenderBelow), so clicks there count too.
        .hitBoxPadding(bottom = 4f)
        // After every layout pass: the tick positions depend on the slider's width.
        .onLayout {
            val offsets = tickXOffsets(width)
            val startX = minBarLeftPad + tickThickness / 2 - 1
            for ((label, tick) in tickLabels) {
                label.applyAnchor(Anchor.inside.topLeft.ofParent(startX + offsets[tick] - label.width / 2, 0f))
            }
        }
        .onRenderBelow { alphaMult ->
            glColor(Color.BLACK, alphaMult)
            GL11.glRectf(left, bottom - 4, right, top)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }
        .onRender { alphaMult ->
            val offsets = tickXOffsets(width)
            val bottomOfBar = bottom + barYPad
            val topOfBar = bottomOfBar + barThickness
            val leftOfBar = left + minBarLeftPad
            val rightOfBar = leftOfBar + offsets.last() + tickThickness

            glColor(lineDarkColor, alphaMult, false)
            GL11.glRectf(leftOfBar, bottomOfBar, rightOfBar, topOfBar)

            for (tickXOffset in offsets) {
                GL11.glRectf(leftOfBar + tickXOffset.toFloat(), topOfBar,
                    leftOfBar + tickXOffset + tickThickness, topOfBar + tickHeight)
            }

            val nodeStartXPos = left + minBarLeftPad + tickThickness / 2 - nodeSize / 2
            val lowerNodeXPos = mapRangeToXOffset(filterData.lowerRange.value, offsets) + nodeStartXPos
            val upperNodeXPos = mapRangeToXOffset(filterData.upperRange.value, offsets) + nodeStartXPos

            glColor(lineBrightColor, alphaMult, false)
            GL11.glRectf(lowerNodeXPos + nodeSize, bottomOfBar, upperNodeXPos, topOfBar)

            glColor(if (lowerNodeHovered) nodeColor.brighter() else nodeColor, alphaMult, false)
            GL11.glRectf(lowerNodeXPos, bottomOfBar + barThickness / 2 - nodeSize / 2,
                lowerNodeXPos + nodeSize, bottomOfBar + barThickness / 2 + nodeSize / 2)

            glColor(if (upperNodeHovered) nodeColor.brighter() else nodeColor, alphaMult, false)
            GL11.glRectf(upperNodeXPos, bottomOfBar + barThickness / 2 - nodeSize / 2,
                upperNodeXPos + nodeSize, bottomOfBar + barThickness / 2 + nodeSize / 2)

            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }
        .onClick { playSound("ui_button_pressed", 1f, 1f) }
        .onHover { event ->
            val lower = lowerIsNearer(event, tickXOffsets(slider.width))
            // the click sound of moving from one handle to the other
            if (lower && upperNodeHovered || !lower && lowerNodeHovered) playSound("ui_button_mouseover", 1f, 0.8f)
            lowerNodeHovered = lower
            upperNodeHovered = !lower
        }
        .onHoverExit {
            lowerNodeHovered = false
            upperNodeHovered = false
        }
        .onMouseHeld { event ->
            val offsets = tickXOffsets(slider.width)
            val nodeStartCenterXPos = slider.left + minBarLeftPad + tickThickness / 2
            var newLower = filterData.lowerRange.value
            var newUpper = filterData.upperRange.value

            if (lowerIsNearer(event, offsets)) newLower = mapXPosToRange(event.x.toFloat(), nodeStartCenterXPos, offsets)
            else newUpper = mapXPosToRange(event.x.toFloat(), nodeStartCenterXPos, offsets)

            if (abs(filterData.upperRange.value - newUpper) > rangeIncrement / 4 ||
                abs(filterData.lowerRange.value - newLower) > rangeIncrement / 4) {
                if (rangeTickSnapping) playSound("ui_number_scrolling", 1f, 0.8f)
                filterData.lowerRange.value = newLower
                filterData.upperRange.value = newUpper
            }
        }

    slider = CustomPanel(Size.fillMaxSize(), sliderModifier) {
        tickLabels.clear()
        for (tick in labelledTicks) {
            val value = sliderTickRanges[tick]
            val text = if (value < 10000) value.toString() else "${value / 1000}K"
            tickLabels += Text(text, font = Font.VICTOR_14, color = Misc.getTextColor()) to tick
        }
    }
}
