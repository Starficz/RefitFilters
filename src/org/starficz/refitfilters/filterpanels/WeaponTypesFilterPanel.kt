package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.staruiframework.*
import org.starficz.refitfilters.PickerPanelHelpers
import org.starficz.refitfilters.WeaponFilterData

fun UIPanelAPI.createWeaponTypesFilterPanel(
    targetWidth: Float,
    targetHeight: Float,
    pickerPanel: UIPanelAPI,
    filterData: WeaponFilterData
): CustomPanelAPI {

    val brightColor = Global.getSettings().basePlayerColor
    val baseColor = brightColor.darker()
    val bgColor = baseColor.darker().darker()

    return HorizontalStackLayout(spacing = 1f, alignment = Alignment.LMID) {
        var needsRefresh = false
        Plugin {
            advance {
                if (needsRefresh) {
                    PickerPanelHelpers.filtersChanged(pickerPanel)
                    needsRefresh = false
                }
            }
        }

        filterData.projectileWeapons.onChange { needsRefresh = true }
        filterData.beamWeapons.onChange { needsRefresh = true }
        filterData.pdWeapons.onChange { needsRefresh = true }
        filterData.nonpdWeapons.onChange { needsRefresh = true }
        filterData.ammoWeapons.onChange { needsRefresh = true }
        filterData.nonAmmoWeapons.onChange { needsRefresh = true }


        val beamGroup = ButtonGroup()
        val pdGroup = ButtonGroup()
        val ammoGroup = ButtonGroup()
        val buttonWidth = 62f

        AreaCheckbox(buttonWidth, targetHeight, "PROJ", baseColor, bgColor, brightColor,
            font = Font.VICTOR_14, bind = filterData.projectileWeapons, buttonGroup = beamGroup) {
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara("Show projectile weapons.", 0f) }
        }

        AreaCheckbox(buttonWidth, targetHeight, "BEAM", baseColor, bgColor, brightColor,
            font = Font.VICTOR_14, bind = filterData.beamWeapons, buttonGroup = beamGroup) {
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara("Show beam weapons.", 0f) }
        }

        AreaCheckbox(buttonWidth - 10f, targetHeight, "PD", baseColor, bgColor, brightColor,
            font = Font.VICTOR_14, bind = filterData.pdWeapons, buttonGroup = pdGroup) {
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara("Show point defense weapons.", 0f) }
        }

        AreaCheckbox(buttonWidth + 10f, targetHeight, "NON-PD", baseColor, bgColor, brightColor,
            font = Font.VICTOR_14, bind = filterData.nonpdWeapons, buttonGroup = pdGroup) {
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara("Show weapons that aren't point defense.", 0f) }
        }

        AreaCheckbox(buttonWidth - 10f, targetHeight, "AMMO", baseColor, bgColor, brightColor,
            font = Font.VICTOR_14, bind = filterData.ammoWeapons, buttonGroup = ammoGroup) {
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara("Show weapons that use ammo.", 0f) }
        }

        AreaCheckbox(buttonWidth + 10f, targetHeight, "NON-AMMO", baseColor, bgColor, brightColor,
            font = Font.VICTOR_14, bind = filterData.nonAmmoWeapons, buttonGroup = ammoGroup) {
            Tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara("Show weapons that don't use ammo.", 0f) }
        }
    }
}