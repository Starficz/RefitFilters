package org.starficz.refitfilters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.components.util.TooltipHelper
import lunalib.lunaExtensions.addLunaElement
import lunalib.lunaExtensions.addLunaSpriteElement
import lunalib.lunaUI.elements.LunaSpriteElement
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.lang.Math.round

class PanelCreator(var weaponPickerDialog: UIPanelAPI, var openedFromCampaign: Boolean) {
    var restrictedWeaponsID: Set<String> = HashSet()
    lateinit var kineticButton: ButtonAPI
    lateinit var keIcon: LunaSpriteElement
    lateinit var heButton: ButtonAPI
    lateinit var heIcon: LunaSpriteElement
    lateinit var energyButton: ButtonAPI
    lateinit var energyIcon: LunaSpriteElement
    lateinit var fragButton: ButtonAPI
    lateinit var fragIcon: LunaSpriteElement

    lateinit var projectileButton: ButtonAPI
    lateinit var beamButton: ButtonAPI
    lateinit var pdButton: ButtonAPI
    lateinit var nonpdButton: ButtonAPI
    lateinit var newFiltersPanel: CustomPanelAPI

    lateinit var rangeSlider: RangeSlider

    val kineticIconString: String = "graphics/ui/icons/damagetype_kinetic.png"
    val highExplosiveIconString: String = "graphics/ui/icons/damagetype_high_explosive.png"
    val energyIconString: String = "graphics/ui/icons/damagetype_energy.png"
    val fragmentationIconString: String = "graphics/ui/icons/damagetype_fragmentation.png"
    val keColor = Color(199,182,158)
    val heColor = Color(208,52,56)
    val energyColor = Color(125, 194, 255)
    val fragColor = Color(255, 255, 131)

