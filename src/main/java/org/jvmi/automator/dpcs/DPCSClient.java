/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvmi.automator.dpcs;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author jbabic
 */
public class DPCSClient {

    private final WebDriver driver;
    private final String uri;
    
    private final Map<String, DPCSItem> cache = new HashMap<>();

    public DPCSClient(String uri) {
        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        this.uri = uri;
    }

    public void login(String username, String password) {
        driver.get(uri);

        WebElement usernameField = driver.findElement(By.cssSelector("input[name='UserName']"));
        WebElement passwordField = driver.findElement(By.cssSelector("input[name='Password']"));
        WebElement login = driver.findElement(By.cssSelector("input[type='submit']"));

        usernameField.sendKeys(username);
        passwordField.sendKeys(password);

        login.click();
    }
    
    public void close(){
        driver.quit();
    }
    
    public DPCSItem search(String id){
        DPCSItem ret = cache.get(id);
        if(ret==null){
            ret = itemLookup(id);
            cache.put(id, ret);
        }
        return ret;
    }
    
    private DPCSItem fetch(String id){
        driver.get("https://donor.dpconsulting.com/NewDDI/MainPage.asp?Head=InvSrch.asp%3FPage%3DHead&Main=InvSrch.asp%3FPage%3DMain");
        driver.switchTo().frame("Main");
        
        WebElement itemCodeField = driver.findElement(By.cssSelector("input[name='Code']"));
        itemCodeField.sendKeys(id);
        itemCodeField.submit();

        String fmv = driver.findElement(By.cssSelector("input[name='Price']")).getAttribute("value");
        String retail = driver.findElement(By.cssSelector("input[name='Retail']")).getAttribute("value");
        String offer = driver.findElement(By.cssSelector("input[name='FMV']")).getAttribute("value");
        
        final DPCSItem item = new DPCSItem();
        item.setFairMarketValue(parseDouble(fmv));
        item.setRetailPrice(parseDouble(retail));
        item.setOfferPrice(parseDouble(offer));
        return item;
    }
    
    private DPCSItem itemLookup(String code){
        //https://donor.dpconsulting.com/NewDDI/MainPage.asp?Head=InvLook.asp%3FPage%3DHead&Main=InvLook.asp%3FPage%3DMain
        driver.get("https://donor.dpconsulting.com/NewDDI/MainPage.asp?Head=InvLook.asp%3FPage%3DHead&Main=InvLook.asp%3FPage%3DMain");
        driver.switchTo().frame("Main");
        
        WebElement itemCodeField = driver.findElement(By.cssSelector("input[name='Code']"));
        WebElement generateReport = driver.findElement(By.cssSelector("input[value='Generate Report']"));
        itemCodeField.sendKeys(code);
        generateReport.click();

        String fmv = driver.findElement(By.xpath("//td[text()='Fair Market Value:']/../td[last()]")).getText();
        String retail = driver.findElement(By.xpath("//td[text()='Retail Price:']/../td[last()]")).getText();
        String offer = driver.findElement(By.xpath("//td[text()='Offer Amount:']/../td[last()]")).getText();
        
        final DPCSItem item = new DPCSItem();
        item.setFairMarketValue(parseDouble(fmv));
        item.setRetailPrice(parseDouble(retail));
        item.setOfferPrice(parseDouble(offer));
        return item;
    }
    
    private BigDecimal parseDouble(String value){
        return new BigDecimal(value.replaceAll("[^\\d.]+", ""));
    }
}
