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
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 *
 * @author jbabic
 */
public class LuminateFTPClient {

    private final String productsPath = "/images/products";
    
    private final String serverAddress;// = "customerftp.convio.net";
    
    private final FTPClient ftp = new FTPClient();

    public LuminateFTPClient(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public boolean login(String userId, String password) throws IOException {
        ftp.connect(serverAddress);
        //login to server
        if (!ftp.login(userId, password)) {
            ftp.logout();
            return false;
        }
        int reply = ftp.getReplyCode();
        //FTPReply stores a set of constants for FTP reply codes. 
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            return false;
        }

        //enter passive mode
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        //get system name
        System.out.println("Remote system is " + ftp.getSystemType());
        //change current directory
        ftp.changeWorkingDirectory(productsPath);
        System.out.println("Current directory is " + ftp.printWorkingDirectory());
        
        return true;
    }
    
    public String uploadProductImage(Path productImage) throws IOException{
        String filename = productImage.getFileName().toString();
        String id = filename.split("_")[0];
        return uploadProductImage(id, productImage);
    }
    
    public String uploadProductImage(String id, Path productImage) throws IOException{
        char first = id.charAt(0);
        
        String path = productsPath+"/"+first;
        String remoteFile = id+".jpg";
        
        ftp.makeDirectory(path);
        ftp.changeWorkingDirectory(path);
        try(InputStream is = Files.newInputStream(productImage)){
            ftp.storeFile(remoteFile, is);
        }
        return path+"/"+remoteFile;
    }
    
    public void shutdown() throws IOException{
        ftp.logout();
        ftp.disconnect();
    }

}
