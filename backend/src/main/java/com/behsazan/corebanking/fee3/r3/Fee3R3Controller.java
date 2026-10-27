package com.behsazan.corebanking.fee3.r3;

import com.behsazan.corebanking.fee3.domain.Fee3Models.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

/** API uses container-authenticated identity and container roles, never client-supplied actor headers. */
@RestController
@RequestMapping("/api/v1/fee3")
public class Fee3R3Controller {
    private final Fee3R3Service service;
    public Fee3R3Controller(Fee3R3Service service){this.service=service;}
    private static Fee3R3Service.Actor actor(HttpServletRequest request,Principal principal){
        String name=principal==null?null:principal.getName();
        boolean authenticated=name!=null&&!name.isBlank()&&
            request.getAuthType()!=null&&!request.getAuthType().isBlank();
        return new Fee3R3Service.Actor(name,authenticated,
            authenticated&&request.isUserInRole("FEE3_MAKER"),
            authenticated&&request.isUserInRole("FEE3_CHECKER"));
    }
    @PostMapping("/validation")
    public Fee3SubmissionPolicy.Report validation(@RequestBody DraftRequest request){
        return service.preflight(request);
    }
    @PostMapping("/preview")
    public Fee3PreviewEngine.PreviewResult preview(@RequestBody Fee3PreviewEngine.PreviewRequest request){
        return service.preview(request);
    }
    @GetMapping("/workflow/readiness")
    public Fee3R3Service.WorkflowReadiness readiness(HttpServletRequest request,Principal principal){
        return service.workflowReadiness(actor(request,principal));
    }
    @GetMapping("/workflow/requests")
    public List<Fee3R3WorkflowRepository.RequestInfo> requests(@RequestParam long itemId,
            HttpServletRequest request,Principal principal){
        return service.requests(itemId,actor(request,principal));
    }
    @PostMapping("/workflow/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Fee3R3WorkflowRepository.RequestInfo submit(@RequestBody Fee3R3Service.SubmitRequest body,
            HttpServletRequest request,Principal principal){
        return service.submit(body,actor(request,principal));
    }
    @PostMapping("/workflow/requests/{requestId}/decision")
    public Fee3R3WorkflowRepository.RequestInfo decide(@PathVariable long requestId,
            @RequestBody Fee3R3Service.DecisionRequest body,HttpServletRequest request,Principal principal){
        return service.decide(requestId,body,actor(request,principal));
    }
}
