package com.example.apiservice.controller;

import com.example.apiservice.domain.entity.SceneEntity;
import com.example.apiservice.service.SceneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scenes")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @PostMapping
    public SceneEntity create(@RequestBody SceneEntity scene) {
        return sceneService.create(scene);
    }

    @GetMapping
    public List<SceneEntity> listAll() {
        return sceneService.listAll();
    }

    @GetMapping("/{id}")
    public SceneEntity getById(@PathVariable Long id) {
        return sceneService.getById(id);
    }

    @PostMapping("/{id}/render-probe-config")
    public SceneEntity updateRenderProbeConfig(@PathVariable Long id, @RequestBody String renderProbeConfigJson) {
        SceneEntity scene = sceneService.getById(id);
        scene.setRenderProbeConfigJson(renderProbeConfigJson);
        return sceneService.create(scene);
    }
}