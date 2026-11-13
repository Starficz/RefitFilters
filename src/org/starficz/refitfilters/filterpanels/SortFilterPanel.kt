package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.SortKey
import org.starficz.refitfilters.WeaponFilterData
import org.starficz.starui.Alignment
import org.starficz.starui.Axis
import org.starficz.starui.components.*
import org.starficz.starui.modifier.*
import org.starficz.starui.Size
import org.starficz.starui.vanilla.*
import java.awt.Color

/** The strip's height. */
const val SORT_ROW_HEIGHT = 20f
/** Above the rule, from the filter row before it. */
const val SORT_ROW_GAP = 5f
/** The rule: vanilla's 1 px line under the mounted weapon. */
const val SORT_RULE_HEIGHT = 1f
/** Between the rule and the strip. */
const val SORT_RULE_GAP = 5f
/** The rule, its gap and the strip. */
const val SORT_PANEL_HEIGHT = SORT_RULE_HEIGHT + SORT_RULE_GAP + SORT_ROW_HEIGHT
/** From the strip to the list below it: the strip heads the list. */
const val SORT_LIST_GAP = 2f

/** The direction arrow: 7 px wide, 4 tall, with 3 px between it and its label. */
private const val ARROW_WIDTH = 7f
private const val ARROW_HEIGHT = 4f
private const val ARROW_GAP = 3f

/**
 * Labels sit 2 px higher than centred: a label's line box reserves room for descenders that these capitals do not
 * use, so centring the box leaves the letters low.
 */
private const val LABEL_LIFT = 4f

/**
 * The sort row: vanilla's 1 px rule, then "SORT BY:" and the sort keys as a header strip (vanilla's table-head look,
 * not buttons). The active key is bright with its direction arrow; click it again to reverse. The keys tile the
 * strip edge to edge: each gets its label (the active one also its arrow) plus an equal share of the space left, so
 * the arrow never squeezes a label.
 */
fun UIPanelAPI.createSortPanel(targetWidth: Float, pickerPanel: UIPanelAPI, filterData: WeaponFilterData): CustomPanelAPI = Column(
    Size.exact(targetWidth, SORT_PANEL_HEIGHT), Modifier.refreshPickerOn(pickerPanel, filterData.sortKey, filterData.sortDescending),
    spacing = 0f
) {
    // splits the filters above from the strip and the list below
    Spacer(Size.exact(targetWidth, SORT_RULE_HEIGHT), Modifier.onRender { alphaMult -> drawRule(left, top, width, alphaMult) })
    Spacer(Size.exact(targetWidth, SORT_RULE_GAP))
    Row(
        Size.exact(targetWidth, SORT_ROW_HEIGHT), Modifier.background(Misc.getDarkPlayerColor().darker().darker()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(SORT_BY, font = Font.VICTOR_14, color = Misc.getGrayColor(),
            modifier = Modifier.margin(left = SORT_BY_LEFT, right = SORT_BY_RIGHT, bottom = LABEL_LIFT))
        // read here, so the strip is laid out again when the order changes
        val activeKey = filterData.sortKey.value
        val descending = filterData.sortDescending.value

        // The keys tile the rest of the strip edge to edge. Each one's fill weight is its content width plus an
        // equal share of the space left, so the fill layout hands every key exactly its label (and the active one
        // its arrow) plus the same extra room.
        val keys = SortKey.values()
        val contents = keys.map { key ->
            measureTextSize(key.label, Font.VICTOR_14).first + if (key == activeKey) ARROW_GAP + ARROW_WIDTH else 0f
        }
        val room = targetWidth - (SORT_BY_LEFT + measureTextSize(SORT_BY, Font.VICTOR_14).first + SORT_BY_RIGHT)
        val share = ((room - contents.sum()) / keys.size).coerceAtLeast(0f)
        for ((key, content) in keys.zip(contents)) {
            SortItem(key, key == activeKey, descending, filterData, weight = content + share)
        }
    }
}

private const val SORT_BY = "SORT BY:"
private const val SORT_BY_LEFT = 8f
private const val SORT_BY_RIGHT = 4f

private fun UIPanelAPI.SortItem(key: SortKey, active: Boolean, descending: Boolean, filterData: WeaponFilterData, weight: Float) {
    val look = Modifier
        .tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) {
            addPara(key.tooltip, 0f)
            addPara("Click again to reverse the order.", 0f)
        }
        .onRenderBelow { alphaMult ->
            if (!active && !isHovering) return@onRenderBelow
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            glColor(if (active) Misc.getDarkPlayerColor() else Misc.getDarkPlayerColor().darker(), alphaMult, false)
            GL11.glRectf(left, bottom, right, top)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
        }
        .onClick {
            playSound("ui_button_pressed", 1f, 1f)
            if (filterData.sortKey.value == key) filterData.sortDescending.value = !filterData.sortDescending.value
            else {
                filterData.sortKey.value = key
                filterData.sortDescending.value = key.descendingFirst
            }
        }
    // the label (and arrow) centred in the key's share of the strip
    Row(Size.mixed(Axis.FillMax(weight), Axis.FillMax()), look, verticalAlignment = Alignment.CenterVertically) {
        FlexSpacer()
        Text(key.label, font = Font.VICTOR_14,
            color = if (active) Misc.getBrightPlayerColor() else Misc.getBasePlayerColor(),
            modifier = Modifier.margin(bottom = LABEL_LIFT))
        if (active) {
            Spacer(Size.exact(ARROW_WIDTH, ARROW_HEIGHT), Modifier.margin(left = ARROW_GAP, bottom = LABEL_LIFT).onRender { alphaMult ->
                drawArrow(left, top, descending, alphaMult)
            })
        }
        FlexSpacer()
    }
}

/** A 7 x 4 px arrowhead from whole-pixel rows (7, 5, 3, 1 wide), pointing down for descending. */
private fun drawArrow(left: Float, top: Float, down: Boolean, alphaMult: Float) {
    val x0 = left.toInt().toFloat()
    val y0 = top.toInt().toFloat()
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    glColor(Misc.getBrightPlayerColor(), alphaMult, false)
    for (row in 0 until 4) {
        val inset = if (down) row else 3 - row   // the widest row at the top when pointing down
        GL11.glRectf(x0 + inset, y0 - row - 1f, x0 + ARROW_WIDTH - inset, y0 - row)
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D)
}

/** Vanilla's line under the mounted weapon: one colour, the last 7 px fading to nothing, as measured in game. */
private val RULE_COLOR = Color(75, 165, 191)
private val RULE_FADE = floatArrayOf(0.93f, 0.79f, 0.64f, 0.49f, 0.36f, 0.22f, 0.07f)

/** The rule starts 2 px left of the filter rows, at the picker's frame, as vanilla's does. */
private fun drawRule(left: Float, top: Float, width: Float, alphaMult: Float) {
    val start = left - 2f
    val solid = width + 2f - RULE_FADE.size
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    glColor(RULE_COLOR, alphaMult, false)
    GL11.glRectf(start, top - 1f, start + solid, top)
    for ((i, fade) in RULE_FADE.withIndex()) {
        glColor(RULE_COLOR, alphaMult * fade, false)
        GL11.glRectf(start + solid + i, top - 1f, start + solid + i + 1f, top)
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D)
}
