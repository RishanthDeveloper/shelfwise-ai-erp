package com.shelfwise.controller;

import com.shelfwise.dto.FraudDto;
import com.shelfwise.service.FraudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;

    @PostMapping("/detect")
    public FraudDto.Response detect(@Valid @RequestBody FraudDto.Request request) {
        return fraudService.detect(request);
    }
}
