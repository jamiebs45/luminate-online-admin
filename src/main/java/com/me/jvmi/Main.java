/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.me.jvmi;

import com.me.jvmi.ProductImages.ProductImage;
import java.io.IOException;
import org.jvmi.automator.luminate.ECommerceProduct;
import org.jvmi.automator.dpcs.DPCSClient;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jvmi.automator.dpcs.DPCSItem;
import org.jvmi.automator.luminate.LuminateOnlineClient;

/**
 *
 * @author jbeckstrom
 */
public class Main {

    public static void main(String[] args) throws IOException {

        String username = args[0];
        String password = args[1];

//        EcommerceAdmin ecommerceAdmin = new EcommerceAdmin(driver, "https://secure2.convio.net/jvmi/admin/EcommerceAdmin");
//        Product product = ecommerceAdmin.createProduct("test");
//        ecommerceAdmin.updateProduct(product);
//        
//        System.out.println(client.productSearch("1823479128374"));
//        System.out.println(client.doesProductExist("1823479128374"));
        DPCSClient dpcs = new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvm");
        
        LuminateFTPClient ftp = new LuminateFTPClient("customerftp.convio.net");
        
        ProductImages images = new ProductImages(Paths.get("/Volumes/jvmpubfs/WEB/images/products/"), ftp);

        Map<String, LuminateOnlineClient> luminateClients = new HashMap<>();
        luminateClients.put("us", new LuminateOnlineClient("https://secure2.convio.net/jvmi/admin/"));
        luminateClients.put("ca", new LuminateOnlineClient("https://secure3.convio.net/jvmica/admin/"));
        luminateClients.put("uk", new LuminateOnlineClient("https://secure3.convio.net/jvmiuk/admin/"));

        Map<String, EcommerceProductFactory> ecommFactories = new HashMap<>();
        ecommFactories.put("us", new EcommerceProductFactory(dpcs, images, Categories.us));
        ecommFactories.put("ca", new EcommerceProductFactory(dpcs, images, Categories.ca));
        ecommFactories.put("uk", new EcommerceProductFactory(dpcs, images, Categories.uk));
        
        List<String> countries = Arrays.asList("uk");//Arrays.asList("us", "ca", "uk");

        
        List<InputRecord> records = new ArrayList<>();
        records.add(new InputRecord(
                "ISIS Exposed, book by Erick Stakelbeck",
                "Who is ISIS? Where did this brutal terrorist group come from and what is driving its successful campaign of murder and conquest? In ISIS Exposed, veteran investigative reporter Erick Stakelbeck gets inside the story of the new caliphate and reveals just how clear and present a threat it is. ",
                "9164",
                "Television Program Offers",
                Arrays.asList("Books > Books", "Jewish Voice Resources > Books", "Offers > Television", "Study Resources > Books"),
                Arrays.asList("Copy of WPMS", "WP", "WPM", "WPMMS", "WPMS", "WPTV", "WP - CAN", "WPM - CAN", "WPMMS-CA", "WPMS-CA", "WPTV - CAN", "WP - UK", "WPM - UK", "WPMMS-UK", "WPMS-UK", "WPTV-UK"),
                Collections.EMPTY_LIST));

        for (InputRecord record : records) {
            for(String country: countries){
                EcommerceProductFactory ecommFactory = ecommFactories.get(country);
                LuminateOnlineClient luminateClient = luminateClients.get(country);
                luminateClient.login(username, password);
                
                ECommerceProduct product = ecommFactory.createECommerceProduct(record);
                luminateClient.createOrUpdateProduct(product);
            }
        }
    }
    
    public static Collection<InputRecord> parseInput(Path csv) throws IOException{
        Map<String, InputRecord> records = new HashMap<>();
        CSVParser parser = CSVParser.parse(csv.toFile(), Charset.forName("UTF-8"), CSVFormat.EXCEL);
        for(CSVRecord record: parser){
            InputRecord input = new InputRecord(record);
            records.put(input.getId(), input);
        }
        
        
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
            ret.setShortDesc(record.getDescription());
            ret.setHtmlFullDesc(createFullDescriptionHtml(record, image.getUrl()));
            ret.setType(record.getType());
            ret.setImagePath(image.getPath());
            ret.setStores(record.getStores());
            ret.setCategories(getCategories(record));

            return ret;
        }

        

        private Set<Integer> getCategories(InputRecord record) {
            Set<Integer> ret = new HashSet<>();
            for (String category : record.getCategories()) {
                String[] split = category.split("\\s*>\\s*");
                String id = categories.lookup(split);
                if (id != null) {
                    ret.add(Integer.parseInt(id));
                }
            }
            return ret;
        }

        private String createProductName(InputRecord record) {
            return String.format("%s - %s", record.getId(), record.getName());
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
            if (imageUrl != null) {
                html.append("<p>").append("<img style='max-width: 100%; height: auto; display: block;' src=").append(imageUrl).append(">").append("</p>");
            }
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
        private final String keywords;
        private final Set<String> packageIds = new HashSet<>();
        private final Set<InputRecord> items = new HashSet<>();
        private final Set<String> categories = new HashSet<>();
        private final Set<String> stores = new HashSet<>();

        public InputRecord(CSVRecord record){
            name = record.get("name");
            description = record.get("description");
            id = record.get("id");
            String itemIds = record.get("package ids");
            if(!itemIds.isEmpty()){
                packageIds.addAll(Arrays.asList(itemIds.split(";")));
            }
            type = record.get("type");
            keywords = record.get("keywords");
            categories.addAll(Arrays.asList(record.get("categories").split(";")));
            stores.addAll(Arrays.asList(record.get("stores").split(";")));
        }
        
        public InputRecord(String name, String description, String id, String type,
                Collection<String> categories, Collection<String> stores, Collection<InputRecord> items) {
            this.name = name;
            this.description = description;
            this.id = id;
            this.type = type;
            this.categories.addAll(categories);
            this.items.addAll(items);
            this.stores.addAll(stores);
        }

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
            return !items.isEmpty();
        }

        public Set<String> getCategories() {
            return categories;
        }

        public Set<InputRecord> getItems() {
            return items;
        }
        
        public void addItem(InputRecord item){
            items.add(item);
        }

        public Set<String> getStores() {
            return stores;
        }

        public Set<String> getPackageIds() {
            return packageIds;
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
