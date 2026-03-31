package com.example.workflowcommerce.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.workflowcommerce.model.Category;
import com.example.workflowcommerce.model.Product;
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.repository.CategoryRepository;
import com.example.workflowcommerce.service.ProductService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/public")
    public List<Product> getPublicProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody Product product) {
        if (productService.existsBySku(product.getSku())) {
            return ResponseEntity.badRequest().body(new MessageResponse("SKU_001: SKU already exists."));
        }
        if (product.getPrice() == null || product.getPrice().doubleValue() < 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("PRICE_001: Price must be >= 0."));
        }
        if (product.getInventoryCount() == null || product.getInventoryCount() < 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("INV_001: Inventory count must be >= 0."));
        }
        Category category = product.getCategory();
        if (category == null || category.getCategory_id() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("CAT_001: Category not found."));
        }
        Optional<Category> categoryOpt = categoryRepository.findById(category.getCategory_id());
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("CAT_001: Category not found."));
        }
        product.setCategory(categoryOpt.get());
        product.setStatus(true);
        productService.saveProduct(product);
        return ResponseEntity.ok(new MessageResponse("Product created successfully."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody Product productRequest) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(404).body(new MessageResponse("ERROR_404: Product not found."));
        }
        Product product = productOpt.get();
        if (productRequest.getPrice() != null && productRequest.getPrice().doubleValue() < 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("PRICE_001: Price must be >= 0."));
        }
        if (productRequest.getInventoryCount() != null && productRequest.getInventoryCount() < 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("INV_001: Inventory count must be >= 0."));
        }
        if (productRequest.getDescription() != null) {
            product.setDescription(productRequest.getDescription());
        }
        if (productRequest.getPrice() != null) {
            product.setPrice(productRequest.getPrice());
        }
        if (productRequest.getInventoryCount() != null) {
            product.setInventoryCount(productRequest.getInventoryCount());
        }
        Category reqCategory = productRequest.getCategory();
        if (reqCategory != null && reqCategory.getCategory_id() != null) {
            Optional<Category> categoryOpt = categoryRepository.findById(reqCategory.getCategory_id());
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new MessageResponse("CAT_001: Category not found."));
            }
            product.setCategory(categoryOpt.get());
        }
        productService.saveProduct(product);
        return ResponseEntity.ok(new MessageResponse("Product updated successfully."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deactivateProduct(@PathVariable Long id) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(404).body(new MessageResponse("ERROR_404: Product not found."));
        }
        Product product = productOpt.get();
        product.setStatus(false); // Soft delete
        productService.saveProduct(product);
        return ResponseEntity.ok(new MessageResponse("Product deactivated. Status: INACTIVE. Deactivating this product will hide it from the customer interface."));
    }
}
