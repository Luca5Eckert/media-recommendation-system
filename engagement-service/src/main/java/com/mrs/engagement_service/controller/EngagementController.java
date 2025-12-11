package com.mrs.engagement_service.controller;

import com.mrs.engagement_service.dto.InteractionCreateRequest;
import com.mrs.engagement_service.service.EngagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/engagement")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid InteractionCreateRequest engagement){
        engagementService.create(engagement);

        return ResponseEntity.status(HttpStatus.CREATED).body("Engagement registered with success");
    }

}
