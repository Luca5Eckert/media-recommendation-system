package com.mrs.catalog_service.controller;

import com.mrs.catalog_service.dto.CreateMediaRequest;
import com.mrs.catalog_service.service.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    public ResponseEntity<String> create(@RequestBody @Validated CreateMediaRequest createMediaRequest){
        mediaService.create(createMediaRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Media created with success");
    }

}
