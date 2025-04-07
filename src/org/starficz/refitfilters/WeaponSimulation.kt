package org.starficz.refitfilters

import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.loading.specs.BaseWeaponSpec
import org.starficz.UIFramework.ReflectionUtils
import org.starficz.UIFramework.ReflectionUtils.get
import org.starficz.UIFramework.addPara
import org.starficz.UIFramework.getChildrenCopy
/*
fun UIPanelAPI.initWeaponSimulationListener(width: Float, height: Float): CustomPanelAPI {

    var lastWeaponId = ""
    //Used to add the ability to quickly simulate combat with a weapon that is currently hovered over.
    fun addSimulationTrigger(element: TooltipMakerAPI) {
        var advancer = element.addLunaElement(0f, 0f)

        advancer.onInput { events ->
            for (event in events) {
                if (event.isConsumed) continue
                if (event.isKeyDownEvent && event.eventValue == Keyboard.KEY_F1) {
                    if (!docked) {
                        //Global.getSoundPlayer().playUISound("", 1f, 1f)
                        Global.getSector().campaignUI.messageDisplay.addMessage("Weapon simulations can only be performed while docked.")
                    } else {
                        startSimulation(lastWeaponId)
                    }
                    event.consume()
                }
            }
        }

        advancer.advance {

            var tooltipPair = ReflectionUtils.invoke("getActiveTooltip", weaponPickerDialog, declared = true)
            if (tooltipPair != null) {

                var cargoTooltip = ReflectionUtils.get("two", tooltipPair) as TooltipMakerAPI? ?: return@advance

                var weaponField = ReflectionUtils.findFieldsOfType(cargoTooltip, BaseWeaponSpec::class.java).firstOrNull() ?: return@advance
                var weapon = weaponField.get(cargoTooltip) as WeaponSpecAPI
                lastWeaponId = weapon.weaponId

                var panel = cargoTooltip.getChildrenCopy().firstOrNull() as UIPanelAPI

                if (simControllerElement != null && panel.getChildrenCopy().contains(simControllerElement!!)) {
                    return@advance
                }


                simControllerElement = cargoTooltip.addLunaElement(0f, 0f).apply {  }.elementPanel

                cargoTooltip!!.position.setSize(cargoTooltip.getWidth(), cargoTooltip.getHeight() + 20)

                var color = Misc.getHighlightColor()
                if (!docked) color = Misc.getNegativeHighlightColor()

                cargoTooltip.addSpacer(5f)
                cargoTooltip.addPara("[F1] Weapon Simulation", 0f, Misc.getTextColor(), color, "F1")

            }

        }
    }

    fun startSimulation(weaponID: String)
    {
        var fakePlayerFleet = Global.getFactory().createEmptyFleet(Factions.PLAYER, "Test", true)
        Global.getSector().playerFleet.containingLocation.addEntity(fakePlayerFleet)
        fakePlayerFleet.setCircularOrbit(Global.getSector().playerFleet, 0.1f, 0.1f, 0.1f)

        var member = fakePlayerFleet.addShip("hyperion_Hull", FleetMemberType.SHIP)
        var variant = member.variant

        variant.addWeapon("WS 003", weaponID)

        variant.numFluxVents = 30
        variant.numFluxCapacitors = 30

        var enemyFleet = Global.getFactory().createEmptyFleet(Factions.HEGEMONY, "Test", true)
        var enemyEnforcer = enemyFleet.addShip("enforcer_Hull", FleetMemberType.SHIP)
        enemyEnforcer.variant.numFluxCapacitors = 20
        enemyEnforcer.variant.numFluxVents = 20

        var enemyMedusa = enemyFleet.addShip("medusa_Hull", FleetMemberType.SHIP)
        enemyMedusa.variant.numFluxCapacitors = 20
        enemyMedusa.variant.numFluxVents = 20

        val bcc = BattleCreationContext(fakePlayerFleet, FleetGoal.ATTACK, enemyFleet, FleetGoal.ATTACK)
        bcc.aiRetreatAllowed = false
        bcc.objectivesAllowed = false
        bcc.standoffRange = 0f

        CampaignEngine.getInstance().getCampaignUI().startBattle(bcc)

        Global.getSector().playerFleet.containingLocation.removeEntity(fakePlayerFleet)

        Global.getCombatEngine().addPlugin( object : EveryFrameCombatPlugin {

            var init = false
            var enemies = ArrayList<ShipAPI>()

            override fun init(engine: CombatEngineAPI?) {
            }

            override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>?) {
                events!!.forEach {
                    if (it.isConsumed) return@forEach
                    if (it.isKeyDownEvent && it.eventValue == Keyboard.KEY_ESCAPE)
                    {
                        if (Global.getCombatEngine()?.isCombatOver == false) {
                            Global.getCombatEngine().endCombat(0f)
                            //it.consume()
                        }
                    }
                }
            }

            override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
                var playership = Global.getCombatEngine().playerShip
                playership.mutableStats.armorDamageTakenMult.modifyMult("weapon_sim", 0f)
                playership.mutableStats.hullDamageTakenMult.modifyMult("weapon_sim", 0f)

                if (!init && Global.getCombatEngine().ships.filter { it.owner != 0 }.isNotEmpty()) {
                    var enemyShips = Global.getCombatEngine().ships.filter { it.owner != 0 }

                    for (enemy in enemyShips) {
                        enemy.shipAI = null
                        enemies.add(enemy)
                    }

                    var medusa = enemies.find { it.hullSpec.hullId.contains("medusa") } ?: return
                    var enforcer = enemies.find { it.hullSpec.hullId.contains("enforcer") } ?: return

                    medusa!!.turnOffTravelDrive()
                    enforcer.turnOffTravelDrive()

                    medusa!!.velocity.set(Vector2f(0f, 0f))
                    enforcer.velocity.set(Vector2f(0f, 0f))

                    medusa!!.location.set(Vector2f(-400f, -650f))
                    enforcer.location.set(Vector2f(400f, -650f))



                    init = true

                }

                for (enemy in enemies) {

                    enemy.velocity.set(Vector2f(enemy.velocity.x * 0.98f, enemy.velocity.y * 0.98f))
                    enemy.angularVelocity *= 0.99f

                    if (enemy.shield?.isOff == true) {
                        enemy.shield?.toggleOn()
                    }
                    enemy.shield?.forceFacing(enemy.facing)
                }
            }

            override fun renderInWorldCoords(viewport: ViewportAPI?) {

            }

            override fun renderInUICoords(viewport: ViewportAPI?) {

            }

        })
        // Global.getCombatEngine().endCombat(10f)
    }
}*/