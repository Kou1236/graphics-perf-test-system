package com.example.apiservice.execution;

import lombok.Data;

import java.time.Instant;

@Data
public class RunExecutionResult {

    /**
     * 进程 PID（启动失败时可能为 null）
     */
    private Long pid;

    /**
     * 进程启动时间
     */
    private Instant startAt;

    /**
     * 进程结束时间
     */
    private Instant endAt;

    /**
     * 退出码（正常为 0，失败或启动异常为非 0）
     */
    private int exitCode;

    /**
     * 错误信息（仅失败时填写）
     */
    private String errorMessage;
}