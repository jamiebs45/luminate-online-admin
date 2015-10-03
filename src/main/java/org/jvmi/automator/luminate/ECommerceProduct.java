/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvmi.automator.luminate;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jbabic
 */
public class ECommerceProduct {
    public static final String DEFAULT_ID = "0";
    
    private String id = DEFAULT_ID;
    private String name;
    private String externalId;
    private LuminateDate expDate = null;
    private BigDecimal standardPrice;
    private BigDecimal fairMarketValue;
    private String type;
    private String shortDesc;
    private Path imagePath;
    private String keywords;
    private String htmlFullDesc;
    private Set<Integer> categories = new HashSet<>();
    private Set<String> stores = new HashSet<>();

    public ECommerceProduct() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getExternalId() {
        return externalId;
    }

    public boolean expires() {
        return expDate != null;
    }

    public String getExpirationMonth() {
        return expDate.getMonth();
    }

    public String getExpirationDay() {
        return expDate.getDay();
    }

    public String getExpirationYear() {
        return expDate.getYear();
    }

    public BigDecimal getStandardPrice() {
        return standardPrice;
    }

    public BigDecimal getFairMarketValue() {
        return fairMarketValue;
    }

    public String getType() {
        return type;
    }

    public Collection<Integer> getCategories() {
        return categories;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public Path getImagePath() {
        return imagePath;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getHtmlFullDesc() {
        return htmlFullDesc;
    }

    public Set<String> getStores() {
        return stores;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setExpDate(LuminateDate expDate) {
        this.expDate = expDate;
    }

    public void setStandardPrice(BigDecimal standardPrice) {
        this.standardPrice = standardPrice;
    }

    public void setFairMarketValue(BigDecimal fairMarketValue) {
        this.fairMarketValue = fairMarketValue;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public void setImagePath(Path imagePath) {
        this.imagePath = imagePath;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public void setHtmlFullDesc(String htmlFullDesc) {
        this.htmlFullDesc = htmlFullDesc;
    }

    public void setCategories(Set<Integer> categories) {
        this.categories = categories;
    }

    public void setStores(Set<String> stores) {
        this.stores = stores;
    }

}
