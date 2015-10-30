/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvmi.automator.luminate;

import com.me.jvmi.Main;
import com.me.jvmi.SelectElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author jbabic
 */
public class LuminateOnlineClient {

    private final WebDriver driver;
    private final String uri;

    private boolean loggedIn = false;

    public LuminateOnlineClient(String uri, int seconds) {
        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
        this.uri = uri;
    }

    public void login(String username, String password) {
        if (loggedIn) {
            return;
        }
        driver.get(uri + "AdminLogin");
        WebElement usernameField = driver.findElement(By.cssSelector("input[id^='USERNAME']"));
        WebElement passwordField = driver.findElement(By.cssSelector("input[id^='Password']"));
        WebElement login = driver.findElement(By.id("login"));
        usernameField.sendKeys(username);
        passwordField.sendKeys(password);
        login.click();
        loggedIn = true;
    }
    
    public void close(){
        driver.quit();
    }
    
    public void editDonationForm(String donFormId, String campaignId){
        driver.get(uri + "Donation2Admin?autores_obj_id=&df_id="+donFormId+"&don.admin=form_autoresp_ed.cf&autores_obj_id_param_name=df_id&autores_type_id=&autores_app_id=9&action=action_configure_autoresponders&dc_id="+campaignId);
        //https://secure2.convio.net/jvmi/admin/Donation2Admin?df_id=8840&don.admin=form_ed_id.cf&action=edit&dc_id=2221
        
        //auto respondeer screen
        //https://secure2.convio.net/jvmi/admin/Donation2Admin?autores_obj_id=&df_id=8840&don.admin=form_autoresp_ed.cf&autores_obj_id_param_name=df_id&autores_type_id=&autores_app_id=9&action=action_configure_autoresponders&dc_id=2221
    
        //look for edit links
        List<String> editUrls = new ArrayList<>();
        for(WebElement link: driver.findElements(By.linkText("Edit"))){
            editUrls.add(link.getAttribute("href"));
        }
        
        for(String editUrl: editUrls){
            driver.get(editUrl);
            WebElement emailField = driver.findElement(By.id("emailname"));
            WebElement submit = driver.findElement(By.id("pstep_next-button"));
            clearAndSendKeys(emailField, "partnerservices@jewishvoice.org");
            submit.click();
        }
        
    }
    
    public Collection<String> donFormSearch(String queryText, boolean pageinate) {
        List<String> ret = new ArrayList<>();

        driver.get(uri + "Donation2Admin?don.admin=all_form_list_pa");
        WebElement searchField = driver.findElement(By.id("filter_text"));
        WebElement searchButton = driver.findElement(By.id("filter_search"));
        searchField.clear();
        searchField.sendKeys(queryText);
        searchButton.click();

        ret.addAll(parseDonationForms());
        while(pageinate && !isLastPage()){
            driver.findElement(By.linkText("Next")).click();
            ret.addAll(parseDonationForms());
        }

        return ret;
    }

    public Collection<ProductShort> productSearch(String queryText, boolean pageinate) {
        List<ProductShort> ret = new ArrayList<>();

        driver.get(uri + "EcommerceAdmin?ecommerce=prod_list");
        WebElement searchField = driver.findElement(By.id("filter_text"));
        WebElement searchButton = driver.findElement(By.id("filter_search"));
        searchField.clear();
        searchField.sendKeys(queryText);
        searchButton.click();

        ret.addAll(parseProducts());
        while(pageinate && !isLastPage()){
            driver.findElement(By.linkText("Next")).click();
            ret.addAll(parseProducts());
        }

        return ret;
    }
    