    fun init() : CustomPanelAPI {

        val width = 300f
        val height = 50f

        newFiltersPanel = Global.getSettings().createCustom(width, height, null)

        val element = newFiltersPanel.createUIElement(width, height, false)
        newFiltersPanel.addUIElement(element)
        element.position.inTR(0f, 0f)

        element.addLunaElement(0f, 0f).apply {
            renderBorder = false
            advance {
                updateFilterValues()
            }
        }


        val innerCustom = element

        filterWeapons()

        innerCustom.setAreaCheckboxFont("graphics/fonts/victor14.fnt")


        val standardColor = Global.getSettings().brightPlayerColor

        projectileButton = innerCustom.addAreaCheckbox("PROJECTILE", null, standardColor.darker(), standardColor.darker().darker().darker(), standardColor, 93f, 25f, 0f).apply {
            isChecked = ModPlugin.projectileActive
            position.setXAlignOffset(-5f)
            setShortcut(8, true)
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show projectile weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)

        beamButton = innerCustom.addAreaCheckbox("BEAM", null, standardColor.darker(), standardColor.darker().darker().darker(),  standardColor, 94f, 25f, 1f).apply {
            isChecked = ModPlugin.beamActive
            position.rightOfTop(projectileButton, 1f)
            setShortcut(9, true)
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show beam weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)

        pdButton = innerCustom.addAreaCheckbox("PD", null, standardColor.darker(), standardColor.darker().darker().darker(), standardColor, 94f, 25f, 1f).apply {
            isChecked = ModPlugin.pdActive
            position.rightOfTop(beamButton, 1f)
            setShortcut(10, true)
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show point defense weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)

        nonpdButton = innerCustom.addAreaCheckbox("NON-PD", null, standardColor.darker(), standardColor.darker().darker().darker(), standardColor, 93f, 25f, 1f).apply {
            isChecked = ModPlugin.nonpdActive
            position.rightOfTop(pdButton, 1f)
            setShortcut(11, true)
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show standard weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)

        kineticButton = innerCustom.addAreaCheckbox("", null, keColor.darker(), keColor.darker().darker(), keColor, 25f, 25f, 0f).apply {
            glowBrightness = 0.5f
            isChecked = ModPlugin.kineticActive
            position.belowLeft(projectileButton, 1f)
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show kinetic weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)
        keIcon = innerCustom.addLunaSpriteElement(kineticIconString, LunaSpriteElement.ScalingTypes.STRETCH_SPRITE, 25f,25f).apply {
            onClick {
                playClickSound()
                damageTypeClicked(kineticButton)
            }
            position.setYAlignOffset(25f)
            updateIcon(DamageType.KINETIC, getSprite(), kineticButton)
        }

        heButton = innerCustom.addAreaCheckbox("", null, heColor.darker(), heColor.darker().darker(), heColor.brighter(), 25f, 25f, 1f).apply {
            isChecked = ModPlugin.heActive
            position.rightOfTop(kineticButton, 1f)
            glowBrightness = 0.5f
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show high explosive weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)
        heIcon = innerCustom.addLunaSpriteElement(highExplosiveIconString, LunaSpriteElement.ScalingTypes.STRETCH_SPRITE, 25f,25f).apply {
            onClick {
                playClickSound()
                damageTypeClicked(heButton)
            }
            updateIcon(DamageType.HIGH_EXPLOSIVE, getSprite(), heButton)
            position.setYAlignOffset(25f)
        }

        energyButton = innerCustom.addAreaCheckbox("", null, energyColor.darker(), energyColor.darker().darker(), energyColor, 25f, 25f, 1f).apply {
            isChecked = ModPlugin.energyActive
            position.rightOfTop(heButton, 1f)
            glowBrightness = 0.5f
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show energy weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)
        energyIcon = innerCustom.addLunaSpriteElement(energyIconString, LunaSpriteElement.ScalingTypes.STRETCH_SPRITE, 25f,25f).apply {
            onClick {
                playClickSound()
                damageTypeClicked(energyButton)
            }
            position.setYAlignOffset(25f)
            updateIcon(DamageType.ENERGY, getSprite(), energyButton)
        }

        fragButton = innerCustom.addAreaCheckbox("", null, fragColor.darker(), fragColor.darker().darker(), fragColor, 25f, 25f, 1f).apply {
            isChecked = ModPlugin.fragActive
            position.rightOfTop(energyButton, 1f)
            glowBrightness = 0.5f
        }
        innerCustom.addTooltipToPrevious(TooltipHelper("Show fragmentation weapons.", 300f), TooltipMakerAPI.TooltipLocation.ABOVE)
        fragIcon = innerCustom.addLunaSpriteElement(fragmentationIconString, LunaSpriteElement.ScalingTypes.STRETCH_SPRITE, 25f,25f).apply {
            onClick {
                playClickSound()
                damageTypeClicked(fragButton)
            }
            position.setYAlignOffset(25f)
            updateIcon(DamageType.FRAGMENTATION, getSprite(), fragButton)
        }
        innerCustom.setParaFontVictor14()
        val rangeLabel = innerCustom.addPara("RANGE", 1f)
        rangeLabel.position.rightOfMid(fragButton, 6f)

        rangeSlider = RangeSlider(innerCustom, 200f, 25f, ModPlugin.lowerRange.toFloat(), ModPlugin.upperRange.toFloat(), ModPlugin.minRange.toFloat(), ModPlugin.maxRange.toFloat()).apply {
            position.rightOfTop(fragButton, 55f)
            backgroundColor = Misc.getHighlightColor().darker().darker().darker()
            borderColor = Misc.getHighlightColor().darker()
        }

        return newFiltersPanel
    }

    fun filterWeapons(){
        if(openedFromCampaign){
            filterCargos()
        } else{
            cacheAllRestrictedWeaponID()
            filterByAddingRestrictedTags()
            reloadWeaponPicker()
            revertRestrictedTags()
        }
    }

    fun filterCargos(){
        val playerCargo = Global.getSector().playerFleet.cargo
        val cargos = ModPlugin.currentEntityCargos

        val originalCargoMap = mutableMapOf<CargoAPI, CargoAPI>()
        originalCargoMap[playerCargo] = playerCargo.createCopy()
        for(cargo in cargos){
            originalCargoMap[cargo] = cargo.createCopy()
        }

        for(cargo in originalCargoMap){
            for(weaponStack in cargo.key.weapons){
                val spec = Global.getSettings().getWeaponSpec(weaponStack.item)
                if(weaponFiltered(spec)) cargo.key.removeWeapons(weaponStack.item, weaponStack.count)
            }
        }

        reloadWeaponPicker()

        for(cargo in originalCargoMap){
            cargo.key.clear()
            cargo.key.addAll(cargo.value)
        }
    }

