package com.behsazan.corebanking.referencedata.general.romanization.web;

import com.behsazan.corebanking.referencedata.general.romanization.application.NameRomanizationToolService;
import com.behsazan.corebanking.referencedata.general.romanization.application.NameRomanizationToolService.NameRomanizationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/name-romanization")
public class NameRomanizationToolController {
    private final NameRomanizationToolService service;

    public NameRomanizationToolController(NameRomanizationToolService service) {
        this.service = service;
    }

    @PostMapping("/resolve")
    NameRomanizationResult resolve(@Valid @RequestBody ResolveNameRequest request) {
        return service.resolve(request.persianName());
    }

    public record ResolveNameRequest(
            @NotBlank(message = "نام فارسی را وارد کنید.")
            @Size(max = 400, message = "طول نام فارسی نمی‌تواند بیش از ۴۰۰ نویسه باشد.")
            String persianName
    ) {}
}
