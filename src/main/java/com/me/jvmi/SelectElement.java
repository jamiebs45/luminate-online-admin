/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.me.jvmi;

import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 *
 * @author jbabic
 */
public class SelectElement {
    private final WebElement select;
    private final WebDriver driver;

    public SelectElement(WebElement select, WebDriver driver) {
        this.select = select;
        this.driver = driver;
    }

    public void selectOption(String optionText) {
        List<WebElement> options = select.findElements(By.tagName("option"));
        for (WebElement option : options) {
            if (option.getText().equalsIgnoreCase(optionText)) {
                option.click();
                break;
            }
        }
    }

    public void selectAll(Set<String> optionsText) {
        Actions shiftClick = new Actions(driver);
        shiftClick = shiftClick.keyDown(Keys.SHIFT);
        List<WebElement> options = select.findElements(By.tagName("option"));
        for (WebElement option : options) {
            if (optionsText.contains(option.getText())) {
                shiftClick = shiftClick.click(option);
            }
        }
        shiftClick.keyUp(Keys.SHIFT).perform();
    }
    
}
