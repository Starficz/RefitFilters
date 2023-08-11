package org.starficz.refitfilters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponGroupType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;

public class MainGUI extends BaseHullMod {

    static HashMap<String, WeaponWhitelist> fleetIDWeaponWhitelist = new HashMap<>();
    static String currentFleetMemberID;
    static final float PAD = 10f;
    static final String kineticIcon = "graphics/ui/icons/damagetype_kinetic.png";
    static final String kineticIconDesaturated = Global.getSettings().getSpriteName("icons", "damagetype_kinetic_desaturated");
    static final String highExplosiveIcon = "graphics/ui/icons/damagetype_high_explosive.png";
    static final String highExplosiveIconDesaturated = Global.getSettings().getSpriteName("icons", "damagetype_high_explosive_desaturated");
    static final String energyIcon = "graphics/ui/icons/damagetype_energy.png";
    static final String energyIconDesaturated = Global.getSettings().getSpriteName("icons", "damagetype_energy_desaturated");
    static final String fragmentationIcon = "graphics/ui/icons/damagetype_fragmentation.png";
    static final String fragmentationIconDesaturated = Global.getSettings().getSpriteName("icons", "damagetype_fragmentation_desaturated");
    static final String beamIcon = "graphics/icons/skills/energy_weapon_mastery.png";
    static final String beamIconDesaturated = Global.getSettings().getSpriteName("icons", "energy_weapon_mastery_desaturated");
    static final String projectileIcon = "graphics/icons/skills/ranged_specialization.png";
    static final String projectileIconDesaturated = Global.getSettings().getSpriteName("icons", "ranged_specialization_desaturated");
    static final String pointDefenseIcon = "graphics/warroom/taskicons/icon_search_and_destroy.png";
    static final String pointDefenseIconDesaturated = Global.getSettings().getSpriteName("icons", "icon_search_and_destroy_desaturated");
    static final String nonPointDefenseIcon = "graphics/warroom/taskicons/icon_set_target.png";
    static final String nonPointDefenseIconDesaturated = Global.getSettings().getSpriteName("icons", "icon_set_target_desaturated");
    static class WeaponWhitelist
    {
        public int activeTab = 0;
        public boolean firstRangeBracket = true;
        public boolean secondRangeBracket = true;
        public boolean thirdRangeBracket = true;
        public boolean fourthRangeBracket = true;
        public boolean kinetic = true;
        public boolean highExplosive = true;
        public boolean energy = true;
        public boolean fragmentation = true;
        public boolean beam = true;
        public boolean projectile = true;
        public boolean pointDefense = true;
        public boolean nonPointDefense = true;
    };

    FleetMemberAPI currentShip;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.removeListenerOfClass(RefitFiltersCostListener.class);
        stats.addListener(new RefitFiltersCostListener());

