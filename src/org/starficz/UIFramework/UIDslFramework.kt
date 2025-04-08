package org.starficz.UIFramework

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.*
import org.lwjgl.input.Keyboard
import java.awt.Color


internal fun CustomPanelAPI.TooltipMakerPanel(
    width: Float,
    height: Float,
    withScroller: Boolean = false,
    builder: TooltipMakerAPI.() -> Unit = {}
): TooltipMakerAPI {
    val tooltipMakerPanel = createUIElement(width, height, withScroller)
    addUIElement(tooltipMakerPanel)
    return tooltipMakerPanel.apply(builder)
}

internal fun UIPanelAPI.Text(
    text: String,
    font: Font? = null,
    color: Color? = null,
    highlightedText: Collection<Pair<String, Color>>? = null,
    builder: BoxedUILabel.() -> Unit = {}
): BoxedUILabel {
    return this.addPara(text, font, color, highlightedText).apply(builder)
}

internal fun UIPanelAPI.LabelledValue(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    width: Float,
    builder: UIComponentAPI.() -> Unit = {}
): UIComponentAPI {
    return this.addLabelledValue(label, value, labelColor, valueColor, width).apply(builder)
}

internal fun UIPanelAPI.TextField(
    width: Float,
    height: Float,
    font: Font,
    builder: TextFieldAPI.() -> Unit = {}
): TextFieldAPI {
    return this.addTextField(width, height, font).apply(builder)
}

internal fun UIPanelAPI.Image(
    imageSpritePath: String,
    width: Float,
    height: Float,
    builder: BoxedUIImage.() -> Unit = {} // Configures BoxedUiImage
): BoxedUIImage {
    return addImage(imageSpritePath, width, height).apply(builder)
}

internal fun UIComponentAPI.Tooltip(
    location: TooltipMakerAPI.TooltipLocation,
    width: Float,
    padding: Float? = null,
    builder: TooltipMakerAPI.() -> Unit = {} // Configuration lambda for tooltip content
) {
    this.addTooltip(location, width, padding, builder)
}

internal fun UIPanelAPI.Button(
    text: String,
    baseColor: Color,
    bgColor: Color,
    align: Alignment = Alignment.MID,
    style: CutStyle = CutStyle.TL_BR,
    width: Float,
    height: Float,
    font: Font? = null,
    builder: ButtonAPI.() -> Unit = {}
): ButtonAPI {
    return this.addButton(text, null, baseColor, bgColor, align, style, width, height, font).apply(builder)
}

internal fun UIPanelAPI.AreaCheckbox(
    text: String,
    baseColor: Color,
    bgColor: Color,
    brightColor: Color,
    width: Float,
    height: Float,
    font: Font? = null,
    leftAlign: Boolean = false,
    flag: Flag? = null,
    buttonGroup: ButtonGroup? = null,
    builder: ButtonAPI.() -> Unit = {} // Configures the BoxedButton wrapper
): ButtonAPI {
    val validGroup = (buttonGroup != null && flag != null)

    val button = this.addAreaCheckbox(text, null, baseColor, bgColor, brightColor, width, height, font,
        leftAlign, if (!validGroup) flag else null).apply(builder)
    if (validGroup) buttonGroup!!.addButtonToGroup(button, flag!!)

    return button
}

internal class ButtonGroup {
    val allFlags: MutableCollection<Flag> = mutableListOf()

    fun addButtonToGroup(button: ButtonAPI, flag: Flag){
        allFlags.add(flag)
        button.isChecked = flag.isEnabled
        button.onClick {
            if (allFlags.count { it.isEnabled } == 1 && flag.isEnabled) {
                // If the only active item is clicked, re-enable all items in the group.
                allFlags.forEach { it.isEnabled = true }
            } else {
                // if multiselect key (Shift or Ctrl) is held, toggle the clicked filter
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
                    Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ||
                    Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                    Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    flag.isEnabled = !flag.isEnabled
                } else { // if no modifier key is held, only exclusively enable the clicked filter
                    allFlags.forEach { it.isEnabled = (it === flag) }
                }
            }
            // sync the button to the flag
            button.isChecked = flag.isEnabled
        }
    }
}

fun UIPanelAPI.CustomPanel(
    width: Float,
    height: Float,
    builder: CustomPanelAPI.(plugin: ExtendableCustomUIPanelPlugin) -> Unit = {}
): CustomPanelAPI {
    val panel = Global.getSettings().createCustom(width, height, null)
    val plugin = ExtendableCustomUIPanelPlugin(panel)
    panel.setPlugin(plugin)
    this.addComponent(panel)
    panel.builder(plugin)
    return panel
}