    fun reloadWeaponPicker() {
        ReflectionUtils.invoke("notifyFilterChanged", weaponPickerDialog) //Refresh the WeaponPickerDialog

        // shift weapons down
        val additionalHeight = 50f
        val innerWeaponPanel = ReflectionUtils.invoke("getInnerPanel", weaponPickerDialog) as UIPanelAPI
        innerWeaponPanel.addComponent(newFiltersPanel)

        var index = 0
        val uiElements = innerWeaponPanel.getChildrenCopy()
        for(uiElement in uiElements){
            if (ReflectionUtils.hasMethodOfName("addItem", uiElement)){
                val existingFilters = uiElements[index]
                val weaponsList = uiElements[index+1]
                val eitherNoWeaponsOrNewFilters = uiElements[index+2]
                val newFilters = uiElements[uiElements.size - 1]

                newFilters.position.belowLeft(existingFilters, 0f)
                weaponsList.position.belowLeft(newFilters, 4f)
                if(eitherNoWeaponsOrNewFilters != newFilters){
                    eitherNoWeaponsOrNewFilters.position.belowLeft(newFilters, 0f)
                }
                break
            }
            index += 1
        }

        val knownFloats = listOf("PAD", "ITEM_WIDTH", "ITEM_HEIGHT", "origXAlignOffset")
        val knownValues = listOf(382.0f, 385.0f, 0f) // this is so jank, but there really is no other way to get this value
        val foundFloats = mutableMapOf<String, Float>()
        for(fieldStr in ReflectionUtils.getFieldsOfType(weaponPickerDialog, Float::class.java)){
            if(fieldStr !in knownFloats){
                val foundFloat = ReflectionUtils.get(fieldStr, weaponPickerDialog) as Float
                if (foundFloat !in knownValues) foundFloats.put(fieldStr, foundFloat)
            }
        }

        if(foundFloats.size == 1){
            val heightField = foundFloats.keys.first()

            val height = ReflectionUtils.get(heightField, weaponPickerDialog) as Float
            ReflectionUtils.set(heightField, weaponPickerDialog, height + additionalHeight)

            ReflectionUtils.invoke("setSize", weaponPickerDialog, 400f, height + additionalHeight)
        } else{
            throw Exception("Unable to differentiate weaponPickerDialog's obf height field")
        }
    }

    fun revertRestrictedTags(){
        for(spec in Global.getSettings().getAllWeaponSpecs()){
            if(Tags.RESTRICTED in spec.tags && spec.weaponId !in restrictedWeaponsID) spec.tags.remove(Tags.RESTRICTED)
        }
    }

    fun filterByAddingRestrictedTags(){
        for(spec in Global.getSettings().getAllWeaponSpecs()){
            if(Tags.RESTRICTED !in spec.tags && weaponFiltered(spec)) spec.addTag(Tags.RESTRICTED)
        }
    }

