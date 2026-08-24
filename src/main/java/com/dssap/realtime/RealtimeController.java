package com.dssap.realtime;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {

    private final RealtimeSapService realtimeSapService;

    public RealtimeController(RealtimeSapService realtimeSapService) {
        this.realtimeSapService = realtimeSapService;
    }

    @GetMapping("/materials/{matnr}")
    public ResponseEntity<RealtimeResponse> material(@PathVariable("matnr") @NotBlank String matnr) {
        return ResponseEntity.ok(realtimeSapService.getMaterial(matnr));
    }

    @GetMapping("/sales-orders/{vbeln}")
    public ResponseEntity<RealtimeResponse> salesOrder(@PathVariable("vbeln") @NotBlank String vbeln) {
        return ResponseEntity.ok(realtimeSapService.getSalesOrder(vbeln));
    }

    @PostMapping("/rfc/{functionName}")
    public ResponseEntity<RealtimeResponse> rfc(@PathVariable String functionName,
                                                @RequestBody(required = false) Map<String, Object> imports) {
        return ResponseEntity.ok(realtimeSapService.callFunction(functionName, imports));
    }
}
