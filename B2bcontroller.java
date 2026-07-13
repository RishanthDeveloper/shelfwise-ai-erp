package com.shelfwise.controller;

import com.shelfwise.dto.B2BDto;
import com.shelfwise.service.B2BService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/b2b")
@RequiredArgsConstructor
public class B2BController {

    private final B2BService b2bService;

    @PostMapping("/recommend")
    public B2BDto.Response recommend(@Valid @RequestBody B2BDto.Request request) {
        return b2bService.recommend(request);
    }
}
