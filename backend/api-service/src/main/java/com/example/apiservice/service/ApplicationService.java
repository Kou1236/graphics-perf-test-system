package com.example.apiservice.service;

import com.example.apiservice.domain.entity.ApplicationEntity;

import java.util.List;

public interface ApplicationService {

    ApplicationEntity create(ApplicationEntity app);

    List<ApplicationEntity> listAll();

    ApplicationEntity getById(Long id);
}