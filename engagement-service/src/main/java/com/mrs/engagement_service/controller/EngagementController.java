package com.mrs.engagement_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/engagement")
public class EngagementController {


    @PostMapping
    public ResponseEntity<String> createEngagement(@RequestBody String engagement){
        return null;
    }

}
