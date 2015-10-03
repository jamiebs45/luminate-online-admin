/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvmi.automator.dpcs;

import java.math.BigDecimal;
import org.openqa.selenium.By;

/**
 *
 * @author jbabic
 */
public class DPCSItem {
    private BigDecimal fairMarketValue;
    private BigDecimal retailPrice;
    private BigDecimal offerPrice;
    
    public DPCSItem(){
        
    }

    public BigDecimal getFairMarketValue() {
        return fairMarketValue;
    }

    public void setFairMarketValue(BigDecimal fairMarketValue) {
        this.fairMarketValue = fairMarketValue;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }

    public BigDecimal getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(BigDecimal offerPrice) {
        this.offerPrice = offerPrice;
    }

    @Override
    public String toString() {
        return "DPCSItem{" + "fairMarketValue=" + fairMarketValue + ", retailPrice=" + retailPrice + ", offerPrice=" + offerPrice + '}';
    }
    
}
