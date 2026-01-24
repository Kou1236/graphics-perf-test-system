package com.example.apiservice.controller;

import com.example.apiservice.domain.entity.ApplicationEntity;
import com.example.apiservice.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apps")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ApplicationEntity create(@RequestBody ApplicationEntity app) {
        return applicationService.create(app);
    }

    @GetMapping
    public List<ApplicationEntity> listAll() {
        return applicationService.listAll();
    }

    @GetMapping("/{id}")
    public ApplicationEntity getById(@PathVariable Long id) {
        return applicationService.getById(id);
    }
}