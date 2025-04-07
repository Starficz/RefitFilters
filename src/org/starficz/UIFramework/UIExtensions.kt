package org.starficz.UIFramework

import com.fs.graphics.Sprite
import com.fs.graphics.util.Fader
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
import org.starficz.UIFramework.ReflectionUtils.getMethodsMatching
import org.starficz.UIFramework.ReflectionUtils.hasMethodOfName
import org.starficz.UIFramework.ReflectionUtils.invoke
import org.starficz.UIFramework.ReflectionUtils.set
import java.awt.Color

// UIComponentAPI extensions that expose UIComponent fields/methods
internal var UIComponentAPI.fader: Fader?
    get() = invoke("getFader") as Fader?
    set(fader) { invoke("setFader", fader) }

internal var UIComponentAPI.opacity: Float
    get() = invoke("getOpacity") as Float
    set(alpha) { invoke("setOpacity", alpha) }

internal var UIComponentAPI.parent: UIPanelAPI?
    get() = invoke("getParent") as UIPanelAPI?
    set(parent) { invoke("setParent", parent) }

internal fun UIComponentAPI.setMouseOverPad(pad1: Float, pad2: Float, pad3: Float, pad4: Float) {
    invoke("setMouseOverPad", pad1, pad2, pad3, pad4)
}

internal val UIComponentAPI.mouseoverHighlightFader: Fader?
    get() = invoke("getMouseoverHighlightFader") as Fader?

internal val UIComponentAPI.topAncestor: UIPanelAPI?
    get() = invoke("findTopAncestor") as UIPanelAPI?

internal fun UIComponentAPI.setTooltipOffsetFromCenter(xPad: Float, yPad: Float){
    invoke("setTooltipOffsetFromCenter", xPad, yPad)
}

internal fun UIComponentAPI.setTooltipPositionRelativeToAnchor(xPad: Float, yPad: Float, anchor: UIComponentAPI){
    invoke("setTooltipPositionRelativeToAnchor", xPad, yPad, anchor)
}

internal fun UIComponentAPI.setSlideData(xOffset: Float, yOffset: Float, durationIn: Float, durationOut: Float){
    invoke("setSlideData", xOffset, yOffset, durationIn, durationOut)
}

internal fun UIComponentAPI.slideIn(){
    invoke("slideIn")
}

internal fun UIComponentAPI.slideOut(){
    invoke("slideOut")
}

internal fun UIComponentAPI.forceSlideIn(){
    invoke("forceSlideIn")
}

internal fun UIComponentAPI.forceSlideOut(){
    invoke("forceSlideOut")
}

internal val UIComponentAPI.sliding: Boolean
    get() = invoke("isSliding") as Boolean

internal val UIComponentAPI.slidIn: Boolean
    get() = invoke("isSlidIn") as Boolean

internal val UIComponentAPI.slidOut: Boolean
    get() = invoke("isSlidOut") as Boolean

internal val UIComponentAPI.slidingIn: Boolean
    get() = invoke("isSlidingIn") as Boolean

internal var UIComponentAPI.enabled: Boolean
    get() = invoke("isEnabled") as Boolean
    set(enabled) {
        invoke("setEnabled", enabled)
    }

internal var UIComponentAPI.width
    get() = position.width
    set(width) { position.setSize(width, position.height) }

internal var UIComponentAPI.height
    get() = position.height
    set(height) { position.setSize(position.width, height) }

internal fun UIComponentAPI.setSize(width: Float, height: Float){
    position.setSize(width, height)
}

internal val UIComponentAPI.x
    get() = position.x

internal val UIComponentAPI.y
    get() = position.y

internal val UIComponentAPI.left
    get() = x

internal val UIComponentAPI.bottom
    get() = y

internal val UIComponentAPI.top
    get() = y + height

internal val UIComponentAPI.right
    get() = x + width

internal fun UIComponentAPI.setLocation(x: Float, y: Float){
    position.setLocation(x, y)
}

