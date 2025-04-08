package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.UIFramework.*
import org.starficz.UIFramework.ButtonGroup
import org.starficz.UIFramework.Font
import org.starficz.UIFramework.anchorInTopLeftOfParent
import org.starficz.UIFramework.anchorRightOfPreviousMatchingMid
import org.starficz.UIFramework.onClick
import org.starficz.refitfilters.FilterData
import org.starficz.refitfilters.FilterPanelCreator


fun UIPanelAPI.createWeaponTypesFilterPanel(width: Float, height: Float): CustomPanelAPI {

    val brightColor = Global.getSettings().basePlayerColor
    val baseColor = brightColor.darker()
    val bgColor = baseColor.darker().darker()

    return CustomPanel(width, height) { plugin ->
        val beamGroup = ButtonGroup()
        val pdGroup = ButtonGroup()
        val ammoGroup = ButtonGroup()
        val buttonWidth = 62f

        AreaCheckbox("PROJ", baseColor, bgColor, brightColor, buttonWidth, height,
            font = Font.VICTOR_14, flag = FilterData.projectileWeapons, buttonGroup = beamGroup) {

            anchorInTopLeftOfParent()

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show projectile weapons.", 0f)
            }
            onClick { FilterPanelCreator.filtersChanged() }
        }

        AreaCheckbox("BEAM", baseColor, bgColor, brightColor, buttonWidth, height,
            font = Font.VICTOR_14, flag = FilterData.beamWeapons, buttonGroup = beamGroup) {

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show beam weapons.", 0f)
            }
            onClick { FilterPanelCreator.filtersChanged() }
        }

        AreaCheckbox("PD", baseColor, bgColor, brightColor, buttonWidth-10f, height,
            font = Font.VICTOR_14, flag = FilterData.pdWeapons, buttonGroup = pdGroup) {

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show point defense weapons.", 0f)
            }
            onClick { FilterPanelCreator.filtersChanged() }
        }

        AreaCheckbox("NON-PD", baseColor, bgColor, brightColor, buttonWidth+10f, height,
            font = Font.VICTOR_14, flag = FilterData.nonpdWeapons, buttonGroup = pdGroup) {

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show weapons that arnt point defense.", 0f)
            }
            onClick { FilterPanelCreator.filtersChanged() }
        }

        AreaCheckbox("AMMO", baseColor, bgColor, brightColor, buttonWidth-10f, height,
            font = Font.VICTOR_14, flag = FilterData.ammoWeapons, buttonGroup = ammoGroup) {

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show weapons that use ammo.", 0f)
            }
            onClick { FilterPanelCreator.filtersChanged() }
        }

        AreaCheckbox("NON-AMMO", baseColor, bgColor, brightColor, buttonWidth+10f, height,
            font = Font.VICTOR_14, flag = FilterData.nonAmmoWeapons, buttonGroup = ammoGroup) {

            anchorRightOfPreviousMatchingMid(1f)

            Tooltip(TooltipMakerAPI.TooltipLocation.ABOVE,300f) {
                addPara("Show weapons that don't use ammo.", 0f)
            }
            onClick { FilterPanelCreator.filtersChanged() }
        }
    }
}
