package com.example.apiservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "runs")
public class RunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sceneId;

    @Column(nullable = false, length = 32)
    private String status;

    private Long pid;

    private Integer exitCode;

    @Column(length = 2048)
    private String errorMessage;

    private Instant startAt;

    private Instant endAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "artifact_dir", length = 1024)
    private String artifactDir;

    @Column(name = "render_events_path", length = 1024)
    private String renderEventsPath;

    @Column(name = "render_probe_status", length = 32)
    private String renderProbeStatus;

    @Column(name = "render_probe_error", length = 2048)
    private String renderProbeError;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = "Pending";
        }
    }
}