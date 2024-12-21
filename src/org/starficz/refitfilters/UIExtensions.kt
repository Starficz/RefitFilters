package org.starficz.refitfilters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.ReflectionUtils

fun UIPanelAPI.getChildrenCopy() : List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
}

fun UIPanelAPI.getChildrenNonCopy() : List<UIComponentAPI>  {
    return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
}

fun UIPanelAPI.getWidth() : Float  {
    return ReflectionUtils.invoke("getWidth", this) as Float
}

fun UIPanelAPI.getHeight() : Float  {
    return ReflectionUtils.invoke("getHeight", this) as Float
}

fun UIPanelAPI.clearChildren() {
    ReflectionUtils.invoke("clearChildren", this)
}

fun UIComponentAPI.getParent() : UIPanelAPI?  {
    return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
}

fun TooltipMakerAPI.getParentWidget() : UIComponentAPI? {
    return ReflectionUtils.invoke("getParentWidget", this) as UIPanelAPI
}

fun UIComponentAPI.setOpacity(alpha: Float)
{
    ReflectionUtils.invoke("setOpacity", this, alpha)
}

fun TooltipMakerAPI.addTooltip(to: UIComponentAPI, location: TooltipMakerAPI.TooltipLocation, width: Float, lambda: (TooltipMakerAPI) -> Unit) {
    this.addTooltipTo(object: TooltipMakerAPI.TooltipCreator {
        override fun isTooltipExpandable(tooltipParam: Any?): Boolean {
            return false
        }

        override fun getTooltipWidth(tooltipParam: Any?): Float {
            return width
        }

        override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
            lambda(tooltip!!)
        }

    }, to, location)
}
