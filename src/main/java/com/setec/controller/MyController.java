package com.setec.controller;

import java.util.List;
import java.util.Map;

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
import com.setec.dao.FileStorageService;

@RestController
@RequestMapping("/api/product")
public class MyController {
    
    @Autowired
    private ProductRepo productRepo;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @GetMapping
    public Object getAll() {
        var products = productRepo.findAll();
        if(products.size()==0)
            return ResponseEntity.status(404).body(Map.of("message","product is empty"));
        return products;
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object postProduct(@ModelAttribute PostProductDAO product) throws Exception {
        
        String fileName = fileStorageService.storeFile(product.getFile());
        
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
            fileStorageService.deleteFile(p.get().getImageUrl());
            
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
                fileStorageService.deleteFile(update.getImageUrl());
                String fileName = fileStorageService.storeFile(product.getFile());
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

    // ADD THIS DEBUG ENDPOINT
    @GetMapping("/debug/files")
    public Object debugFiles() {
        try {
            String uploadDir = System.getenv("DATABASE_URL") != null ? "/tmp/static" : "myApp/static";
            java.io.File dir = new java.io.File(uploadDir);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("uploadDir", uploadDir);
            result.put("exists", dir.exists());
            result.put("isDirectory", dir.isDirectory());
            
            if (dir.exists() && dir.isDirectory()) {
                java.io.File[] files = dir.listFiles();
                result.put("fileCount", files != null ? files.length : 0);
                result.put("files", files != null ? 
                    java.util.Arrays.stream(files)
                        .map(file -> Map.of(
                            "name", file.getName(),
                            "size", file.length(),
                            "url", "https://product-web-api.onrender.com/static/" + file.getName()
                        ))
                        .collect(java.util.stream.Collectors.toList()) 
                    : java.util.List.of());
            }
            
            return result;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}