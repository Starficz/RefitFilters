package org.starficz.refitfilters

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.econ.MarketAPI

class CampaignMarketListener : BaseCampaignEventListener(false)  {

    override fun reportPlayerOpenedMarket(market: MarketAPI) {
        for (submarket in market.submarketsCopy){
            ModPlugin.currentEntityCargos.add(submarket.cargo)

        }
    }

    override fun reportPlayerClosedMarket(market: MarketAPI) {
        ModPlugin.currentEntityCargos.clear()
    }
}