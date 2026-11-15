package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11
import org.starficz.refitfilters.*
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.starui.*
import org.starficz.starui.Alignment
import org.starficz.starui.components.*
import org.starficz.starui.modifier.*
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

    val maxRange = if(filterData is WeaponFilterData) RFSettings.weaponMaxRange else RFSettings.fighterMaxRange
    val rangeIncrement = if(filterData is WeaponFilterData) RFSettings.weaponRangeIncrement else RFSettings.fighterRangeIncrement
    // snapping is one setting for the whole slider; the tick distance is a setting for range only
    val tickSnapping = if(filterData is WeaponFilterData) RFSettings.weaponRangeTickSnapping else RFSettings.fighterRangeTickSnapping

    val kineticIconPath = "graphics/ui/icons/damagetype_kinetic.png"
    val highExplosiveIconPath = "graphics/ui/icons/damagetype_high_explosive.png"
    val energyIconPath = "graphics/ui/icons/damagetype_energy.png"
    val fragmentationIconPath = "graphics/ui/icons/damagetype_fragmentation.png"

    val keColor = Color(199,182,158)
    val heColor = Color(208,52,56)
    val energyColor = Color(125, 194, 255)
    val fragColor = Color(255, 255, 131)

    val watched = with(filterData) { mutableListOf<UIState<*>>(kineticDamage, heDamage, energyDamage, fragDamage, lowerRange, upperRange) }
    if (filterData is WeaponFilterData) with(filterData) { watched += listOf(dpsLower, dpsUpper, dmgLower, dmgUpper, effLower, effUpper) }
    if (filterData is FighterFilterData) with(filterData) { watched += listOf(refitLower, refitUpper, sizeLower, sizeUpper) }
    val refresh = Modifier.refreshPickerOn(pickerPanel, *watched.toTypedArray())

    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    return Row(Size.exact(targetWidth, targetHeight), refresh, verticalAlignment = Alignment.CenterVertically, spacing = 1f) {
        val damageTypeGroup = FilterGroup()

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

        val range = StatSlider(SliderStat.RANGE, "range", filterData.lowerRange, filterData.upperRange, 0f, maxRange.toFloat(),
            rangeIncrement.toFloat(), tickSnapping, { v -> if (v < 10000) v.toInt().toString() else "${v.toInt() / 1000}K" },
            "Filters based on non-modified base range.")

        // the selector picks what the slider filters; each stat keeps its own limits
        fun scaled(stat: SliderStat, name: String, scale: StatScale, lower: UIState<Float>, upper: UIState<Float>,
                   format: (Float) -> String, tooltip: String, labelEvery: Int = 5) =
            StatSlider(stat, name, lower, upper, scale.min, scale.max, scale.step, tickSnapping, format, tooltip, labelEvery)
        val whole = { v: Float -> v.toInt().toString() }
        val stats = when (filterData) {
            is WeaponFilterData -> listOf(
                range,
                scaled(SliderStat.DPS, "DPS", StatScale.DPS, filterData.dpsLower, filterData.dpsUpper, whole,
                    "Filters based on sustained damage per second."),
                scaled(SliderStat.DMG, "damage per shot", StatScale.DMG, filterData.dmgLower, filterData.dmgUpper, whole,
                    "Filters based on damage per shot."),
                scaled(SliderStat.EFF, "flux efficiency", StatScale.EFF, filterData.effLower, filterData.effUpper, { v -> String.format("%.1f", v) },
                    "Filters based on flux efficiency: damage per point of flux."),
            )
            is FighterFilterData -> listOf(
                range,
                scaled(SliderStat.REFIT, "refit time", StatScale.REFIT, filterData.refitLower, filterData.refitUpper, whole,
                    "Filters based on refit time: the seconds a lost fighter takes to replace."),
                scaled(SliderStat.SIZE, "wing size", StatScale.SIZE, filterData.sizeLower, filterData.sizeUpper, whole,
                    "Filters based on wing size: fighters per wing.", labelEvery = 1),
            )
            else -> listOf(range)
        }
        Row(Size.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, spacing = 1f) {
            val shown = stats.firstOrNull { it.stat == filterData.sliderStat.value } ?: range
            StatSelector(filterData, stats, targetHeight)
            RangeSlider(shown)
        }
    }
}

