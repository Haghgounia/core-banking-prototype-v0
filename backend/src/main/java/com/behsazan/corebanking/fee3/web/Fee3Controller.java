package com.behsazan.corebanking.fee3.web;

import com.behsazan.corebanking.fee3.application.Fee3Service;
import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fee3")
public class Fee3Controller {
    private final Fee3Service service;
    public Fee3Controller(Fee3Service service){this.service=service;}
    @GetMapping("/readiness")
    public Readiness readiness(){return service.readiness();}
    @GetMapping("/items")
    public Page<CatalogRow> catalog(@RequestParam(required=false) String search,
        @RequestParam(required=false) String module,
        @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="25") int size){return service.catalog(search,module,page,size);}
    @GetMapping("/items/{code}")
    public ItemAggregate item(@PathVariable String code){return service.item(code);}
    @GetMapping("/references/operations")
    public List<Operation> operations(){return service.operations();}
    @GetMapping("/references/formulas")
    public List<Formula> formulas(){return service.formulas();}
    @GetMapping("/references/sources")
    public List<Source> sources(){return service.sources();}
    @GetMapping("/references/codes/{group}")
    public List<RefCode> codes(@PathVariable String group){return service.codes(group);}
    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public DraftReceipt create(@RequestBody DraftRequest request,
        @RequestHeader(name="X-FEE3-UAT-Token",required=false) String token){return service.createDraft(request,token);}
    @PutMapping("/drafts/{itemId}/{versionId}")
    public DraftReceipt update(@PathVariable long itemId,@PathVariable long versionId,
                               @RequestBody DraftRequest request,
                               @RequestHeader(name="X-FEE3-UAT-Token",required=false) String token){return service.updateDraft(itemId,versionId,request,token);}
}
