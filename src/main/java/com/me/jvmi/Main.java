/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.me.jvmi;

import com.me.jvmi.ProductImages.ProductImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.jvmi.automator.dpcs.DPCSClient;
import org.jvmi.automator.dpcs.DPCSItem;
import org.jvmi.automator.luminate.ECommerceProduct;
import org.jvmi.automator.luminate.LuminateOnlineClient;

/**
 *
 * @author jbeckstrom
 */
public class Main {
    
    /*
    publish date
    expiration date
    priority
    keywords
    short description
    excludes list
    check for images only
    intelligent error handling
    */

    public static void main(String[] args) throws IOException {

        final Path localProductImagesPath = Paths.get("/Users/jbeckstrom/Desktop/products/");//Paths.get("/Volumes/jvmpubfs/WEB/images/products/");
        final Path uploadCSVFile = Paths.get("/Users/jbeckstrom/Desktop/product-upload.csv");
        final Config config = new Config(Paths.get("config.properties"));

        Map<String, DPCSClient> dpcsClients = new HashMap<>();
        dpcsClients.put("us", new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvm"));
        dpcsClients.put("ca", new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvcn"));
        dpcsClients.put("uk", new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvuk"));
        
        for(DPCSClient client: dpcsClients.values()){
            client.login(config.dpcsUser(), config.dpcsPassword());
        }
        
        LuminateFTPClient ftp = new LuminateFTPClient("customerftp.convio.net");
        ftp.login(config.ftpUser(), config.ftpPassword());

        ProductImages images = new ProductImages(localProductImagesPath, ftp);

        Map<String, LuminateOnlineClient> luminateClients = new HashMap<>();
        luminateClients.put("us", new LuminateOnlineClient("https://secure2.convio.net/jvmi/admin/"));
        luminateClients.put("ca", new LuminateOnlineClient("https://secure3.convio.net/jvmica/admin/"));
        luminateClients.put("uk", new LuminateOnlineClient("https://secure3.convio.net/jvmiuk/admin/"));

        Map<String, EcommerceProductFactory> ecommFactories = new HashMap<>();
        ecommFactories.put("us", new EcommerceProductFactory(dpcsClients.get("us"), images, Categories.us));
        ecommFactories.put("ca", new EcommerceProductFactory(dpcsClients.get("ca"), images, Categories.ca));
        ecommFactories.put("uk", new EcommerceProductFactory(dpcsClients.get("uk"), images, Categories.uk));

        List<String> countries = Arrays.asList("us", "ca", "uk");

        Collection<InputRecord> records = parseInput(uploadCSVFile);

        for (InputRecord record : records) {
            for (String country : countries) {
                EcommerceProductFactory ecommFactory = ecommFactories.get(country);
                LuminateOnlineClient luminateClient = luminateClients.get(country);
                luminateClient.login(config.luminateUser(), config.luminatePassword());

                ECommerceProduct product = ecommFactory.createECommerceProduct(record);
                luminateClient.createOrUpdateProduct(product);
            }
        }
    }

    public static Collection<InputRecord> parseInput(Path csv) throws IOException {
        Map<String, InputRecord> records = new HashMap<>();
        CSVParser parser = CSVParser.parse(csv.toFile(), Charset.forName("UTF-8"), CSVFormat.EXCEL.withHeader());

        for (CSVRecord record : parser) {
            InputRecord input = new InputRecord(record);
            records.put(input.getId(), input);
        }

        for (InputRecord record : records.values()) {
            if (record.isPackage()) {
                for (String id : record.packageIds) {
                    if (!records.containsKey(id)) {
                        throw new IllegalStateException("Could not find product for package id: " + id);
                    }
                    record.addItem(records.get(id));
                }
            }
        }

        return records.values();
    }

    public static class EcommerceProductFactory {

        private final ProductImages images;
        private final DPCSClient dpcs;
        private final Categories categories;

        public EcommerceProductFactory(DPCSClient dpcs, ProductImages images, Categories categories) {
            this.dpcs = dpcs;
            this.images = images;
            this.categories = categories;
        }

        public ECommerceProduct createECommerceProduct(InputRecord record) throws IOException {
            final DPCSItem dpcsItem = dpcs.search(record.getId());
            final ProductImage image = images.getProductImage(record.getId());

            ECommerceProduct ret = new ECommerceProduct();
            ret.setName(createProductName(record));
            ret.setExternalId(record.getId());
            ret.setStandardPrice(getStandardPrice(dpcsItem));
            ret.setFairMarketValue(getFairMarketPrice(dpcsItem));
            ret.setShortDesc(getDescription(record));
            ret.setHtmlFullDesc(createFullDescriptionHtml(record, image.getUrl()));
            ret.setType(record.getType());
            ret.setImagePath(image.getPath());
            ret.setStores(record.getStores());
            ret.setCategories(getCategories(record));
            ret.setKeywords(getKeywords(record));

            return ret;
        }

        private Set<Integer> getCategories(InputRecord record) {
            Set<Integer> ret = new HashSet<>();
            if (record.isPackage()) {
                for (InputRecord child : record.getItems()) {
                    ret.addAll(getCategories(child));
                }
            }
            for (String category : record.getCategories()) {
                String[] split = category.split("\\s*>\\s*");
                String id = categories.lookup(split);
                if (id != null) {
                    ret.add(Integer.parseInt(id));
                }
            }
            return ret;
        }
        
        private String getKeywords(InputRecord record){
            Set<String> keywords = new HashSet<>();
            keywords.addAll(record.getKeywords());
            if(record.isPackage()){
                for(InputRecord child: record.getItems()){
                    keywords.addAll(child.getKeywords());
                }
            }
            return StringUtils.join(keywords, ",");
        }

        private String getDescription(InputRecord record) {
            String description = record.getDescription();
            if (StringUtils.isBlank(description) && record.isPackage()) {
                description = record.getFirstItem().getDescription();
            }
            return description;
        }

        private String createProductName(InputRecord record) {
            String name = record.getName();
            if (StringUtils.isBlank(name) && record.isPackage()) {
                for (InputRecord child : record.getItems()) {
                    if (StringUtils.isBlank(name)) {
                        name = child.getName();
                    } else {
                        name += " + " + child.getName();
                    }
                }
            }
            return String.format("%s - %s", record.getId(), name);
        }

        private BigDecimal getStandardPrice(DPCSItem dpcsItem) {
            BigDecimal retail = dpcsItem.getRetailPrice();
            BigDecimal offer = dpcsItem.getOfferPrice();
            return retail.compareTo(BigDecimal.ZERO) == 0 ? offer : retail;
        }

        private BigDecimal getFairMarketPrice(DPCSItem dpcsItem) {
            return dpcsItem.getFairMarketValue();
        }

        private String createFullDescriptionHtml(InputRecord record, String imageUrl) {
            StringBuilder html = new StringBuilder();
            if (imageUrl != null) {
                html.append("<p>").append("<img style='max-width: 100%; height: auto; display: block;' src=").append(imageUrl).append(">").append("</p>");
            }
            if (record.isPackage()) {
                for (InputRecord item : record.getItems()) {
                    html.append(createShortDescriptionHtml(item, imageUrl));
                }
            } else {
                html.append(createShortDescriptionHtml(record, imageUrl));
            }
            return html.toString();
        }

        private String createShortDescriptionHtml(InputRecord record, String imageUrl) {
            StringBuilder html = new StringBuilder();
            html.append("<h2>").append(record.getName()).append("</h2>");
            html.append("<p>").append(record.getDescription()).append("</p>");
            return html.toString();
        }

    }

    public static class InputRecord {

        private final String name;
        private final String description;
        private final String id;
        private final String type;
        private final Set<String> keywords = new HashSet<>();
        private final Set<String> packageIds = new LinkedHashSet<>();
        private final Set<InputRecord> items = new LinkedHashSet<>();
        private final Set<String> categories = new HashSet<>();
        private final Set<String> stores = new HashSet<>();

        public InputRecord(CSVRecord record) {
            name = record.get(0);
            description = record.get(1);
            id = record.get(2);
            packageIds.addAll(split(record.get(3), ";"));
            type = record.get(4);
            categories.addAll(split(record.get(5), ";"));
            keywords.addAll(split(record.get(6).toLowerCase(), ","));
            stores.addAll(split(record.get(7), ";"));
        }
        
        private Collection<String> split(String txt, String delimeter){
            if(!txt.contains(delimeter)){
                return Collections.EMPTY_LIST;
            }
            return Arrays.asList(txt.split("\\s*"+delimeter+"\\s*"));
        }

//        public InputRecord(String name, String description, String id, String type,
//                Collection<String> categories, Collection<String> stores, Collection<InputRecord> items) {
//            this.name = name;
//            this.description = description;
//            this.id = id;
//            this.type = type;
//            this.categories.addAll(categories);
//            this.items.addAll(items);
//            this.stores.addAll(stores);
//        }
        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public boolean isPackage() {
            return !packageIds.isEmpty();
        }

        public Set<String> getCategories() {
            return categories;
        }

        public InputRecord getFirstItem() {
            if (!items.isEmpty()) {
                return items.iterator().next();
            }
            return null;
        }

        public Set<InputRecord> getItems() {
            return items;
        }

        public void addItem(InputRecord item) {
            items.add(item);
        }

        public Set<String> getStores() {
            return stores;
        }

        public Set<String> getPackageIds() {
            return packageIds;
        }

        public Set<String> getKeywords() {
            return keywords;
        }
        
        @Override
        public String toString() {
            return "InputRecord{" + "name=" + name + ", description=" + description + ", id=" + id + ", type=" + type + ", keywords=" + keywords + ", packageIds=" + packageIds + ", items=" + items + ", categories=" + categories + ", stores=" + stores + '}';
        }

    }

    public static class InputData {

        private final Map<String, InputRecord> records = new HashMap<>();

        public InputData() {

        }

        public Collection<InputRecord> getRecords() {
            return records.values();
        }

        public InputRecord getRecord(String id) {
            return records.get(id);
        }

    }

}
