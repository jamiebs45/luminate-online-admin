/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.me.jvmi;

import com.me.jvmi.ProductImages.ProductImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvmi.automator.dpcs.DPCSClient;
import org.jvmi.automator.dpcs.DPCSItem;
import org.jvmi.automator.luminate.ECommerceProduct;
import org.jvmi.automator.luminate.LuminateOnlineClient;
import org.jvmi.automator.luminate.ProductShort;

/**
 *
 * @author jbeckstrom
 */
public class Main {

    /*
     get prices from dpcs in first pass
     publish date
     expiration date
     priority
     keywords
     short description
     excludes list
     check for images only
     intelligent error handling
     retry 3 times
     */
    public static void main(String[] args) throws IOException {

        final Path localProductImagesPath = Paths.get("/Volumes/jvmpubfs/WEB/images/products/");
        final Path uploadCSVFile = Paths.get("/Users/jbabic/Documents/products/upload.csv");
        final Config config = new Config(Paths.get("../config.properties"));

        LuminateOnlineClient luminateClient2 = new LuminateOnlineClient("https://secure2.convio.net/jvmi/admin/", 3);
        luminateClient2.login(config.luminateUser(), config.luminatePassword());
        
   
        Set<String> completed = new HashSet<>( IOUtils.readLines(Files.newBufferedReader(Paths.get("completed.txt"))) );
        
        
        try(InputStream is = Files.newInputStream(Paths.get("donforms.csv"));
                PrintWriter pw = new PrintWriter(new FileOutputStream(new File("completed.txt"), true))){
            
            for(String line : IOUtils.readLines(is)){
                if(completed.contains(line)){
                    System.out.println("completed: "+line);
                    continue;
                }
                try{
                    luminateClient2.editDonationForm(line, "-1");
                    pw.println(line);
                    System.out.println("done: "+line);
                    pw.flush();
                }catch(Exception e){
                    System.out.println("skipping: "+line);
                    e.printStackTrace();
                }
            }
        }
        
        
        
//        luminateClient2.editDonationForm("8840", "-1");
//        Collection<String> ids = luminateClient2.donFormSearch("", true);
//        
//        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
//        try (FileWriter fileWriter = new FileWriter(new File("donforms.csv"));
//                CSVPrinter csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);) {
//
//            for (String id : ids) {
//                csvFilePrinter.printRecord(id);
//            }
//        }
//        
        
        if(true){
            return;
        }

        
        Collection<InputRecord> records = parseInput(uploadCSVFile);
        
        LuminateFTPClient ftp = new LuminateFTPClient("customerftp.convio.net");
        ftp.login(config.ftpUser(), config.ftpPassword());

        ProductImages images = new ProductImages(localProductImagesPath, ftp);
        
        validateImages(records, images);
        
        Map<String, DPCSClient> dpcsClients = new HashMap<>();
        dpcsClients.put("us", new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvm"));
        dpcsClients.put("ca", new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvcn"));
        dpcsClients.put("uk", new DPCSClient("https://donor.dpconsulting.com/NewDDI/Logon.asp?client=jvuk"));

        for (DPCSClient client : dpcsClients.values()) {
            client.login(config.dpcsUser(), config.dpcsPassword());
        }

        Map<String, LuminateOnlineClient> luminateClients = new HashMap<>();
        luminateClients.put("us", new LuminateOnlineClient("https://secure2.convio.net/jvmi/admin/", 10));
        luminateClients.put("ca", new LuminateOnlineClient("https://secure3.convio.net/jvmica/admin/", 10));
        luminateClients.put("uk", new LuminateOnlineClient("https://secure3.convio.net/jvmiuk/admin/", 10));

        Map<String, EcommerceProductFactory> ecommFactories = new HashMap<>();
        ecommFactories.put("us", new EcommerceProductFactory(dpcsClients.get("us"), images, Categories.us));
        ecommFactories.put("ca", new EcommerceProductFactory(dpcsClients.get("ca"), images, Categories.ca));
        ecommFactories.put("uk", new EcommerceProductFactory(dpcsClients.get("uk"), images, Categories.uk));

        List<String> countries = Arrays.asList("us", "ca", "uk");

        

        boolean error = false;
        for (InputRecord record : records) {
            for (String country : countries) {
                if (record.ignore(country)) {
                    System.out.println("IGNORE: " + country + " " + record);
                    continue;
                }
                try {
                    EcommerceProductFactory ecommFactory = ecommFactories.get(country);
                    LuminateOnlineClient luminateClient = luminateClients.get(country);
                    luminateClient.login(config.luminateUser(), config.luminatePassword());

                    ECommerceProduct product = ecommFactory.createECommerceProduct(record);
                    luminateClient.createOrUpdateProduct(product);
                } catch (Exception e) {
                    System.out.println("ERROR: " + country + " " + record);
                    //System.out.println(e.getMessage());
                    error = true;
                    e.printStackTrace();
                }
            }
        }

        if (!error) {
            for (String country : countries) {
                LuminateOnlineClient luminateClient = luminateClients.get(country);
                DPCSClient dpcsClient = dpcsClients.get(country);
                luminateClient.close();
                dpcsClient.close();
            }
        }
    }
    
    public static void validateImages(Collection<InputRecord> records, ProductImages images){
        List<Exception> exceptions = new ArrayList<>();
        for(InputRecord record: records){
            try {
                images.getProductImage(record.getId());
            } catch (Exception ex) {
                exceptions.add(ex);
            }
        }
        if(!exceptions.isEmpty()){
            for(Exception e: exceptions){
                e.printStackTrace();
            }
            throw new IllegalStateException("Could not find product images!");
        }
    }

    public static Collection<InputRecord> parseInput(Path csv) throws IOException {
        Map<String, InputRecord> records = new HashMap<>();
        CSVParser parser = CSVParser.parse(csv.toFile(), Charset.forName("UTF-8"), CSVFormat.DEFAULT.withHeader());

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
            final ProductImage image = images.getProductImage(record.getId());

            ECommerceProduct ret = new ECommerceProduct();
            ret.setName(createProductName(record));
            ret.setExternalId(record.getId());
            ret.setStandardPrice(getStandardPrice(record));
            ret.setFairMarketValue(getFairMarketPrice(record));
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

        private String getKeywords(InputRecord record) {
            Set<String> keywords = new HashSet<>();
            keywords.addAll(record.getKeywords());
            if (record.isPackage()) {
                for (InputRecord child : record.getItems()) {
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

        private BigDecimal getStandardPrice(InputRecord record) {
            if (record.getPrice().equals(BigDecimal.ZERO)) {
                final DPCSItem dpcsItem = dpcs.search(record.getId());
                BigDecimal retail = dpcsItem.getRetailPrice();
                BigDecimal offer = dpcsItem.getOfferPrice();
                return offer.compareTo(BigDecimal.ZERO) == 0 ? retail : offer;
            } else {
                return record.getPrice();
            }
        }

        private BigDecimal getFairMarketPrice(InputRecord record) {
            if (record.getFmv().equals(BigDecimal.ZERO)) {
                final DPCSItem dpcsItem = dpcs.search(record.getId());
                return dpcsItem.getFairMarketValue();
            } else {
                return record.getFmv();
            }
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
            if (StringUtils.isBlank(record.getFullDescription())) {
                html.append("<h2 style='margin-top:15px;margin-bottom:15px;'>").append(record.getName()).append("</h2>");
                html.append("<p>").append(record.getDescription()).append("</p>");
            } else {
                html.append(record.getFullDescription());
            }
            return html.toString();
        }

    }

    public static class InputRecord {

        private final String name;
        private final String description;
        private final String fullDescription;
        private final String id;
        private final String type;
        private final BigDecimal fmv;
        private final BigDecimal price;
        private final Set<String> keywords = new HashSet<>();
        private final Set<String> packageIds = new LinkedHashSet<>();
        private final Set<InputRecord> items = new LinkedHashSet<>();
        private final Set<String> categories = new HashSet<>();
        private final Set<String> stores = new HashSet<>();
        private final Set<String> ignoreCountries = new HashSet<>();

        public InputRecord(CSVRecord record) {
            int index = 0;
            id = record.get(index++);
            name = record.get(index++);
            description = record.get(index++);
            fullDescription = record.get(index++);
            packageIds.addAll(split(record.get(index++), ";"));
            fmv = parseAmount(record.get(index++));
            price = parseAmount(record.get(index++));
            type = record.get(index++);
            categories.addAll(split(record.get(index++), ";"));
            keywords.addAll(split(record.get(index++).toLowerCase(), ","));
            stores.addAll(split(record.get(index++), ";"));
            ignoreCountries.addAll(split(record.get(index++), ";"));
        }

        private boolean parseBoolean(String txt) {
            if (StringUtils.isBlank(txt)) {
                return false;
            }
            return Boolean.parseBoolean(txt);
        }

        private BigDecimal parseAmount(String txt) {
            if (StringUtils.isBlank(txt)) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(txt);
        }

        private Collection<String> split(String txt, String delimeter) {
            if (StringUtils.isBlank(txt)) {
                return Collections.EMPTY_LIST;
            }
            return Arrays.asList(txt.split("\\s*" + delimeter + "\\s*"));
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

        public String getFullDescription() {
            return fullDescription;
        }

        public BigDecimal getFmv() {
            return fmv;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public boolean ignore(String country) {
            return ignoreCountries.contains(country);
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
