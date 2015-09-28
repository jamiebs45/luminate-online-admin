/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.me.jvmi;

import com.sun.org.apache.xerces.internal.impl.xs.XSConstraints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;

/**
 *
 * @author jbeckstrom
 */
public class Main {
    
    
    
    public static void main(String[] args){
        
        String username = args[0];
        String password = args[1];
        
        WebDriver driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        
        driver.get("https://secure2.convio.net/jvmi/admin/AdminLogin");
        
        WebElement usernameField = driver.findElement(By.cssSelector("input[id^='USERNAME'"));
        WebElement passwordField = driver.findElement(By.cssSelector("input[id^='Password'"));
        WebElement login = driver.findElement(By.id("login"));
        
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        
        login.click();
        
        EcommerceAdmin ecommerceAdmin = new EcommerceAdmin(driver, "https://secure2.convio.net/jvmi/admin/EcommerceAdmin");
        Product product = ecommerceAdmin.createProduct("test");
        ecommerceAdmin.updateProduct(product);
    }
    
    public static class LuminateOnlineClient{
        
    }
    
    public static class EcommerceAdmin{
        
        private final String uri;
        private final WebDriver driver;
        
        public EcommerceAdmin(WebDriver driver, String uri){
            this.driver = driver;
            this.uri = uri;
        }
        
        public Product createProduct(String name){
            driver.get(uri + "?prod_id=0&ecommerce=prod_create");
            
            WebElement nameField = driver.findElement(By.id("product_namename"));          
            nameField.sendKeys(name);

            driver.findElement(By.id("pstep_save-button")).click();
            
            String id = driver.findElement(By.id("prod_id")).getAttribute("value");
            return new Product(id);
        }
        
        public void updateProduct(Product product){
//            doNameStep(product);
//            doPricingStep(product);
//            doCatalogInfoStep(product);
//            doShortDescriptionStep(product);
//            doFullDescriptionStep(product);
            doStoresStep(product);
        }
        
        private void doNameStep(Product product){
            //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create
            driver.get(uri + "?prod_id="+product.getId()+"&ecommerce=prod_create");
            
            WebElement nameField = driver.findElement(By.id("product_namename"));
            WebElement externalIdField = driver.findElement(By.id("product_external_idname"));
            
            nameField.sendKeys(product.getName());
            externalIdField.sendKeys(product.getExternalId());
            
            if(product.expires()){
                WebElement enableExpirationDateCheckBox = driver.findElement(By.id("enable_expiration_datename"));
                enableExpirationDateCheckBox.click();
                
                SelectElement expirationDateMonth = new SelectElement(
                        driver.findElement(By.id("expiration_date_MONTH")), driver);
                SelectElement expirationDateDay = new SelectElement(
                        driver.findElement(By.id("expiration_date_DAY")), driver);
                SelectElement expirationDateYear = new SelectElement(
                        driver.findElement(By.id("expiration_date_YEAR")), driver);
                
                expirationDateMonth.selectOption(product.getExpirationMonth());
                expirationDateDay.selectOption(product.getExpirationDay());
                expirationDateYear.selectOption(product.getExpirationYear());
            }
            
            driver.findElement(By.id("pstep_save-button")).click();
        }
        
        private void doPricingStep(Product product){
            driver.get(uri + "?prod_id="+product.getId()+"&ecommerce=prod_create_pricing");
            
            WebElement standardPriceField = driver.findElement(By.id("standard_priceinput"));
            WebElement fairMarketValueField = driver.findElement(By.id("fair_market_valueinput"));
            
            standardPriceField.sendKeys(product.getStandardPrice());
            fairMarketValueField.sendKeys(product.getFairMarketValue());
            
            driver.findElement(By.id("pstep_save-button")).click();
        }
        
        private void doCatalogInfoStep(Product product){
            //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create_catalog
            driver.get(uri + "?prod_id="+product.getId()+"&ecommerce=prod_create_catalog");
            
            SelectElement productTypeSelect = new SelectElement(
                    driver.findElement(By.id("product_type_sel")), driver);
            productTypeSelect.selectOption(product.getType());
            
            for(int categoryId: product.getCategories()){
                WebElement categoryCheckBox = driver.findElement(
                        By.xpath("//input[@value='"+categoryId+"']/../input[@type='checkbox']"));
                categoryCheckBox.click();
            }
            
            driver.findElement(By.id("pstep_save-button")).click();
        }
        