    private boolean isLastPage(){
        //Records 1 - 20 of 2,000
        try{
            String text = driver.findElement(By.xpath("//p[starts-with(text(),'Records')]")).getText();
        
            Pattern p = Pattern.compile("Records ([0-9,]+) - ([0-9,]+) of ([0-9,]+)");
            Matcher m = p.matcher(text);
            if(m.find()){
                String of = m.group(2);
                String total = m.group(3);
                return of.equals(total);
            }else{
                throw new IllegalStateException("Could not find 'Records' on page!");
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
            return false;
        }
    }
    
    private List<String> parseDonationForms(){
        List<String> ret = new ArrayList<>();
        try {
            
            for(WebElement link: driver.findElements(By.linkText("Edit"))){
            ret.add(link.getAttribute("href"));
        }
            
//            Collection<WebElement> elements = driver.findElements(By.cssSelector("tr[class^='lc_Row']"));
//            for (WebElement element : elements) {
//                List<WebElement> cells = element.findElements(By.cssSelector("td[id^='lc_cell']"));
//                if(cells.size()==7){
//                    String id = cells.get(0).findElement(By.className("ObjectId")).getText().trim();
//                    id = id.replaceAll("Form ID:\\s+", "");
//                    ret.add(id.trim());
//                }
//            }
        } catch (Exception e) {
            //ignore
        }
        return ret;
    }
    
    private List<ProductShort> parseProducts(){
        List<ProductShort> ret = new ArrayList<>();
        try {
            Collection<WebElement> elements = driver.findElements(By.cssSelector("tr[class^='lc_Row']"));
            for (WebElement element : elements) {
                List<WebElement> cells = element.findElements(By.cssSelector("td[id^='lc_cell']"));
                if(cells.size()==5){
                    String id = cells.get(0).findElement(By.className("ObjectId")).getText().trim();
                    id = id.replaceAll("ID:\\s+", "");
                    String active = cells.get(2).getText().trim();
                    ret.add(new ProductShort(id, active.equalsIgnoreCase("Active")));
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return ret;
    }

    public boolean doesProductExist(String id) {
        //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?ecommerce=prod_list
        driver.get(uri + "EcommerceAdmin?ecommerce=prod_list");
        WebElement searchField = driver.findElement(By.id("filter_text"));
        WebElement searchButton = driver.findElement(By.id("filter_search"));
        searchField.clear();
        searchField.sendKeys(id);
        searchButton.click();

        try {
            driver.findElement(By.xpath("//p[text()='There are currently no products defined.']"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void createOrUpdateProduct(ECommerceProduct product) {
        validateProduct(product);
        Collection<ProductShort> ids = productSearch(product.getExternalId(), false);
        if (ids.size() > 1) {
            throw new IllegalStateException("More than one product with an external id '" + product.getExternalId() + "' exists!");
        }

        if (ids.isEmpty()) {
            createProduct(product);
        } else {
            product.setId(ids.iterator().next().getId());
            doNameStep(product);
        }
        doPricingStep(product);
        doCatalogInfoStep(product);
        doShortDescriptionStep(product);
        doFullDescriptionStep(product);
        doStoresStep(product);
        doPublish(product);
    }

    private void validateProduct(ECommerceProduct product) {
        if (product.getExternalId() == null) {
            throw new IllegalStateException("Product External ID must not be null!");
        }
//        if (product.getStores().isEmpty()) {
//            throw new IllegalStateException("At least one store must be defined for a product!");
//        }
        if (product.getStandardPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Standard Price must be greater than zero! (Standard Price = " + product.getStandardPrice() + ")");
        }
        if (product.getFairMarketValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Fair Market Value must be greater than zero! (Fair Market Value = " + product.getFairMarketValue() + ")");
        }
        if (product.getFairMarketValue().compareTo(product.getStandardPrice()) > 0) {
            throw new IllegalStateException("Fair Market Value must be less than Standard Price! "
                    + "(Standard Price = " + product.getStandardPrice() + " AND (Fair Market Value = " + product.getFairMarketValue() + ")");
        }
    }

    private void createProduct(ECommerceProduct product) {
        doNameStep(product);
        String id = driver.findElement(By.id("prod_id")).getAttribute("value");
        product.setId(id);
    }

    private void doNameStep(ECommerceProduct product) {
        //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_create");

        WebElement externalIdField = driver.findElement(By.id("product_external_idname"));
        clearAndSendKeys(externalIdField, product.getExternalId());
        
        WebElement nameField = driver.findElement(By.id("product_namename"));
        clearAndSendKeys(nameField, product.getName());
        
        

        if (product.expires()) {
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

    private void doPricingStep(ECommerceProduct product) {
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_create_pricing");

        WebElement standardPriceField = driver.findElement(By.id("standard_priceinput"));
        clearAndSendKeys(standardPriceField, product.getStandardPrice().toPlainString());
        
        WebElement fairMarketValueField = driver.findElement(By.id("fair_market_valueinput"));
        clearAndSendKeys(fairMarketValueField, product.getFairMarketValue().toPlainString());

        driver.findElement(By.id("pstep_save-button")).click();
    }

    private void doCatalogInfoStep(ECommerceProduct product) {
        //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create_catalog
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_create_catalog");

        if (product.getType() != null) {
            SelectElement productTypeSelect = new SelectElement(
                    driver.findElement(By.id("product_type_sel")), driver);
            productTypeSelect.selectOption(product.getType());
        }

        if (!product.getCategories().isEmpty()) {
            List<WebElement> selectedProductCategories = driver.findElements(By.cssSelector("tr[id^='product_category'] input[checked='checked']"));
            for (WebElement productCategoryCheckBox : selectedProductCategories) {
                productCategoryCheckBox.click();
            }

            for (int categoryId : product.getCategories()) {
                WebElement categoryCheckBox = driver.findElement(
                        By.xpath("//input[@value='" + categoryId + "']/../input[@type='checkbox']"));
                categoryCheckBox.click();
            }
        }

        driver.findElement(By.id("pstep_save-button")).click();
    }

    private void doShortDescriptionStep(ECommerceProduct product) {
        //https://secure2.convio.net/jvmi/admin/EcommerceAdmin?prod_id=0&ecommerce=prod_create_short_desc
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_create_short_desc");

        WebElement shortDescField = driver.findElement(By.id("product_short_descname"));
        WebElement keywordsField = driver.findElement(By.id("product_search_keywordsname"));
        WebElement fileInput = driver.findElement(By.cssSelector("input[type='file']"));

        clearAndSendKeys(shortDescField, product.getShortDesc());
        clearAndSendKeys(keywordsField, product.getKeywords());
        clearAndSendKeys(fileInput, product.getImagePath().toAbsolutePath().toString());

        driver.findElement(By.id("pstep_save-button")).click();
    }

    private void doFullDescriptionStep(ECommerceProduct product) {
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_create_long_desc");

        try {
            WebElement htmlTextArea = driver.findElement(By.id("Gprod_htmltextarea"));
            clearAndSendKeys(htmlTextArea, product.getHtmlFullDesc());
        } catch (Exception e) {
            WebElement textEditor = driver.findElement(By.linkText("Use Plain Text Editor"));
            textEditor.click();
            
            WebElement htmlTextArea = driver.findElement(By.id("Gprod_htmltextarea"));
            clearAndSendKeys(htmlTextArea, product.getHtmlFullDesc());
        }

        driver.findElement(By.id("pstep_save-button")).click();
    }

    private void doStoresStep(ECommerceProduct product) {
        if (product.getStores().isEmpty()) {
            return;
        }
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_create_stores");

        WebElement removeAllButton = driver.findElement(By.id("product_stores_Remove_All"));
        removeAllButton.click();

        SelectElement sourceList = new SelectElement(
                driver.findElement(By.id("product_stores_SourceList")), driver);
        WebElement addSelectedButton = driver.findElement(By.id("product_stores_Add_Selected"));
        sourceList.selectAll(product.getStores());
        addSelectedButton.click();

        driver.findElement(By.id("pstep_save-button")).click();
    }

    private void doPublish(ECommerceProduct product) {
        driver.get(uri + "EcommerceAdmin?prod_id=" + product.getId() + "&ecommerce=prod_edit_publish");

        WebElement publishButton = driver.findElement(By.id("product_publish_buttonbtn"));
        publishButton.click();
    }

    private void clearAndSendKeys(WebElement element, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        element.clear();
        element.sendKeys(text);
    }

    public static void main(String[] args){
        String text = "Records 1 - 20 of 2,000";
        Pattern p = Pattern.compile("Records ([0-9,]+) - ([0-9,]+) of ([0-9,]+)");
        Matcher m = p.matcher(text);
        if(m.find()){
            String of = m.group(2);
            String total = m.group(3);
            System.out.println(of+" "+total);
        }
    }
}
