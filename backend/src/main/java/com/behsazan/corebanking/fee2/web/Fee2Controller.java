package com.behsazan.corebanking.fee2.web;

import com.behsazan.corebanking.fee2.application.Fee2Service;
import com.behsazan.corebanking.fee2.domain.Fee2Models.CalculationConfigRequest;
import com.behsazan.corebanking.fee2.domain.Fee2Models.CatalogResponse;
import com.behsazan.corebanking.fee2.domain.Fee2Models.SelectOption;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TableDescriptor;
import com.behsazan.corebanking.fee2.domain.Fee2Models.TablePage;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioCatalogItem;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioCreateRequest;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioCreateResponse;
import com.behsazan.corebanking.fee2.domain.Fee2Models.StudioSummary;
import com.behsazan.corebanking.fee2.domain.Fee2Models.VersionTransitionRequest;
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
@RequestMapping("/api/v1/fee2")
public class Fee2Controller {
    private final Fee2Service service;
    public Fee2Controller(Fee2Service service) { this.service = service; }

    @GetMapping("/catalog")
    CatalogResponse catalog() { return service.catalog(); }

    @GetMapping("/studio/summary")
    StudioSummary studioSummary(@RequestParam String scopeId) { return service.studioSummary(scopeId); }

    @GetMapping("/studio/catalog")
    List<StudioCatalogItem> studioCatalog(@RequestParam String scopeId) { return service.studioCatalog(scopeId); }

    @PostMapping("/studio/fees")
    StudioCreateResponse createStudioFee(@RequestBody StudioCreateRequest request,
                                         @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.createStudioFee(request, actor);
    }

    @GetMapping("/tables/{table}/descriptor")
    TableDescriptor descriptor(@PathVariable String table) { return service.descriptor(table); }

    @GetMapping("/tables/{table}/rows")
    TablePage rows(@PathVariable String table,
                   @RequestParam(required=false) String text,
                   @RequestParam(defaultValue="0") int page,
                   @RequestParam(defaultValue="25") int size,
                   @RequestParam(required=false) String filterColumn,
                   @RequestParam(required=false) String filterValue) {
        return service.search(table,text,page,size,filterColumn,filterValue);
    }

    @GetMapping("/tables/{table}/rows/{id}")
    Map<String,Object> row(@PathVariable String table, @PathVariable String id) { return service.findById(table,id); }

    @GetMapping("/tables/{table}/lookup")
    List<SelectOption> lookup(@PathVariable String table,
                              @RequestParam(required=false) String text,
                              @RequestParam(defaultValue="500") int limit) {
        return service.lookup(table,text,limit);
    }

    @PostMapping("/tables/{table}/rows")
    Map<String,Object> create(@PathVariable String table, @RequestBody Map<String,Object> values,
                              @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.create(table,values,actor);
    }

    @PutMapping("/tables/{table}/rows/{id}")
    Map<String,Object> update(@PathVariable String table, @PathVariable String id, @RequestBody Map<String,Object> values,
                              @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.update(table,id,values,actor);
    }

    @DeleteMapping("/tables/{table}/rows/{id}")
    ResponseEntity<Void> delete(@PathVariable String table, @PathVariable String id) {
        service.delete(table,id); return ResponseEntity.noContent().build();
    }

    @PutMapping("/versions/{id}/calculation")
    Map<String,Object> updateCalculation(@PathVariable String id, @RequestBody CalculationConfigRequest request,
                                         @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.updateCalculation(id, request, actor);
    }

    @PostMapping("/versions/{id}/transition")
    Map<String,Object> transitionVersion(@PathVariable String id, @RequestBody VersionTransitionRequest request,
                                         @RequestHeader(name="X-User-Name",defaultValue="prototype-ui") String actor) {
        return service.transitionVersion(id,request.targetStatus(),request.comment(),actor);
    }
}
