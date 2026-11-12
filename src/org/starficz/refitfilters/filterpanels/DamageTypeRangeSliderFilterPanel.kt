package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.RFSettings
import org.starficz.refitfilters.WeaponFilterData
import org.starficz.starui.compose.ImageSize
import org.starficz.starui.compose.LayoutSize
import org.starficz.starui.compose.PanelSize
import org.starficz.starui.compose.Size
import org.starficz.starui.compose.components.AreaCheckbox
import org.starficz.starui.compose.components.CustomPanel
import org.starficz.starui.compose.components.Image
import org.starficz.starui.compose.components.Row
import org.starficz.starui.compose.components.Text
import org.starficz.starui.compose.layout.VerticalAlignment
import org.starficz.starui.compose.modifier.Modifier
import org.starficz.starui.compose.modifier.anchor
import org.starficz.starui.compose.modifier.tooltip
import org.starficz.starui.compose.runtime.ButtonGroup
import org.starficz.starui.compose.runtime.StarUIPanelPlugin
import org.starficz.starui.compose.runtime.UIState
import org.starficz.starui.core.Anchor
import com.fs.starfarer.api.ui.LabelAPI
import org.starficz.starui.core.Font
import org.starficz.starui.core.applyAnchor
import org.starficz.starui.core.bottom
import org.starficz.starui.core.left
import org.starficz.starui.core.right
import org.starficz.starui.core.top
import org.starficz.starui.core.width
import org.starficz.starui.linMap
import org.starficz.starui.playSound
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

    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    return Row(LayoutSize.exact(targetWidth, targetHeight), verticalAlignment = VerticalAlignment.Center, spacing = 1f) {
        with(filterData) {
            refreshPickerOn(pickerPanel, kineticDamage, heDamage, energyDamage, fragDamage, lowerRange, upperRange)
        }

        val damageTypeGroup = ButtonGroup()

        fun createDamageTypeButton(iconPath: String, typeColor: Color, filterState: UIState<Boolean>, tooltipText: String) {
            // Reads filterState, so this little panel re-runs when the type is toggled: the checkbox and icon are
            // the same native objects each time (StarUI reuses them), only their look is updated.
            CustomPanel(PanelSize.exact(targetHeight, targetHeight)) {
                val enabled = filterState.value
                val btn = AreaCheckbox(
                    "", Size.exact(targetHeight, targetHeight),
                    Modifier.anchor(Anchor.inside.center.ofParent())
                        .tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara(tooltipText, 0f) },
                    typeColor.darker(), typeColor.darker().darker(), typeColor,
                    bind = filterState, buttonGroup = damageTypeGroup
                )
                val img = Image(iconPath, ImageSize.exact(targetHeight, targetHeight), Modifier.anchor(Anchor.inside.center.ofParent()))

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

        Text("RANGE", Size.wrap(6f, 0f), font = Font.VICTOR_14, color = Misc.getTextColor())

        val sliderTooltip = Modifier.tooltip(370f, TooltipMakerAPI.TooltipLocation.ABOVE) {
            addPara("Filters based on non-modified base range.", 0f)
            addPara("Click anywhere within the slider to move the nearest point.", 0f)
            addPara("Setting the maximum to $maxRange makes the upper limit infinite.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "$maxRange")
        }

        CustomPanel(PanelSize.fill(), sliderTooltip) {
            val sliderPanel = this
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
            val maxSliderRange = minRange + numOfTicks * rangeIncrement
            val sliderTickRanges = (0..numOfTicks).map { it * rangeIncrement + minRange }

            fun tickSpacing() = (sliderPanel.width - minBarLeftPad - minBarRightPad - tickThickness) / numOfTicks.toFloat()

            fun tickXOffsets() = if (pixelPerfect) (0..numOfTicks).map { (it * tickSpacing()).toInt() }
                                 else (0..numOfTicks).map { (it * tickSpacing()).roundToInt() }

            fun startingTickXPos() = minBarLeftPad + tickThickness / 2 - 1

            fun mapRangeToXOffset(range: Float): Float {
                val offsets = tickXOffsets()
                val rangeXOffset: Float = range.linMap(minRange, maxSliderRange, offsets.first(), offsets.last())
                val nodeSnappedXOffset = offsets.minByOrNull { abs(rangeXOffset - it) }!!.toFloat()
                return if (rangeTickSnapping) nodeSnappedXOffset else rangeXOffset
            }

            fun mapXPosToRange(xPos: Float): Float {
                val offsets = tickXOffsets()
                val nodeStartCenterXPos = sliderPanel.left + minBarLeftPad + tickThickness / 2
                val range: Float = xPos.linMap(offsets.first() + nodeStartCenterXPos,
                    offsets.last() + nodeStartCenterXPos, minRange, maxSliderRange)
                val snappedRange = sliderTickRanges.minByOrNull { abs(range - it) }!!
                return if (rangeTickSnapping) snappedRange.toFloat() else range
            }

            // Tick labels are placed once the slider's width is known (onLayout below).
            fun createLabel(value: Int): LabelAPI {
                val str = if (value < 10000) value.toString() else "${value / 1000}K"
                return Text(str, font = Font.VICTOR_14, color = Misc.getTextColor())
            }

            val minLabel = createLabel(minRange)

            val oneThirdIndex = ((sliderTickRanges.size - 1) / 3f).roundToInt()
            val oneThirdLabel = createLabel(sliderTickRanges[oneThirdIndex])

            val twoThirdIndex = ((sliderTickRanges.size - 1) * 2 / 3f).roundToInt()
            val twoThirdLabel = createLabel(sliderTickRanges[twoThirdIndex])

            val maxLabel = createLabel(sliderTickRanges.last())

            with(sliderPanel.plugin as StarUIPanelPlugin) {
                setMouseCapturePad(0f, 0f, 0f, 4f)

                // After every layout pass: the tick positions depend on the slider's width.
                onLayout {
                    val offsets = tickXOffsets()
                    val startX = startingTickXPos()

                    minLabel.applyAnchor(Anchor.inside.topLeft.ofParent(startX + offsets[0] - minLabel.width / 2, 0f))
                    oneThirdLabel.applyAnchor(Anchor.inside.topLeft.ofParent(startX + offsets[oneThirdIndex] - oneThirdLabel.width / 2, 0f))
                    twoThirdLabel.applyAnchor(Anchor.inside.topLeft.ofParent(startX + offsets[twoThirdIndex] - twoThirdLabel.width / 2, 0f))
                    maxLabel.applyAnchor(Anchor.inside.topLeft.ofParent(startX + offsets.last() - maxLabel.width / 2, 0f))
                }

                renderBelow { alphaMult ->
                    glColor(Color.BLACK, alphaMult)
                    GL11.glRectf(left, bottom - 4, right, top)
                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                }

                render { alphaMult ->
                    val offsets = tickXOffsets()
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
                    val lowerNodeXPos = mapRangeToXOffset(filterData.lowerRange.value) + nodeStartXPos
                    val upperNodeXPos = mapRangeToXOffset(filterData.upperRange.value) + nodeStartXPos

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

                onClick { playSound("ui_button_pressed", 1f, 1f) }

                onHover { event ->
                    val nodeStartCenterXPos = left + minBarLeftPad + tickThickness / 2
                    val lowerNodeXPos = mapRangeToXOffset(filterData.lowerRange.value) + nodeStartCenterXPos
                    val upperNodeXPos = mapRangeToXOffset(filterData.upperRange.value) + nodeStartCenterXPos

                    if (abs(lowerNodeXPos - event.x.toFloat()) <= abs(upperNodeXPos - event.x.toFloat())) {
                        if (upperNodeHovered) playSound("ui_button_mouseover", 1f, 0.8f)
                        lowerNodeHovered = true
                        upperNodeHovered = false
                    } else {
                        if (lowerNodeHovered) playSound("ui_button_mouseover", 1f, 0.8f)
                        lowerNodeHovered = false
                        upperNodeHovered = true
                    }
                }

                onHoverExit {
                    lowerNodeHovered = false
                    upperNodeHovered = false
                }

                onMouseHeld { event ->
                    val nodeStartCenterXPos = left + minBarLeftPad + tickThickness / 2
                    val lowerNodeXPos = mapRangeToXOffset(filterData.lowerRange.value) + nodeStartCenterXPos
                    val upperNodeXPos = mapRangeToXOffset(filterData.upperRange.value) + nodeStartCenterXPos

                    var newLower = filterData.lowerRange.value
                    var newUpper = filterData.upperRange.value

                    if (abs(lowerNodeXPos - event.x.toFloat()) <= abs(upperNodeXPos - event.x.toFloat())) {
                        newLower = mapXPosToRange(event.x.toFloat())
                    } else {
                        newUpper = mapXPosToRange(event.x.toFloat())
                    }

                    if (abs(filterData.upperRange.value - newUpper) > rangeIncrement / 4 ||
                        abs(filterData.lowerRange.value - newLower) > rangeIncrement / 4) {
                        if (rangeTickSnapping) playSound("ui_number_scrolling", 1f, 0.8f)
                        filterData.lowerRange.value = newLower
                        filterData.upperRange.value = newUpper
                    }
                }
            }
        }
    }
}
