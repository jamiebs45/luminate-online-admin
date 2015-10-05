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
import java.util.Properties;

/**
 *
 * @author jbeckstrom
 */
public class Config {
    
    private final Properties props;
    
    public Config(Path configFile) throws IOException{
        props = new Properties();
        try(InputStream is = Files.newInputStream(configFile)){
            props.load(is);
        }
    }
    
    public String dpcsUser(){
        return props.getProperty("dpcs.user");
    }
    
    public String dpcsPassword(){
        return props.getProperty("dpcs.pass");
    }
    
    public String ftpUser(){
        return props.getProperty("ftp.user");
    }
    
    public String ftpPassword(){
        return props.getProperty("ftp.pass");
    }
    
    public String luminateUser(){
        return props.getProperty("luminate.user");
    }
    
    public String luminatePassword(){
        return props.getProperty("luminate.pass");
    }
}