internal val UIComponentAPI.centerX
    get() = position.centerX

internal val UIComponentAPI.centerY
    get() = position.centerY

internal var UIComponentAPI.xAlignOffset: Float
    get() = position.invoke("getXAlignOffset") as Float
    set(xOffset) { position.setXAlignOffset(xOffset) }

internal var UIComponentAPI.yAlignOffset: Float
    get() = position.invoke("getYAlignOffset") as Float
    set(yOffset) { position.setYAlignOffset(yOffset) }



internal fun UIComponentAPI.anchorInTopLeftOfParent(xPad: Float = 0f, yPad: Float = 0f) {
    this.position.inTL(xPad, yPad)
}
internal fun UIComponentAPI.anchorInTopRightOfParent(xPad: Float = 0f, yPad: Float = 0f) {
    this.position.inTR(xPad, yPad)
}
internal fun UIComponentAPI.anchorInTopMiddleOfParent(yPad: Float = 0f) {
    this.position.inTMid(yPad)
}
internal fun UIComponentAPI.anchorInBottomLeftOfParent(xPad: Float = 0f, yPad: Float = 0f) {
    this.position.inBL(xPad, yPad)
}
internal fun UIComponentAPI.anchorInBottomMiddleOfParent(yPad: Float = 0f) {
    this.position.inBMid(yPad)
}
internal fun UIComponentAPI.anchorInBottomRightOfParent(xPad: Float = 0f, yPad: Float = 0f) {
    this.position.inBR(xPad, yPad)
}
internal fun UIComponentAPI.anchorInLeftMiddleOfParent(xPad: Float = 0f) {
    this.position.inLMid(xPad)
}
internal fun UIComponentAPI.anchorInRightMiddleOfParent(xPad: Float = 0f) {
    this.position.inRMid(xPad)
}
internal fun UIComponentAPI.anchorInCenterOfParent() {
    val floatType = Float::class.javaPrimitiveType!!
    val paramTypes = listOf(this.position::class.java,
        floatType, floatType, floatType, floatType, floatType, floatType).toTypedArray()

    this.position.getMethodsMatching("relativeTo", parameterTypes = paramTypes)[0]
        .invoke(this.position, null, 0.5f, 0.5f, -0.5f, -0.5f, 0f, 0f)
}

internal val UIPanelAPI.previousComponent
    get() = getChildrenCopy().lastOrNull()

internal fun UIComponentAPI.anchorRightOfPreviousMatchingTop(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.rightOfTop(it, padding) }
}
internal fun UIComponentAPI.anchorLeftOfPreviousMatchingTop(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.leftOfTop(it, padding) }
}
internal fun UIComponentAPI.anchorLeftOfPreviousMatchingMid(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.leftOfMid(it, padding) }
}
internal fun UIComponentAPI.anchorLeftOfPreviousMatchingBottom(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.leftOfBottom(it, padding) }
}
internal fun UIComponentAPI.anchorRightOfPreviousMatchingMid(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.rightOfMid(it, padding) }
}
internal fun UIComponentAPI.anchorRightOfPreviousMatchingBottom(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.rightOfBottom(it, padding) }
}
internal fun UIComponentAPI.anchorAbovePreviousMatchingLeft(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.aboveLeft(it, padding) }
}
internal fun UIComponentAPI.anchorAbovePreviousMatchingMid(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.aboveMid(it, padding) }
}
internal fun UIComponentAPI.anchorAbovePreviousMatchingRight(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.aboveRight(it, padding) }
}
internal fun UIComponentAPI.anchorBelowPreviousMatchingLeft(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.belowLeft(it, padding) }
}
internal fun UIComponentAPI.anchorBelowPreviousMatchingMid(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.belowMid(it, padding) }
}
internal fun UIComponentAPI.anchorBelowPreviousMatchingRight(padding: Float = 0f) {
    parent?.getChildrenCopy()?.dropLast(1)?.lastOrNull()?.let { this.position.belowRight(it, padding) }
}
internal fun UIComponentAPI.anchorToPreviousMatchingCenter(xPad: Float = 0f, yPad: Float = 0f) {
    val parent = this.parent ?: return; val children = parent.getChildrenCopy(); if (children.size <= 1) return
    val anchor = children.dropLast(1).lastOrNull()
    anchor?.let { nonNullAnchor ->
        this.position.invoke("relativeTo", nonNullAnchor, 0.5f, 0.5f, -0.5f, -0.5f, xPad, yPad)
    }
}

