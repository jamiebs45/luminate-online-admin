/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.me.jvmi;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jbabic
 */
public class ProductImages {

    private final String webHost = "jvmi.convio.net";
    private final Path imagesDir;// = Paths.get("/Volumes/jvmpubfs/WEB/images/products/");
    private final LuminateFTPClient ftp;

    private final Map<String, ProductImage> cache = new HashMap<>();

    public ProductImages(Path imagesDir, LuminateFTPClient ftp) {
        this.imagesDir = imagesDir;
        this.ftp = ftp;
    }

    public ProductImage getProductImage(String id) throws IOException {
        ProductImage ret = cache.get(id);
        if (ret == null) {
            ret = create(id);
            cache.put(id, ret);
        }
        return ret;
    }

    private ProductImage create(String id) throws IOException {
        Path image = findImage(id);
        if (image == null) {
            throw new IllegalStateException("Could not find product image for id: "+id);
        }
        String url = getImageUrl(image);
        return new ProductImage(url, image);
    }

    private String getImageUrl(Path image) throws IOException {
        String path = ftp.uploadProductImage(image);
        return "http://" + webHost + path;
    }

    private Path findImage(String id) {
        Path dir = imagesDir.resolve(id.charAt(0) + "");
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                String filename = path.getFileName().toString();
                if (filename.startsWith(id + "_")) {
                    return path;
                }
            }
        } catch (IOException ex) {
        }
        return null;
    }

    public static class ProductImage {

        private final String url;
        private final Path path;

        public ProductImage() {
            this(null, null);
        }

        public ProductImage(String url, Path path) {
            this.url = url;
            this.path = path;
        }

        public String getUrl() {
            return url;
        }

        public Path getPath() {
            return path;
        }

    }
}