        if(stats.getFleetMember() == null) return;
        if (!fleetIDWeaponWhitelist.containsKey(stats.getFleetMember().getId())){
            fleetIDWeaponWhitelist.put(stats.getFleetMember().getId(), new WeaponWhitelist());
        }
        currentFleetMemberID = stats.getFleetMember().getId();
        removeFilterExceptThisShip(stats.getFleetMember());
        updateOPAdjustment(stats.getVariant());
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.getVariant().hasHullMod("RF_OPAdjuster")) {
            ship.getVariant().addMod("RF_OPAdjuster");
        }
        removeFilterExceptThisShip(ship.getFleetMember());
        updateOPAdjustment(ship.getVariant());
    }

    public void removeFilterExceptThisShip(FleetMemberAPI ship){
        if (ship == null || ship.getFleetData() == null) return;
        for(FleetMemberAPI fleetMember : ship.getFleetData().getMembersListCopy()){
            if (fleetMember == ship) continue;

            if (fleetMember.getVariant().hasHullMod("RF_MainGUI"))
                fleetMember.getVariant().removeMod("RF_MainGUI");

            if (fleetMember.getVariant().hasHullMod("RF_OPAdjuster"))
                fleetMember.getVariant().removeMod("RF_OPAdjuster");
        }
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (amount > 0 && member.getVariant().hasHullMod("RF_MainGUI"))
            member.getVariant().removeMod("RF_MainGUI");
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    // Description tooltips are nightmares of UI and subtooltip generation, the following was my best attempt at organizing this code. It still isn't very readable.
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if(isForModSpec || ship == null) return;


        // get and update data, currently in the form of tags in the ship variant, might cause issues? idk
        WeaponWhitelist weaponWhitelist = fleetIDWeaponWhitelist.get(ship.getMutableStats().getFleetMember().getId());
        if (weaponWhitelist == null) return;
        processInputs(ship, weaponWhitelist);


        // start first tab
        tooltip.addSectionHeading("Range", weaponWhitelist.activeTab == 0 ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor(), weaponWhitelist.activeTab == 0 ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(), Alignment.MID, PAD);
        int currentTab = 0;

        TooltipMakerAPI firstRangeSeparatorTT = tooltip.beginSubTooltip( width/4);
        firstRangeSeparatorTT.addPara("400", (weaponWhitelist.firstRangeBracket || weaponWhitelist.secondRangeBracket) ? Misc.getHighlightColor() : Misc.getGrayColor(), PAD).setAlignment(Alignment.MID);
        tooltip.endSubTooltip();

        TooltipMakerAPI secondRangeSeparatorTT = tooltip.beginSubTooltip( width/4);
        secondRangeSeparatorTT.addPara("700", (weaponWhitelist.secondRangeBracket || weaponWhitelist.thirdRangeBracket) ? Misc.getHighlightColor() : Misc.getGrayColor(), PAD).setAlignment(Alignment.MID);
        tooltip.endSubTooltip();

        TooltipMakerAPI thirdRangeSeparatorTT = tooltip.beginSubTooltip( width/4);
        thirdRangeSeparatorTT.addPara("1000", (weaponWhitelist.thirdRangeBracket || weaponWhitelist.fourthRangeBracket) ? Misc.getHighlightColor() : Misc.getGrayColor(), PAD).setAlignment(Alignment.MID);
        tooltip.endSubTooltip();

        // need to shift the separators meaning we cant use addCustom() as that will shift everything else following this point, why? idk. crazy BS, ask Alex?
        tooltip.addCustomDoNotSetPosition(firstRangeSeparatorTT);
        tooltip.addCustomDoNotSetPosition(secondRangeSeparatorTT);
        tooltip.addCustomDoNotSetPosition(thirdRangeSeparatorTT);

        UIComponentAPI anchor = tooltip.addSpacer(0f);

        firstRangeSeparatorTT.getPosition().rightOfMid(anchor, 0f);
        firstRangeSeparatorTT.getPosition().setXAlignOffset(width/8-PAD);
        firstRangeSeparatorTT.getPosition().setYAlignOffset(-PAD);
        secondRangeSeparatorTT.getPosition().rightOfMid(firstRangeSeparatorTT, 0f);
        thirdRangeSeparatorTT.getPosition().rightOfMid(secondRangeSeparatorTT, 0f);

        Color rangeTickColor = new Color(175,125,0);
        Color rangeWhitelistColor = new Color(0,30,0);
        Color rangeBlacklistColor = new Color(30,0,0);

        // fake range tick marks with colons, the pixels are slightly off this way but anything better is way too much effort
        TooltipMakerAPI firstRangeBracketTT = tooltip.beginSubTooltip( width/4);
        firstRangeBracketTT.addSectionHeading(":       :       :", rangeTickColor, weaponWhitelist.firstRangeBracket ? rangeWhitelistColor : rangeBlacklistColor, Alignment.LMID, PAD);
        addTogglePara(firstRangeBracketTT, 1, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI secondRangeBracketTT = tooltip.beginSubTooltip( width/4);
        secondRangeBracketTT.addSectionHeading(":       :       :", rangeTickColor, weaponWhitelist.secondRangeBracket ? rangeWhitelistColor : rangeBlacklistColor, Alignment.LMID, PAD);
        addTogglePara(secondRangeBracketTT, 2, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI thirdRangeBracketTT = tooltip.beginSubTooltip( width/4);
        thirdRangeBracketTT.addSectionHeading(":       :       :", rangeTickColor, weaponWhitelist.thirdRangeBracket ? rangeWhitelistColor : rangeBlacklistColor, Alignment.LMID, PAD);
        addTogglePara(thirdRangeBracketTT, 3, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI fourthRangeBracketTT = tooltip.beginSubTooltip( width/4);
        fourthRangeBracketTT.addSectionHeading(":       :       :", rangeTickColor, weaponWhitelist.fourthRangeBracket ? rangeWhitelistColor : rangeBlacklistColor, Alignment.LMID, PAD);
        addTogglePara(fourthRangeBracketTT, 4, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        tooltip.addCustom(firstRangeBracketTT, 0f);
        tooltip.addCustomDoNotSetPosition(secondRangeBracketTT);
        tooltip.addCustomDoNotSetPosition(thirdRangeBracketTT);
        tooltip.addCustomDoNotSetPosition(fourthRangeBracketTT);

        firstRangeBracketTT.getPosition().setYAlignOffset(-30f);
        secondRangeBracketTT.getPosition().rightOfMid(firstRangeBracketTT, 0f);
        thirdRangeBracketTT.getPosition().rightOfMid(secondRangeBracketTT, 0f);
        fourthRangeBracketTT.getPosition().rightOfMid(thirdRangeBracketTT, 0f);
        // update the height to account for the y offset here
        tooltip.setHeightSoFar(tooltip.getHeightSoFar()+40f);

        // start second tab
        Color notSelectedColor = new Color(30,30,30);
        tooltip.addSectionHeading("Damage Type", weaponWhitelist.activeTab == 1 ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor(), weaponWhitelist.activeTab == 1 ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(), Alignment.MID, PAD);
        currentTab = 1;

        TooltipMakerAPI kineticTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        kineticTT.addSectionHeading("Kinetic", weaponWhitelist.kinetic ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.kinetic ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        kineticTT.addImage(weaponWhitelist.kinetic ? kineticIcon : kineticIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(kineticTT, 1, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI highExplosiveTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        highExplosiveTT.addSectionHeading("HE", weaponWhitelist.highExplosive ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.highExplosive ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        highExplosiveTT.addImage(weaponWhitelist.highExplosive ? highExplosiveIcon : highExplosiveIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(highExplosiveTT, 2, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI energyTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        energyTT.addSectionHeading("Energy", weaponWhitelist.energy ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.energy ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        energyTT.addImage(weaponWhitelist.energy ? energyIcon : energyIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(energyTT, 3, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI fragmentationTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        fragmentationTT.addSectionHeading("Frag", weaponWhitelist.fragmentation ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.fragmentation ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        fragmentationTT.addImage(weaponWhitelist.fragmentation ? fragmentationIcon : fragmentationIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(fragmentationTT, 4, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();


        tooltip.addCustom(kineticTT, 0f);
        tooltip.addCustomDoNotSetPosition(highExplosiveTT);
        tooltip.addCustomDoNotSetPosition(energyTT);
        tooltip.addCustomDoNotSetPosition(fragmentationTT);

        kineticTT.getPosition().setYAlignOffset(-PAD);
        highExplosiveTT.getPosition().rightOfMid(kineticTT, PAD);
        energyTT.getPosition().rightOfMid(highExplosiveTT, PAD);
        fragmentationTT.getPosition().rightOfMid(energyTT, PAD);


        // start third tab
        tooltip.addSectionHeading("Weapon Type", weaponWhitelist.activeTab == 2 ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor(), weaponWhitelist.activeTab == 2 ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(), Alignment.MID, PAD);
        currentTab = 2;

        TooltipMakerAPI beamTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        beamTT.addSectionHeading("Beam", weaponWhitelist.beam ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.beam ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        beamTT.addImage(weaponWhitelist.beam ? beamIcon : beamIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(beamTT, 1, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI projectileTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        projectileTT.addSectionHeading("Projectile", weaponWhitelist.projectile ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.projectile ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        projectileTT.addImage(weaponWhitelist.projectile ? projectileIcon : projectileIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(projectileTT, 2, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI pointDefenseTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        pointDefenseTT.addSectionHeading("PD", weaponWhitelist.pointDefense ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.pointDefense ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        pointDefenseTT.addImage(weaponWhitelist.pointDefense ? pointDefenseIcon : pointDefenseIconDesaturated, width/4- (PAD*1.5f), PAD);
        addTogglePara(pointDefenseTT, 3, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();

        TooltipMakerAPI nonPointDefenseTT = tooltip.beginSubTooltip( width/4-(PAD*0.75f));
        nonPointDefenseTT.addSectionHeading("Non-PD", weaponWhitelist.nonPointDefense ? Misc.getBrightPlayerColor() : Misc.getGrayColor(), weaponWhitelist.nonPointDefense ? Misc.getDarkPlayerColor() : notSelectedColor, Alignment.MID, PAD);
        nonPointDefenseTT.addImage(weaponWhitelist.nonPointDefense ? nonPointDefenseIcon : nonPointDefenseIconDesaturated, width/4-(PAD*1.5f), PAD);
        addTogglePara(nonPointDefenseTT, 4, currentTab, weaponWhitelist.activeTab);
        tooltip.endSubTooltip();


        tooltip.addCustom(beamTT, 0f);
        tooltip.addCustomDoNotSetPosition(projectileTT);
        tooltip.addCustomDoNotSetPosition(pointDefenseTT);
        tooltip.addCustomDoNotSetPosition(nonPointDefenseTT);

        beamTT.getPosition().setYAlignOffset(-PAD);
        projectileTT.getPosition().rightOfMid(beamTT, PAD);
        pointDefenseTT.getPosition().rightOfMid(projectileTT, PAD);
        nonPointDefenseTT.getPosition().rightOfMid(pointDefenseTT, PAD);


    }

    private static void processInputs(ShipAPI ship, WeaponWhitelist weaponWhitelist){
        if (weaponWhitelist == null) return;

        if (Keyboard.isKeyDown(58)) {
            weaponWhitelist.activeTab = (weaponWhitelist.activeTab + 1) % 3;
        }
        if (Keyboard.isKeyDown(2)) {
            if(weaponWhitelist.activeTab == 0) {weaponWhitelist.firstRangeBracket = !weaponWhitelist.firstRangeBracket;}
            if(weaponWhitelist.activeTab == 1) {weaponWhitelist.kinetic = !weaponWhitelist.kinetic;}
            if(weaponWhitelist.activeTab == 2) {weaponWhitelist.beam = !weaponWhitelist.beam;}
        }
        if (Keyboard.isKeyDown(3)) {
            if(weaponWhitelist.activeTab == 0) {weaponWhitelist.secondRangeBracket = !weaponWhitelist.secondRangeBracket;}
            if(weaponWhitelist.activeTab == 1) {weaponWhitelist.highExplosive = !weaponWhitelist.highExplosive;}
            if(weaponWhitelist.activeTab == 2) {weaponWhitelist.projectile = !weaponWhitelist.projectile;}
        }
        if (Keyboard.isKeyDown(4)) {
            if(weaponWhitelist.activeTab == 0) {weaponWhitelist.thirdRangeBracket = !weaponWhitelist.thirdRangeBracket;}
            if(weaponWhitelist.activeTab == 1) {weaponWhitelist.energy = !weaponWhitelist.energy;}
            if(weaponWhitelist.activeTab == 2) {weaponWhitelist.pointDefense = !weaponWhitelist.pointDefense;}
        }
        if (Keyboard.isKeyDown(5)) {
            if(weaponWhitelist.activeTab == 0) {weaponWhitelist.fourthRangeBracket = !weaponWhitelist.fourthRangeBracket;}
            if(weaponWhitelist.activeTab == 1) {weaponWhitelist.fragmentation = !weaponWhitelist.fragmentation;}
            if(weaponWhitelist.activeTab == 2) {weaponWhitelist.nonPointDefense = !weaponWhitelist.nonPointDefense;}
        }
        updateOPAdjustment(ship.getVariant());
    }

    private static void updateOPAdjustment(ShipVariantAPI ship){
        int totalDiscount = 0;
        for(String slotID : ship.getFittedWeaponSlots()){
            WeaponSlotAPI slot = ship.getSlot(slotID);
            WeaponSpecAPI weapon = ship.getWeaponSpec(slotID);
            if (weapon == null) continue;
            boolean ignoreWeapon = slot.isBuiltIn() || slot.isSystemSlot() || slot.isDecorative();
            if(MainGUI.isBlacklisted(weapon) && !ignoreWeapon){
                totalDiscount += 10000;
            }
        }

        Global.getSettings().getHullModSpec("RF_OPAdjuster").setCapitalCost(-totalDiscount);
        Global.getSettings().getHullModSpec("RF_OPAdjuster").setCruiserCost(-totalDiscount);
        Global.getSettings().getHullModSpec("RF_OPAdjuster").setDestroyerCost(-totalDiscount);
        Global.getSettings().getHullModSpec("RF_OPAdjuster").setFrigateCost(-totalDiscount);
    }

    private static void addTogglePara(TooltipMakerAPI parent, int key, int section, int activeSection){
        if (section == activeSection)
            parent.addPara("(" + key + ") Toggle", PAD, Misc.getGrayColor(), Misc.getHighlightColor(), String.valueOf(key)).setAlignment(Alignment.MID);
    }

    public static class RefitFiltersCostListener implements WeaponOPCostModifier {
        @Override
        public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
            return currCost + 10000 * (isBlacklisted(weapon) ? 1 : 0);
        }
    }

    public static boolean isBlacklisted(WeaponSpecAPI weapon){
        if(weapon.getType() == WeaponAPI.WeaponType.BUILT_IN) return false;
        if(weapon.getType() == WeaponAPI.WeaponType.DECORATIVE) return false;
        if(weapon.getType() == WeaponAPI.WeaponType.LAUNCH_BAY) return false;
        if(weapon.getType() == WeaponAPI.WeaponType.SYSTEM) return false;
        if(weapon.getType() == WeaponAPI.WeaponType.STATION_MODULE) return false;
        boolean blacklisted = true;

        WeaponWhitelist weaponWhitelist = fleetIDWeaponWhitelist.get(currentFleetMemberID);
        if(weaponWhitelist == null) return false;

        //weapon ranges TODO: maybe separate out missiles instead of multiplier
        float range = weapon.getMaxRange();
        boolean isMissile = weapon.getMountType() == WeaponAPI.WeaponType.MISSILE;
        float missileMultiplier = Global.getSettings().getFloat("missileMultiplier");

        boolean isFirstRangeBracket = (range <= 400 * (isMissile ? missileMultiplier : 1));
        boolean isSecondRangeBracket = (range >= 400 * (isMissile ? missileMultiplier : 1) && range <= 700 * (isMissile ? missileMultiplier : 1));
        boolean isThirdRangeBracket = (range >= 700 * (isMissile ? missileMultiplier : 1) && range <= 1000 * (isMissile ? missileMultiplier : 1));
        boolean isFourthRangeBracket = (range >= 1000 * (isMissile ? missileMultiplier : 1));

        // do positive check here to get inclusive ranges instead of exclusive
        if ((isFirstRangeBracket && weaponWhitelist.firstRangeBracket) ||
            (isSecondRangeBracket && weaponWhitelist.secondRangeBracket) ||
            (isThirdRangeBracket && weaponWhitelist.thirdRangeBracket) ||
            (isFourthRangeBracket && weaponWhitelist.fourthRangeBracket)) {
            blacklisted = false;
        }

        //damage types
        if ((weapon.getDamageType() == DamageType.KINETIC && !weaponWhitelist.kinetic) ||
            (weapon.getDamageType() == DamageType.HIGH_EXPLOSIVE && !weaponWhitelist.highExplosive) ||
            (weapon.getDamageType() == DamageType.ENERGY && !weaponWhitelist.energy) ||
            (weapon.getDamageType() == DamageType.FRAGMENTATION && !weaponWhitelist.fragmentation)){
            blacklisted = true;
        }

        //beam and projectiles
        if ((weapon.isBeam() && !weaponWhitelist.beam) ||
            (!weapon.isBeam() && !weaponWhitelist.projectile)){
            blacklisted = true;
        }

        //point defense
        boolean isPD = weapon.getAIHints().contains(WeaponAPI.AIHints.PD) || weapon.getAIHints().contains(WeaponAPI.AIHints.PD_ALSO);

        if ((isPD && !weaponWhitelist.pointDefense) ||
            (!isPD && !weaponWhitelist.nonPointDefense)) {
            blacklisted = true;
        }

        return blacklisted;
    }
}
