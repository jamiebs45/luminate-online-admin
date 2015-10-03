/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.me.jvmi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jbabic
 */
public enum Categories {
    us("us"),
    uk("uk"),
    ca("ca");
    
    private static final String DELIMETER = ".";
    private final Properties props = new Properties();
    
    private Categories(String code){
        Path path = Paths.get("resources", "categories-"+code+".properties");
        try(InputStream is = Files.newInputStream(path)){
            props.load(is);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load properties file: "+path, ex);
        }
    }
    
    public String lookup(String... categories){
        String key = StringUtils.join(categories, DELIMETER);
        key = key.toLowerCase().replaceAll("\\s+", "_");
        return props.getProperty(key);
    }
}
