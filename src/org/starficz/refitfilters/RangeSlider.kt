package org.starficz.refitfilters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaUI.elements.LunaElement
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class RangeSlider(tooltip: TooltipMakerAPI, width: Float, height: Float, var valueA: Float, var valueB: Float,
                  var minValue: Float, var maxValue: Float, var increment: Float = 100f) : LunaElement(tooltip, width, height) {

    val sliderNodeWidth = 2f
    val tickWidth = 1f
    val tickHeight = 0.3f
    val tickIncrements = generateSequence(minValue) { it + increment }.takeWhile { it <= maxValue }.map { (it - minValue) / (maxValue - minValue) }.toList()

    var levelA = linMap(0f,1f, minValue, maxValue, valueA)
    var levelB = linMap(0f,1f, minValue, maxValue, valueB)
    var nodeAHovered = false
    var nodeBHovered = false
    var nodeColor = Misc.getHighlightColor()

    init {
        enableTransparency = true
        renderBackground = false
        renderBorder = false

        onClick {
            Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f)
        }
        onHover{event ->
            // get mouse level and snap it to the closest interval
            val minSliderX = position.centerX - width / 2
            val maxSliderX = position.centerX + width / 2
            var mouseLevel = linMap(0f,1f, minSliderX, maxSliderX, event.x.toFloat())

            if (abs(levelA - mouseLevel) <= abs(levelB - mouseLevel)) {
                if(nodeBHovered) Global.getSoundPlayer().playUISound("ui_number_scrolling", 1f, 0.8f)
                nodeAHovered = true
                nodeBHovered = false
            } else {
                if(nodeAHovered) Global.getSoundPlayer().playUISound("ui_number_scrolling", 1f, 0.8f)
                nodeAHovered = false
                nodeBHovered = true
            }

        }
        onHoverEnter {
            Global.getSoundPlayer().playUISound("ui_number_scrolling", 1f, 0.8f)
        }
        onHoverExit {
            nodeAHovered = false
            nodeBHovered = false
        }
        onHeld {event ->
            // get mouse level and snap it to the closest interval
            val minSliderX = position.centerX - width / 2
            val maxSliderX = position.centerX + width / 2
            var mouseLevel = linMap(0f,1f, minSliderX, maxSliderX, event.x.toFloat())


            if (abs(levelA - mouseLevel) <= abs(levelB - mouseLevel)) levelA = mouseLevel else levelB = mouseLevel

            levelA = ((minValue + levelA * (maxValue - minValue)) / increment).roundToInt() * increment.let { (it - minValue) / (maxValue - minValue) }
            levelB = ((minValue + levelB * (maxValue - minValue)) / increment).roundToInt() * increment.let { (it - minValue) / (maxValue - minValue) }

            valueA = linMap(minValue, maxValue, 0f, 1f, levelA)
            valueB = linMap(minValue, maxValue, 0f, 1f, levelB)
            event.consume()
        }

        // i got too lazy to dynamicaly get these values
        this.innerElement.setParaFontVictor14()
        this.innerElement.addPara("0", 0f).apply {
            position.inTL(-3f,0f)
        }
        this.innerElement.addPara("500", 0f).apply {
            position.inTL(width/3-13f,0f)
        }
        this.innerElement.addPara("1000", 0f).apply {
            position.inTL(width*2/3-17f,0f)
        }
        this.innerElement.addPara("1500", 0f).apply {
            position.inTL(width-19f,0f)
        }
    }

    override fun advance(amount: Float) {
        super.advance(amount)
    }

    override fun renderBelow(alphaMult: Float) {
        super.renderBelow(alphaMult)

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        // background line
        var color = backgroundColor
        GL11.glColor4f(color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f * (alphaMult * backgroundAlpha))
        GL11.glRectf(x + tickWidth, y + height * 0.05f , x + width - tickWidth, y + height * 0.10f)

        // tick marks
        for (tickPos in tickIncrements){
            GL11.glRectf(x + (width-tickWidth*4) * tickPos+tickWidth, y + height * 0.05f , x + (width-tickWidth*4) * tickPos+tickWidth*3, y + height * tickHeight)
        }

        var min = position.centerX - width / 2
        var max = position.centerX + width / 2
        var nodeAPos = Misc.interpolate(min, max, levelA)
        var nodeBPos = Misc.interpolate(min, max, levelB)

        color = nodeColor
        GL11.glColor4f(color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f * (alphaMult * backgroundAlpha))
        GL11.glRectf(min(nodeAPos, nodeBPos), y + height * 0.05f , max(nodeAPos, nodeBPos), y + height * 0.10f)

        GL11.glPopMatrix()
    }

    override fun render(alphaMult: Float) {
        super.render(alphaMult)

        var min = position.centerX - width / 2 + sliderNodeWidth
        var max = position.centerX + width / 2 - sliderNodeWidth

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        var nodeAColor = if(nodeAHovered) nodeColor.brighter() else nodeColor
        GL11.glColor4f(nodeAColor.red / 255f,
            nodeAColor.green / 255f,
            nodeAColor.blue / 255f,
            nodeAColor.alpha / 255f * (alphaMult * backgroundAlpha))

        var nodeAPos = Misc.interpolate(min, max, levelA)
        GL11.glRectf(nodeAPos - sliderNodeWidth, y , nodeAPos + sliderNodeWidth, y + sliderNodeWidth*2)

        var nodeBColor = if(nodeBHovered) nodeColor.brighter() else nodeColor
        GL11.glColor4f(nodeBColor.red / 255f,
            nodeBColor.green / 255f,
            nodeBColor.blue / 255f,
            nodeBColor.alpha / 255f * (alphaMult * backgroundAlpha))

        var nodeBPos = Misc.interpolate(min, max, levelB)
        GL11.glRectf(nodeBPos - sliderNodeWidth, y , nodeBPos + sliderNodeWidth, y + sliderNodeWidth*2)

        GL11.glPopMatrix()
    }

    fun linMap(minOut: Float, maxOut: Float, minIn: Float, maxIn: Float, input: Float): Float {
        if (input > maxIn) return maxOut
        if (input < minIn) return minOut
        return minOut + (input - minIn) * (maxOut - minOut) / (maxIn - minIn)
    }

    fun getCurrentMinValue(): Float{
        return min(valueA, valueB)
    }

    fun getCurrentMaxValue(): Float{
        return max(valueA, valueB)
    }
}