internal fun UIComponentAPI.addTooltip(
    location: TooltipLocation,
    width: Float,
    padding: Float? = null,
    lambda: (TooltipMakerAPI) -> Unit) {
    val tooltip = object: StandardTooltipV2Expandable(width, false, true) {
        override fun createImpl(p0: Boolean) {
            lambda(this)
        }
    }

    val tooltipClass = StandardTooltipV2Expandable::class.java
    if(padding == null){
        when(location){
            TooltipLocation.LEFT -> tooltipClass.invoke("addTooltipLeft", this, tooltip)
            TooltipLocation.RIGHT -> tooltipClass.invoke("addTooltipRight", this, tooltip)
            TooltipLocation.ABOVE -> tooltipClass.invoke("addTooltipAbove", this, tooltip)
            TooltipLocation.BELOW -> tooltipClass.invoke("addTooltipBelow", this, tooltip)
        }
    }
    else{
        when(location){
            TooltipLocation.LEFT -> tooltipClass.invoke("addTooltipLeft", this, tooltip, padding)
            TooltipLocation.RIGHT -> tooltipClass.invoke("addTooltipRight", this, tooltip, padding)
            TooltipLocation.ABOVE -> tooltipClass.invoke("addTooltipAbove", this, tooltip, padding)
            TooltipLocation.BELOW -> tooltipClass.invoke("addTooltipBelow", this, tooltip, padding)
        }
    }
}

// UIPanelAPI extensions that expose UIPanel methods
internal fun UIPanelAPI.getChildrenCopy(): List<UIComponentAPI> {
    return invoke("getChildrenCopy") as List<UIComponentAPI>
}

internal fun UIPanelAPI.getChildrenNonCopy(): List<UIComponentAPI> {
    return invoke("getChildrenNonCopy") as List<UIComponentAPI>
}

internal fun UIPanelAPI.findChildWithMethod(methodName: String): UIComponentAPI? {
    return getChildrenCopy().find { it.hasMethodOfName(methodName) }
}

internal fun UIPanelAPI.allChildsWithMethod(methodName: String): List<UIComponentAPI> {
    return getChildrenCopy().filter { it.hasMethodOfName(methodName) }
}

internal fun UIPanelAPI.clearChildren() {
    invoke("clearChildren")
}

// Abstract base class for Boxed vanilla elements to fix vanilla jank / things with no API's (like images)
abstract class BoxedUIElement(val boxedElement: UIComponentAPI)

class BoxedUILabel(val uiLabel: LabelAPI): BoxedUIElement(uiLabel as UIComponentAPI),
    UIComponentAPI by (uiLabel as UIComponentAPI), LabelAPI by uiLabel {
    override fun advance(amount: Float) { uiLabel.advance(amount) }
    override fun getOpacity() = uiLabel.opacity
    override fun setOpacity(opacity: Float) { uiLabel.opacity = opacity }
    override fun render(alphaMult: Float) { uiLabel.render(alphaMult) }
    override fun getPosition() = uiLabel.position
}

class BoxedUIImage(val uiImage: UIComponentAPI): BoxedUIElement(uiImage), UIComponentAPI by uiImage {
    var spriteName = uiImage.invoke("getSpriteName") as String
        set(newSpriteName) { uiImage.invoke("setSprite", newSpriteName, true) }
    var sprite = uiImage.invoke("getSprite") as Sprite
        set(newSprite) { uiImage.invoke("setSprite", newSprite, true) }

