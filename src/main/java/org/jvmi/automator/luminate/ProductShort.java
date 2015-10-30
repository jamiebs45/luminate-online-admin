/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jvmi.automator.luminate;

/**
 *
 * @author jbabic
 */
public class ProductShort {
    private final String id;
    private final boolean active;
    
    public ProductShort(String id, boolean active){
        this.id = id;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }
}
