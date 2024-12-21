package org.starficz.refitfilters

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Fonts
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import lunalib.lunaUI.elements.LunaElement
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class SearchField(tooltip: TooltipMakerAPI, width: Float, height: Float, private var text: String, var textColor: Color = Misc.getBasePlayerColor()) : LunaElement(tooltip, width, height) {

    private var textElement: TooltipMakerAPI? = null
    private var para: LabelAPI? = null

    private var blinkInterval = IntervalUtil(0.5f, 0.5f)
    private var blink = false

    private var font = Fonts.DEFAULT_SMALL

    init {
        innerElement.setParaFont(font)
        setText(text)
    }


    override fun advance(amount: Float) {
        super.advance(amount)

        blinkInterval.advance(amount)

        if (blinkInterval.intervalElapsed()) {
            blink = !blink
            if (para != null) {
                if (blink) {
                    para!!.text = "$text "
                }
                else {
                    para!!.text = "$text|"
                }
            }
        }
    }

    fun getText(): String {
        return text
    }

    fun setText(text: String) {
        if (textElement != null) {
            elementPanel.removeComponent(textElement)
            para = null
            textElement = null
        }

        this.text = text

        textElement = elementPanel.createUIElement(width, height, false)
        elementPanel.addUIElement(textElement)

        textElement!!.setParaFont(font)

        var add = " "
        if (blink) add = "|"
        para = textElement!!.addPara(text + add, 0f, textColor, Misc.getHighlightColor())
        para!!.position.inTL(5f, height / 2 - para!!.computeTextHeight(para!!.text) / 2)
    }


    override fun processInput(events: MutableList<InputEventAPI>?) {
        super.processInput(events)

        if (para == null) return

        for (event in events!!){
            if (event.isConsumed) continue
            if (event.isKeyboardEvent && (event.isKeyDownEvent || event.isRepeat)){

                if (event.eventValue == Keyboard.KEY_V && event.isCtrlDown){
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
                    for (char in clipboard)
                    {
                        appendCharIfPossible(char)
                    }
                    event.consume()
                    break
                }
                if (event.eventValue == Keyboard.KEY_RETURN || event.eventValue == Keyboard.KEY_NUMPADENTER) {
                    break
                }

                if (event.eventValue == Keyboard.KEY_ESCAPE) {
                    if (text.isNotEmpty()){
                        playSound("ui_typer_buzz")
                        deleteAll()
                        event.consume()
                        break
                    } else{
                        return
                    }
                }

                if (event.eventValue == Keyboard.KEY_BACK){
                    if(text.isEmpty()){
                        playSound("ui_typer_buzz")
                        event.consume()
                    }
                    else if (event.isShiftDown){
                        deleteAll()
                    }
                    else if (event.isCtrlDown){
                        deleteLastWord()
                    }
                    else{
                        playSound("ui_typer_type")
                        setText(text.substring(0, text.length - 1))
                    }
                    event.consume()
                    break
                }

                if (event.isCtrlDown || event.isAltDown || event.eventValue in (2..11)){
                    break
                }

                appendCharIfPossible(event.eventChar)
                event.consume()
            }
        }
    }


    private fun appendCharIfPossible(char: Char){
        val appended = text + char

        val valid = para!!.computeTextWidth(para!!.text) < (width - 20)

        if (isValidChar(char) && valid){
            playSound("ui_typer_type")
            setText(appended)
        }
    }

    private fun isValidChar(char: Char?) : Boolean {
        return when(char){
            '\u0000' -> false
            '%' -> false
            '$' -> false
            else -> true
        }
    }


    private fun deleteAll() {
        playSound("ui_typer_type")
        setText("")
    }

    private fun deleteLastWord() {
        var last = text.lastIndexOf(" ")
        if (last == text.length - 1 && last > 0) last = text.substring(0, last).lastIndexOf(" ")

        if (last == -1) deleteAll()

        else {
            playSound("ui_typer_type")
            setText(text.substring(0, last + 1))
        }
    }
}