    var borderColor = uiImage.invoke("getBorderColor") as Color
        set(newColor) { uiImage.invoke("setBorderColor", newColor, true) }

    var outline = uiImage.invoke("isWithOutline") as Boolean
        set(withOutline) { uiImage.invoke("setWithOutline", withOutline) }

    var textureClamp = uiImage.invoke("isTexClamp") as Boolean
        set(texClamp) { uiImage.invoke("setTexClamp", texClamp) }

    var forceNoRounding = uiImage.invoke("isForceNoRounding") as Boolean
        set(noRounding) { uiImage.invoke("setForceNoRounding", noRounding) }

    val originalAspectRatio = uiImage.invoke("getOriginalAR") as Float

    fun setStretch(stretch: Boolean) { uiImage.invoke("setStretch", stretch) }
    fun setRenderSchematic(renderSchematic: Boolean) { uiImage.invoke("setRenderSchematic", renderSchematic) }
    fun sizeToOriginalSpriteSize() { uiImage.invoke("autoSize") }
    fun sizeToOriginalAspectRatioWithWidth(width: Float) { uiImage.invoke("autoSizeToWidth", width) }
    fun sizeToOriginalAspectRatioWithHeight(height: Float) { uiImage.invoke("autoSizeToHeight", height) }
}

internal fun UIPanelAPI.addPara(text: String, font: Font? = null, color: Color? = null,
                                highlightedText: Collection<Pair<String, Color>>? = null): BoxedUILabel {
    val tempPanel = Global.getSettings().createCustom(width, height, null)
    val tempTMAPI = tempPanel.createUIElement(width, height, false)
    color?.let { tempTMAPI.setParaFontColor(it) }
    font?.let { tempTMAPI.setParaFont(getFontPath(font)) }

    val para = if(highlightedText != null){
        val (highlights, highlightColors) = highlightedText.unzip()
        tempTMAPI.addPara(text, 0f, highlightColors.toTypedArray(), *highlights.toTypedArray())
    } else {
        tempTMAPI.addPara(text, 0f)
    }

    this.addComponent(para as UIComponentAPI)
    para.invoke("autoSize")
    return BoxedUILabel(para)
}

internal fun UIPanelAPI.addImage(imageSpritePath: String, width: Float, height: Float): BoxedUIImage {
    val tempPanel = Global.getSettings().createCustom(width, height, null)
    val tempTMAPI = tempPanel.createUIElement(width, height, false)
    tempTMAPI.addImage(imageSpritePath, width, height, 0f)
    val tempTMAPIsUIPanel = tempTMAPI.getChildrenCopy()[0] as UIPanelAPI
    val image = tempTMAPIsUIPanel.getChildrenCopy()[0]

    this.addComponent(image)
    return BoxedUIImage(image)
}

internal fun UIPanelAPI.addButton(
    text: String, data: Any?, baseColor: Color, bgColor: Color, align: Alignment, style: CutStyle,
    width: Float, height: Float, font: Font? = null): ButtonAPI {
    // make a button in a temp panel/element
    val tempPanel = Global.getSettings().createCustom(width, height, null)
    val tempTMAPI = tempPanel.createUIElement(width, height, false)
    when(font){
        Font.VICTOR_10 -> tempTMAPI.setButtonFontVictor10()
        Font.VICTOR_14 -> tempTMAPI.setButtonFontVictor14()
        Font.ORBITRON_20 -> tempTMAPI.setButtonFontOrbitron20()
        Font.ORBITRON_20_BOLD -> tempTMAPI.setButtonFontOrbitron20Bold()
        Font.ORBITRON_24 -> tempTMAPI.setButtonFontOrbitron24()
        Font.ORBITRON_24_BOLD -> tempTMAPI.setButtonFontOrbitron24Bold()
        null -> tempTMAPI.setButtonFontDefault()
    }
    val button = tempTMAPI.addButton(text, data, baseColor, bgColor, align, style, width, height, 0f)

    // hijack button and move it to UIPanel
    this.addComponent(button)
    button.xAlignOffset = 0f
    button.yAlignOffset = 0f
    return button
}

