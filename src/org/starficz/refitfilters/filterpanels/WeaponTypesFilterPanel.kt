package org.starficz.refitfilters.filterpanels

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.starficz.refitfilters.PickerPanelHelpers.refreshPickerOn
import org.starficz.refitfilters.WeaponFilterData
import org.starficz.starui.Alignment
import org.starficz.starui.components.AreaCheckbox
import org.starficz.starui.components.FilterGroup
import org.starficz.starui.components.Row
import org.starficz.starui.modifier.Modifier
import org.starficz.starui.modifier.tooltip
import org.starficz.starui.Size
import org.starficz.starui.UIState

/** PROJ / BEAM, PD / NON-PD, AMMO / NON-AMMO: three button pairs, each its own group. Colours and font: the filter theme. */
fun UIPanelAPI.createWeaponTypesFilterPanel(
    targetWidth: Float,
    targetHeight: Float,
    pickerPanel: UIPanelAPI,
    filterData: WeaponFilterData
): CustomPanelAPI = with(filterData) {
    // Exact size: FilterPanelCreator adds up the panels' heights right after creating them, before the first layout.
    Row(
        Size.exact(targetWidth, targetHeight),
        Modifier.refreshPickerOn(pickerPanel, projectileWeapons, beamWeapons, pdWeapons, nonpdWeapons, ammoWeapons, nonAmmoWeapons),
        verticalAlignment = Alignment.CenterVertically, spacing = 1f
    ) {
        val beamGroup = FilterGroup()
        val pdGroup = FilterGroup()
        val ammoGroup = FilterGroup()
        val buttonWidth = 62f

        fun typeButton(text: String, width: Float, state: UIState<Boolean>, group: FilterGroup, tooltipText: String) {
            AreaCheckbox(
                text, state, Size.exact(width, targetHeight),
                Modifier.tooltip(300f, TooltipMakerAPI.TooltipLocation.ABOVE) { addPara(tooltipText, 0f) },
                buttonGroup = group
            )
        }

        typeButton("PROJ", buttonWidth, projectileWeapons, beamGroup, "Show projectile weapons.")
        typeButton("BEAM", buttonWidth, beamWeapons, beamGroup, "Show beam weapons.")
        typeButton("PD", buttonWidth - 10f, pdWeapons, pdGroup, "Show point defense weapons.")
        typeButton("NON-PD", buttonWidth + 10f, nonpdWeapons, pdGroup, "Show weapons that aren't point defense.")
        typeButton("AMMO", buttonWidth - 10f, ammoWeapons, ammoGroup, "Show weapons that use ammo.")
        typeButton("NON-AMMO", buttonWidth + 10f, nonAmmoWeapons, ammoGroup, "Show weapons that don't use ammo.")
    }
}
