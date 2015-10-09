/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvmi.automator.luminate;

import com.me.jvmi.Main;
import com.me.jvmi.SelectElement;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

    public LuminateOnlineClient(String uri) {
        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
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

    public Collection<String> productSearch(String queryText) {
        Set<String> ret = new HashSet<>();

        driver.get(uri + "EcommerceAdmin?ecommerce=prod_list");
        WebElement searchField = driver.findElement(By.id("filter_text"));
        WebElement searchButton = driver.findElement(By.id("filter_search"));
        searchField.clear();
        searchField.sendKeys(queryText);
        searchButton.click();

        try {
            Collection<WebElement> elements = driver.findElements(By.cssSelector("table[id^='lct'] tr .ObjectId"));
            for (WebElement element : elements) {
                String text = element.getText().trim();
                String[] split = text.split(":");
                ret.add(split[1].trim());
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
        Collection<String> ids = productSearch(product.getExternalId());
        if (ids.size() > 1) {
            throw new IllegalStateException("More than one product with an external id '" + product.getExternalId() + "' exists!");
        }

        if (ids.isEmpty()) {
            createProduct(product);
        } else {
            product.setId(ids.iterator().next());
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

        WebElement nameField = driver.findElement(By.id("product_namename"));
        WebElement externalIdField = driver.findElement(By.id("product_external_idname"));

        clearAndSendKeys(nameField, product.getName());
        clearAndSendKeys(externalIdField, product.getExternalId());

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
        WebElement fairMarketValueField = driver.findElement(By.id("fair_market_valueinput"));

        clearAndSendKeys(standardPriceField, product.getStandardPrice().toPlainString());
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

}