internal fun UIPanelAPI.addAreaCheckbox(
    text: String, data: Any?, baseColor: Color, bgColor: Color, brightColor: Color,
    width: Float, height: Float, font: Font? = null, leftAlign: Boolean = false, flag: Flag? = null): ButtonAPI {
    // make a button in a temp panel/element
    val tempPanel = Global.getSettings().createCustom(width, height, null)
    val tempTMAPI = tempPanel.createUIElement(width, height, false)
    font?.let { tempTMAPI.setAreaCheckboxFont(getFontPath(font)) }

    val button = tempTMAPI.addAreaCheckbox(
        text, data, baseColor, bgColor, brightColor, width, height, 0f, leftAlign)

    this.addComponent(button)
    if (flag != null) {
        button.isChecked = flag.isEnabled
        button.onClick { flag.isEnabled = button.isChecked  }
    }
    button.xAlignOffset = 0f
    button.yAlignOffset = 0f
    return button
}


// CustomPanelAPI implements the same Listener that a ButtonAPI requires,
// A CustomPanel then happens to trigger its CustomUIPanelPlugin buttonPressed() method
// thus we can map our functions into a CustomUIPanelPlugin, and have them be triggered
internal class ButtonListener(button: ButtonAPI) : BaseCustomUIPanelPlugin() {
    private val onClickFunctions = mutableListOf<() -> Unit>()

    init {
        val buttonListener = Global.getSettings().createCustom(0f, 0f, this)
        button.invoke("setListener", buttonListener)
    }
    override fun buttonPressed(buttonId: Any?) { onClickFunctions.forEach { it() } }
    fun addOnClick(function: () -> Unit) { onClickFunctions.add(function) }
    fun clearOnClickFunctions() { onClickFunctions.clear() }
}

// Extension function for ButtonAPI
internal fun ButtonAPI.onClick(function: () -> Unit) {
    // Use reflection to check if this button already has a listener
    val existingListener = invoke("getListener")
    if (existingListener is CustomPanelAPI && existingListener.plugin is ButtonListener) {
        (existingListener.plugin as ButtonListener).addOnClick(function)
    } else {
        // if not, make one
        val listener = ButtonListener(this)
        listener.addOnClick(function)
    }
}

// Custom CustomUIPanelPlugin extensions that map the plugin to the panel
internal val ExtendableCustomUIPanelPlugin.width
    get() = customPanel.width

internal val ExtendableCustomUIPanelPlugin.height
    get() = customPanel.height

internal val ExtendableCustomUIPanelPlugin.x
    get() = customPanel.x

internal val ExtendableCustomUIPanelPlugin.y
    get() = customPanel.y

internal val ExtendableCustomUIPanelPlugin.left
    get() = x

internal val ExtendableCustomUIPanelPlugin.bottom
    get() = y

internal val ExtendableCustomUIPanelPlugin.top
    get() = y + height

internal val ExtendableCustomUIPanelPlugin.right
    get() = x + width

internal val ExtendableCustomUIPanelPlugin.centerX
    get() = customPanel.centerX

internal val ExtendableCustomUIPanelPlugin.centerY
    get() = customPanel.centerY

internal val ExtendableCustomUIPanelPlugin.xAlignOffset
    get() = customPanel.xAlignOffset

internal val ExtendableCustomUIPanelPlugin.yAlignOffset
    get() = customPanel.yAlignOffset

internal fun CustomPanelAPI.setPlugin(plugin: CustomUIPanelPlugin) {
    set(value=plugin)
}
