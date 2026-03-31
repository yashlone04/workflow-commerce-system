package com.example.workflowcommerce.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.example.workflowcommerce.payload.response.MessageResponse;
import com.example.workflowcommerce.repository.CategoryRepository;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    com.example.workflowcommerce.repository.ProductRepository productRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findByStatusTrue();
        Map<Long, Long> counts = productRepository.countActiveByCategory().stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        categories.forEach(category -> {
            category.setProductCount(counts.getOrDefault(category.getCategory_id(), 0L));
        });
        return categories;
    }

    @GetMapping("/public")
    public List<Category> getPublicCategories() {
        return categoryRepository.findByStatusTrue();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody Category category) {
        if (categoryRepository.existsByCategory_name(category.getCategory_name())) {
            return ResponseEntity.badRequest().body(new MessageResponse("TAXO_001: Category identity already established in system infrastructure."));
        }
        category.setStatus(true);
        categoryRepository.save(category);
        return ResponseEntity.ok(new MessageResponse("Category provisioned successfully. Operational status: ACTIVE."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @Valid @RequestBody Category categoryRequest) {
        return categoryRepository.findById(id).map(category -> {
            if (!category.getCategory_name().equals(categoryRequest.getCategory_name())
                && categoryRepository.existsByCategory_name(categoryRequest.getCategory_name())) {
                return ResponseEntity.badRequest().body(new MessageResponse("TAXO_001: Category identity already established in system infrastructure."));
            }
            category.setCategory_name(categoryRequest.getCategory_name());
            category.setDescription(categoryRequest.getDescription());
            categoryRepository.save(category);
            return ResponseEntity.ok(new MessageResponse("Category configuration updated. Delta persisted to primary ledger."));
        }).orElse(ResponseEntity.status(404).body(new MessageResponse("ERROR_404: Resource target not found in infrastructure.")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deactivateCategory(@PathVariable Long id) {
        return categoryRepository.findById(id).map(category -> {
            category.setStatus(false); // Determinstic Soft-Delete logic
            categoryRepository.save(category);
            return ResponseEntity.ok(new MessageResponse("Category decommissioned. Status: INACTIVE."));
        }).orElse(ResponseEntity.status(404).body(new MessageResponse("ERROR_404: Resource target not found in infrastructure.")));
    }
}