/** One stat the slider can show: its own range states, bounds, tick step and label format. */
class StatSlider(
    val stat: SliderStat, /** For the selector's tooltip. */ val name: String, val lower: UIState<Float>, val upper: UIState<Float>,
    val min: Float, val max: Float, val step: Float, val snapping: Boolean, val format: (Float) -> String, val tooltip: String,
    /** Label every this many ticks when that ends on the last one (see RangeSlider); null: the ends and the thirds. */
    val labelEvery: Int? = null
) {
    val filtered: Boolean get() = lower.value > min || upper.value < max
}

/**
 * The stat selector (a selector, not a filter toggle): the sort strip's dark fill in a frame, the stat between two
 * small arrows, the pips in their own band below. Click the right half for the next stat, the left half or right-click
 * for the one before. Pips: bright for the stat shown, yellow for every stat that filters.
 */
private fun UIPanelAPI.StatSelector(filterData: FilterData, stats: List<StatSlider>, height: Float) {
    val width = 60f
    fun step(by: Int) {
        val all = stats.map { it.stat }
        val at = all.indexOf(filterData.sliderStat.value).coerceAtLeast(0)
        filterData.sliderStat.value = all[(at + by + all.size) % all.size]
        playSound("ui_button_pressed", 1f, 1f)
    }
    /** A 3 x 6 px arrowhead from whole-pixel columns (6, 4, 2 tall), so both sides come out the same. */
    fun arrow(tipX: Float, centerY: Float, pointsRight: Boolean) {
        val cx = tipX.toInt().toFloat()
        val cy = centerY.toInt().toFloat()
        for (i in 0..2) {
            val half = 3f - i                        // heights 6, 4, 2 around the line between two pixel rows
            val x = if (pointsRight) cx - 2 + i else cx + 2 - i
            GL11.glRectf(x, cy + 1f - half, x + 1f, cy + 1f + half)
        }
    }
    lateinit var slider: CustomPanelAPI
    val look = Modifier
        .tooltip(330f, TooltipMakerAPI.TooltipLocation.ABOVE) {
            val names = stats.map { it.name }
            addPara("Choose what the slider filters: ${names.dropLast(1).joinToString(", ")} or ${names.last()}.", 0f)
            addPara("Click the right side for the next one, the left side or right-click for the one before.", 0f)
            addPara("Every stat keeps its own limits, and all of them filter at once. A yellow mark below the name means that stat is filtering.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), "yellow mark")
        }
        .onRenderBelow { alphaMult ->
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            glColor(Misc.getDarkPlayerColor().darker().darker(), alphaMult, false)
            GL11.glRectf(left, bottom, right, top)
            if (isHovering) {
                // the half a click would use lights up, fading out towards the other side: left goes back, right forward.
                // The cursor is read now, not kept from hover events: a click rebuilds this panel, and a kept position
                // would start over (one frame of the wrong side after every click on the right).
                val leftSide = Global.getSettings().mouseX < left + this.width / 2f
                val c = Misc.getDarkPlayerColor()
                val (r, g, b) = Triple(c.red / 255f, c.green / 255f, c.blue / 255f)
                val litX = if (leftSide) left else right
                val fadedX = if (leftSide) right else left
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                GL11.glBegin(GL11.GL_QUADS)
                GL11.glColor4f(r, g, b, alphaMult); GL11.glVertex2f(litX, bottom); GL11.glVertex2f(litX, top)
                GL11.glColor4f(r, g, b, 0f); GL11.glVertex2f(fadedX, top); GL11.glVertex2f(fadedX, bottom)
                GL11.glEnd()
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }
        .border(Misc.getDarkPlayerColor())
        .onRender { alphaMult ->
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            // the arrows, level with the label
            val cy = top - 10.5f
            glColor(Misc.getBasePlayerColor(), alphaMult * 0.8f, false)
            arrow(left + 4f, cy, false)
            arrow(right - 5f, cy, true)
            // the pips
            val pipW = 5f; val pipH = 2f; val gap = 3f
            val total = stats.size * pipW + (stats.size - 1) * gap
            var x = left + ((this.width - total) / 2f).toInt()
            val y = bottom + 5f   // centred between the label and the frame
            for (s in stats) {
                val color = when {
                    s.filtered -> Misc.getHighlightColor()
                    s.stat == filterData.sliderStat.value -> Misc.getBrightPlayerColor()
                    else -> Misc.getDarkPlayerColor()
                }
                glColor(color, alphaMult, false)
                GL11.glRectf(x, y, x + pipW, y + pipH)
                x += pipW + gap
            }
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }
        .onClick { event ->
            if (event.isRMBDownEvent) step(-1)
            else if (event.isLMBDownEvent) step(if (event.x < slider.left + slider.width / 2f) -1 else 1)
        }
    slider = Box(Size.exact(width, height), look) {
        Text(filterData.sliderStat.value.label, font = Font.VICTOR_14, color = Misc.getBasePlayerColor(),
            modifier = Modifier.align(Alignment.TopCenter).margin(top = 1f))   // its line box puts the glyphs 7 px down, level with the arrows
    }
}

/**
 * Two handles on one bar of ticks [rangeIncrement] apart; a click or drag moves the handle nearer the cursor. Drawn
 * by hand: StarUI's Slider is vanilla's single-value slider.
 */
private fun UIPanelAPI.RangeSlider(stat: StatSlider) {
    val minRange = stat.min
    val maxRange = stat.max
    val rangeIncrement = stat.step
    val rangeTickSnapping = stat.snapping
    val lowerState = stat.lower
    val upperState = stat.upper
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

    val numOfTicks = ((maxRange - minRange) / rangeIncrement).roundToInt()
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
        val lowerNodeXPos = mapRangeToXOffset(lowerState.value, offsets) + nodeStartCenterXPos
        val upperNodeXPos = mapRangeToXOffset(upperState.value, offsets) + nodeStartCenterXPos
        return abs(lowerNodeXPos - event.x) <= abs(upperNodeXPos - event.x)
    }

    // Four tick labels (each with its tick's index), placed once the slider's width is known (onLayout).
    // Every labelEvery ticks only when that ends on the last tick and stays readable (the tops are settings, so any
    // count of ticks can come up); else the ends and the thirds.
    val labelledTicks = stat.labelEvery?.takeIf { numOfTicks % it == 0 && numOfTicks / it <= 6 }
        ?.let { every -> (0..numOfTicks step every).toList() }
        ?: listOf(0, ((sliderTickRanges.size - 1) / 3f).roundToInt(), ((sliderTickRanges.size - 1) * 2 / 3f).roundToInt(), sliderTickRanges.size - 1)
    val tickLabels = mutableListOf<Pair<LabelAPI, Int>>()

    val sliderModifier = Modifier
        .tooltip(370f, TooltipMakerAPI.TooltipLocation.ABOVE) {
            addPara(stat.tooltip, 0f)
            addPara("Click anywhere within the slider to move the nearest point.", 0f)
            addPara("Setting the maximum to ${stat.format(maxRange)} makes the upper limit infinite.",
                0f, Misc.getTextColor(), Misc.getHighlightColor(), stat.format(maxRange))
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
            val lowerNodeXPos = mapRangeToXOffset(lowerState.value, offsets) + nodeStartXPos
            val upperNodeXPos = mapRangeToXOffset(upperState.value, offsets) + nodeStartXPos

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
            var newLower = lowerState.value
            var newUpper = upperState.value

            if (lowerIsNearer(event, offsets)) newLower = mapXPosToRange(event.x.toFloat(), nodeStartCenterXPos, offsets)
            else newUpper = mapXPosToRange(event.x.toFloat(), nodeStartCenterXPos, offsets)

            if (abs(upperState.value - newUpper) > rangeIncrement / 4 ||
                abs(lowerState.value - newLower) > rangeIncrement / 4) {
                if (rangeTickSnapping) playSound("ui_number_scrolling", 1f, 0.8f)
                lowerState.value = newLower
                upperState.value = newUpper
            }
        }

    slider = CustomPanel(Size.fillMaxSize(), sliderModifier) {
        tickLabels.clear()
        for (tick in labelledTicks) {
            val value = sliderTickRanges[tick]
            val text = stat.format(value)
            tickLabels += Text(text, font = Font.VICTOR_14, color = Misc.getTextColor()) to tick
        }
    }
}
