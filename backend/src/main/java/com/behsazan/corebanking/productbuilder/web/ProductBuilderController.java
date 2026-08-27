package com.behsazan.corebanking.productbuilder.web;

import com.behsazan.corebanking.productbuilder.application.ProductBuilderService;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.CatalogResponse;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.ProductWorkspace;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.SelectOption;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TableDescriptor;
import com.behsazan.corebanking.productbuilder.domain.ProductBuilderModels.TablePage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/product-builder")
public class ProductBuilderController {
    private final ProductBuilderService service;

    public ProductBuilderController(ProductBuilderService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    CatalogResponse catalog() {
        return service.catalog();
    }

    @GetMapping("/products/{productId}/workspace")
    ProductWorkspace productWorkspace(@PathVariable long productId) {
        return service.productWorkspace(productId);
    }

    @GetMapping("/tables/{table}/descriptor")
    TableDescriptor descriptor(@PathVariable String table) {
        return service.descriptor(table);
    }

    @GetMapping("/tables/{table}/rows")
    TablePage rows(@PathVariable String table,
                   @RequestParam(required = false) String text,
                   @RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "25") int size,
                   @RequestParam(required = false) String filterColumn,
                   @RequestParam(required = false) String filterValue) {
        return service.search(table, text, page, size, filterColumn, filterValue);
    }

    @GetMapping("/tables/{table}/rows/{id}")
    Map<String, Object> row(@PathVariable String table, @PathVariable long id) {
        return service.findById(table, id);
    }

    @GetMapping("/tables/{table}/lookups/{column}")
    List<SelectOption> lookup(@PathVariable String table,
                              @PathVariable String column,
                              @RequestParam(required = false) String text,
                              @RequestParam(defaultValue = "200") int limit) {
        return service.lookup(table, column, text, limit);
    }

    @PostMapping("/tables/{table}/rows")
    Map<String, Object> create(@PathVariable String table,
                               @RequestBody Map<String, Object> values,
                               @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.create(table, values, actor);
    }

    @PutMapping("/tables/{table}/rows/{id}")
    Map<String, Object> update(@PathVariable String table,
                               @PathVariable long id,
                               @RequestBody Map<String, Object> values,
                               @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        return service.update(table, id, values, actor);
    }

    @DeleteMapping("/tables/{table}/rows/{id}")
    ResponseEntity<Void> delete(@PathVariable String table,
                                @PathVariable long id,
                                @RequestHeader(name = "X-User-Name", defaultValue = "prototype-ui") String actor) {
        service.delete(table, id, actor);
        return ResponseEntity.noContent().build();
    }
}