    fun weaponFiltered(weaponSpec: WeaponSpecAPI): Boolean{
        if(weaponSpec.damageType == DamageType.KINETIC && !ModPlugin.kineticActive) return true
        if(weaponSpec.damageType == DamageType.HIGH_EXPLOSIVE && !ModPlugin.heActive) return true
        if(weaponSpec.damageType == DamageType.ENERGY && !ModPlugin.energyActive) return true
        if(weaponSpec.damageType == DamageType.FRAGMENTATION && !ModPlugin.fragActive) return true

        if(!weaponSpec.isBeam && !ModPlugin.projectileActive) return true
        if(weaponSpec.isBeam && !ModPlugin.beamActive) return true

        if((weaponSpec.getAIHints().contains(WeaponAPI.AIHints.PD) || weaponSpec.getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)) && !ModPlugin.pdActive) return true
        if(!(weaponSpec.getAIHints().contains(WeaponAPI.AIHints.PD) || weaponSpec.getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)) && !ModPlugin.nonpdActive) return true

        if(weaponSpec.maxRange < ModPlugin.lowerRange && ModPlugin.lowerRange != ModPlugin.minRange) return true
        if(weaponSpec.maxRange > ModPlugin.upperRange && ModPlugin.upperRange != ModPlugin.maxRange) return true

        return false
    }

    fun cacheAllRestrictedWeaponID(){
        val restrictedWeps = Global.getSettings().allWeaponSpecs.filter { Tags.RESTRICTED in it.tags }
        restrictedWeaponsID = restrictedWeps.map { it.weaponId }.toSet()
    }

    fun updateFilterValues(){

        // handle click logic for dual beam/projectile
        if(beamButton.isChecked != ModPlugin.beamActive){
            beamButton.isChecked = true
            if(ModPlugin.projectileActive) projectileButton.isChecked = false else projectileButton.isChecked = true
        }
        else if(projectileButton.isChecked != ModPlugin.projectileActive){
            projectileButton.isChecked = true
            if(ModPlugin.beamActive) beamButton.isChecked = false else beamButton.isChecked = true
        }


        // handle click logic for dual pd/nonpd
        if(pdButton.isChecked != ModPlugin.pdActive){
            pdButton.isChecked = true
            if(ModPlugin.nonpdActive) nonpdButton.isChecked = false else nonpdButton.isChecked = true
        }
        else if(nonpdButton.isChecked != ModPlugin.nonpdActive){
            nonpdButton.isChecked = true
            if(ModPlugin.pdActive) pdButton.isChecked = false else pdButton.isChecked = true
        }

        val buttonMappings = listOf(
            kineticButton to { ModPlugin::kineticActive },
            heButton to { ModPlugin::heActive },
            energyButton to { ModPlugin::energyActive },
            fragButton to { ModPlugin::fragActive },
            projectileButton to { ModPlugin::projectileActive },
            beamButton to { ModPlugin::beamActive },
            pdButton to { ModPlugin::pdActive },
            nonpdButton to { ModPlugin::nonpdActive }
        )

        // map buttons to backend filters
        var filtersChanged = false
        buttonMappings.forEach { (button, property) ->
            val currentValue = property().get()
            if (currentValue != button.isChecked) {
                property().set(button.isChecked)
                filtersChanged = true
            }
        }

        // map range slider to upper/lower range
        if(round(rangeSlider.getCurrentMinValue())!= ModPlugin.lowerRange){
            ModPlugin.lowerRange = rangeSlider.getCurrentMinValue().toInt()
            filtersChanged = true
        }
        if(round(rangeSlider.getCurrentMaxValue()) != ModPlugin.upperRange){
            ModPlugin.upperRange = rangeSlider.getCurrentMaxValue().toInt()
            filtersChanged = true
        }

        // if filters changed, run script
        if(filtersChanged) filterWeapons()
    }

    fun damageTypeClicked(clicked: ButtonAPI){
        clicked.isChecked = !clicked.isChecked
        val allDisabled = !kineticButton.isChecked && !heButton.isChecked && !energyButton.isChecked && !fragButton.isChecked
        val damageTypeButtons = arrayOf(kineticButton, heButton, energyButton, fragButton)

        for(button in damageTypeButtons){
            if(allDisabled){
                button.isChecked = true
                continue
            }
            if(!(Keyboard.isKeyDown(42) && Keyboard.isKeyDown(29))){
                if(button == clicked) button.isChecked = true
                else button.isChecked = false
            }
        }
        updateIcon(DamageType.KINETIC, keIcon.getSprite(), kineticButton)
        updateIcon(DamageType.HIGH_EXPLOSIVE, heIcon.getSprite(), heButton)
        updateIcon(DamageType.FRAGMENTATION, fragIcon.getSprite(), fragButton)
        updateIcon(DamageType.ENERGY, energyIcon.getSprite(), energyButton)
    }

    fun updateIcon(damageType: DamageType, iconSprite: SpriteAPI, button: ButtonAPI){
        val activeColor = when (damageType){
            DamageType.KINETIC -> keColor
            DamageType.HIGH_EXPLOSIVE -> heColor
            DamageType.FRAGMENTATION -> fragColor
            DamageType.ENERGY -> energyColor
            DamageType.OTHER -> keColor
        }
        if(button.isChecked){
            iconSprite.setAdditiveBlend()
            iconSprite.alphaMult = 0.9f
            iconSprite.color = activeColor
            button.glowBrightness = 0.5f
        } else{
            iconSprite.setNormalBlend()
            iconSprite.alphaMult = 1f
            iconSprite.color = activeColor.darker()
            button.glowBrightness = 1f
        }
    }
}