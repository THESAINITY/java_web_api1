package com.setec.controller;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.setec.dao.PostProductDAO;
import com.setec.dao.PutProductDAO;
import com.setec.entities.Product;
import com.setec.repos.ProductRepo;

@RestController
@RequestMapping("/api/product")
public class MyController {
    
    @Autowired
    private ProductRepo productRepo;
    
    // Simple method to get upload directory
    private String getUploadDir() {
        // Check if we're on Render (production)
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && databaseUrl.contains("postgres")) {
            // On Render - use /tmp directory
            return "/tmp/static";
        } else {
            // Local development on Mac - use myApp/static
            return "myApp/static";
        }
    }
    
    @GetMapping
    public Object getAll() {
        var products = productRepo.findAll();
        if(products.size()==0)
            return ResponseEntity.status(404).body(Map.of("message","product is empty"));
        return products;
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object postProduct(@ModelAttribute PostProductDAO product) throws Exception {
        
        String uploadDir = getUploadDir();
        File dir = new File(uploadDir);
        if(!dir.exists())
            dir.mkdirs();
            
        var file = product.getFile();
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = UUID.randomUUID() + extension;
        String filePath = Paths.get(uploadDir, fileName).toString();
        file.transferTo(new File(filePath));
        
        Product pro = new Product();
        pro.setName(product.getName());
        pro.setPrice(product.getPrice());
        pro.setQty(product.getQty());
        pro.setImageUrl("/static/" + fileName);
        
        productRepo.save(pro);
        
        return ResponseEntity.status(201).body(pro);
    }

    @GetMapping({"{id}","id/{id}"})
    public Object getById(@PathVariable("id") Integer id) {
        var pro = productRepo.findById(id);
        if(pro.isPresent()) {
            return pro.get();
        }
        return ResponseEntity.status(404)
                .body(Map.of("message","Product id = "+id+ " not found"));
    }
    
    @GetMapping("name/{name}")
    public Object getByName(@PathVariable("name") String name) {
        List<Product> pro = productRepo.findByName(name);
        if(pro.size()>0) {
            return pro;
        }
        return ResponseEntity.status(404)
                .body(Map.of("message","Product name = "+name+ " not found"));
    }
    
    @DeleteMapping({"{id}","id/{id}"})
    public Object deleteById(@PathVariable("id")Integer id) {
        var p = productRepo.findById(id);
        if(p.isPresent()) {
            String uploadDir = getUploadDir();
            String imagePath = p.get().getImageUrl().replace("/static/", "/");
            String filePath = uploadDir + imagePath;
            new File(filePath).delete();
            
            productRepo.delete(p.get());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Product id = "+id+" has been deleted"));
        }
        return ResponseEntity.status(404).body(Map.of("message", "Product id = "+id+" not found"));
    }
    
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object putProduct(@ModelAttribute PutProductDAO product) throws Exception {
        Integer id = product.getId();
        var p = productRepo.findById(id);
        if(p.isPresent()) {
            var update = p.get();
            update.setName(product.getName());
            update.setPrice(product.getPrice());
            update.setQty(product.getQty());
            
            if(product.getFile() != null) {
                String uploadDir = getUploadDir();
                File dir = new File(uploadDir);
                if(!dir.exists())
                    dir.mkdirs();
                
                var file = product.getFile();
                String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
                String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                String fileName = UUID.randomUUID() + extension;
                String filePath = Paths.get(uploadDir, fileName).toString();
                
                // Delete old file
                String oldImagePath = update.getImageUrl().replace("/static/", "/");
                String oldFilePath = uploadDir + oldImagePath;
                new File(oldFilePath).delete();
                
                // Save new file
                file.transferTo(new File(filePath));
                update.setImageUrl("/static/" + fileName);
            }
            
            productRepo.save(update);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message","Product id = "+id+" update successful ",
                            "product",update));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message","Product id = "+id+" not found"));
    }
}