package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.WeaponFilterData
import org.starficz.starui.compose.Size
import org.starficz.starui.compose.components.AreaCheckbox
import org.starficz.starui.compose.components.Row
import org.starficz.starui.compose.Alignment
import org.starficz.starui.compose.modifier.Modifier
import org.starficz.starui.compose.modifier.tooltip
import org.starficz.starui.compose.runtime.ButtonGroup
import org.starficz.starui.compose.runtime.UIState
import org.starficz.starui.core.Font

fun UIPanelAPI.createWeaponTypesFilterPanel(
    targetWidth: Float,
    targetHeight: Float,
    pickerPanel: UIPanelAPI,
    filterData: WeaponFilterData
): CustomPanelAPI {

    val brightColor = Global.getSettings().basePlayerColor
    val baseColor = brightColor.darker()
    val bgColor = baseColor.darker().darker()

    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    return Row(Size.exact(targetWidth, targetHeight), verticalAlignment = Alignment.CenterVertically, spacing = 1f) {
        with(filterData) {
            refreshPickerOn(pickerPanel, projectileWeapons, beamWeapons, pdWeapons, nonpdWeapons, ammoWeapons, nonAmmoWeapons)
        }

        val beamGroup = ButtonGroup()
        val pdGroup = ButtonGroup()
        val ammoGroup = ButtonGroup()
        val buttonWidth = 62f

        fun typeButton(text: String, width: Float, state: UIState<Boolean>, group: ButtonGroup, tooltipText: String) {
            AreaCheckbox(
                text, state, Size.exact(width, targetHeight),
                Modifier.tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara(tooltipText, 0f) },
                baseColor = baseColor, darkColor = bgColor, brightColor = brightColor, font = Font.VICTOR_14,
                buttonGroup = group
            )
        }

        typeButton("PROJ", buttonWidth, filterData.projectileWeapons, beamGroup, "Show projectile weapons.")
        typeButton("BEAM", buttonWidth, filterData.beamWeapons, beamGroup, "Show beam weapons.")
        typeButton("PD", buttonWidth - 10f, filterData.pdWeapons, pdGroup, "Show point defense weapons.")
        typeButton("NON-PD", buttonWidth + 10f, filterData.nonpdWeapons, pdGroup, "Show weapons that aren't point defense.")
        typeButton("AMMO", buttonWidth - 10f, filterData.ammoWeapons, ammoGroup, "Show weapons that use ammo.")
        typeButton("NON-AMMO", buttonWidth + 10f, filterData.nonAmmoWeapons, ammoGroup, "Show weapons that don't use ammo.")
    }
}
