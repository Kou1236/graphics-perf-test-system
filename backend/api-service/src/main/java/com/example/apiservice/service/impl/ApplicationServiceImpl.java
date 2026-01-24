package com.example.apiservice.service.impl;

import com.example.apiservice.domain.entity.ApplicationEntity;
import com.example.apiservice.domain.repository.ApplicationRepository;
import com.example.apiservice.service.ApplicationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public ApplicationEntity create(ApplicationEntity app) {
        return applicationRepository.save(app);
    }

    @Override
    public List<ApplicationEntity> listAll() {
        return applicationRepository.findAll();
    }

    @Override
    public ApplicationEntity getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
    }
}