package com.behsazan.corebanking.fee3.r3;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** F6 simulation deliberately has no repository, authentication bypass for writes, or financial effects. */
@RestController
@RequestMapping("/api/v1/fee3/simulations")
public final class Fee3F6SimulationController {
    @PostMapping("/preview")
    public Fee3F6SimulationService.SimulationResult preview(
            @RequestBody Fee3PreviewEngine.PreviewRequest request) {
        return Fee3F6SimulationService.simulate(request);
    }
}