        private void doShortDescriptionStep(Product product){
            //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create_short_desc
            driver.get(uri + "?prod_id="+product.getId()+"&ecommerce=prod_create_short_desc");
            
            WebElement shortDescField = driver.findElement(By.id("product_short_descname"));
            WebElement keywordsField = driver.findElement(By.id("product_search_keywordsname"));
            WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));
            
            shortDescField.sendKeys(product.getShortDesc());
            keywordsField.sendKeys(product.getKeywords());
            fileInput.sendKeys(product.getImagePath());
            
            driver.findElement(By.id("pstep_save-button")).click();
        }
        
        private void doFullDescriptionStep(Product product){
            driver.get(uri + "?prod_id="+product.getId()+"&ecommerce=prod_create_long_desc");
        
            WebElement textEditor = driver.findElement(By.linkText("Use Plain Text Editor"));
            textEditor.click();

            WebElement htmlTextArea = driver.findElement(By.id("Gprod_htmltextarea"));
            htmlTextArea.sendKeys(product.getHtmlFullDesc());
            
            driver.findElement(By.id("pstep_save-button")).click();
        }
        
        private void doStoresStep(Product product){
            //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create_stores
            driver.get(uri + "?prod_id="+product.getId()+"&ecommerce=prod_create_stores");
        
            WebElement removeAllButton = driver.findElement(By.id("product_stores_Remove_All"));
            removeAllButton.click();
            
            SelectElement sourceList = new SelectElement(
                    driver.findElement(By.id("product_stores_SourceList")), driver);
            WebElement addSelectedButton = driver.findElement(By.id("product_stores_Add_Selected"));
            sourceList.selectAll(product.getStores());
            addSelectedButton.click();
            
            driver.findElement(By.id("pstep_save-button")).click();
        }
        
    }

    public static class SelectElement{
        private final WebElement select;
        private final WebDriver driver;

        public SelectElement(WebElement select, WebDriver driver) {
            this.select = select;
            this.driver = driver;
        }
        
        public void selectOption(String optionText){
            List<WebElement> options = select.findElements(By.tagName("option"));
            for(WebElement option: options){
                if(option.getText().equalsIgnoreCase(optionText)){
                    option.click();
                    break;
                }
            }
        }
        
        public void selectAll(Set<String> optionsText){
            Actions shiftClick = new Actions(driver);
            shiftClick = shiftClick.keyDown(Keys.SHIFT);
            List<WebElement> options = select.findElements(By.tagName("option"));
            for(WebElement option: options){
                if(optionsText.contains(option.getText())){
                    shiftClick = shiftClick.click(option);
                }
            }
            shiftClick.keyUp(Keys.SHIFT).perform();
        }
    }
    
    public static class ProductDate{
        private String month;
        private String day;
        private String year;

        public void setMonth(String month) {
            this.month = month;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getMonth() {
            return month;
        }

        public String getDay() {
            return day;
        }

        public String getYear() {
            return year;
        }
    }
    
    public static class Product{
        
        private final String id;
        private String name = "test product";
        private String externalId = "1234";
        private boolean expires = false;
        private ProductDate expDate;
        private String standardPrice = "100";
        private String fairMarketValue = "40";
        private String type = "Television Program Offers";
        private int[] categories = {1044, 1038};
        private String shortDesc = "this is a short description";
        private String imagePath = "/Users/jbeckstrom/Desktop/index.jpg";
        private String keywords = "test keyword";
        private String htmlFullDesc = "<h1>header</h1><p>body</p>";
        private Set<String> stores = new HashSet(Arrays.asList("WP", "WPM", "WPMMS", "WPMS"));

        public Product(String id){
            this.id = id;
        }
        
        public String getId(){
            return id;
        }
        
        private String getName() {
            return name;
        }

        private String getExternalId() {
            return externalId;
        }

        private boolean expires() {
            return expires;
        }

        private String getExpirationMonth() {
            return expDate.getMonth();
        }

        private String getExpirationDay() {
            return expDate.getDay();
        }

        private String getExpirationYear() {
            return expDate.getYear();
        }

        private String getStandardPrice() {
            return standardPrice;
        }

        private String getFairMarketValue() {
            return fairMarketValue;
        }

        private String getType() {
            return type;
        }

        private int[] getCategories() {
            return categories;
        }

        private String getShortDesc() {
            return shortDesc;
        }

        private String getImagePath() {
            return imagePath;
        }

        private String getKeywords() {
            return keywords;
        }

        private String getHtmlFullDesc() {
            return htmlFullDesc;
        }

        private Set<String> getStores() {
            return stores;
        }
        
    }
    
}
