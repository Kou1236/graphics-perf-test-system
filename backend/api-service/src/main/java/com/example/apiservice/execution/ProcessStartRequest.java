package com.example.apiservice.execution;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProcessStartRequest {

    /**
     * 可执行文件路径
     */
    private String executablePath;

    /**
     * 命令行参数
     */
    private List<String> arguments;

    /**
     * 工作目录（可选）
     */
    private String workingDirectory;

    /**
     * 环境变量（当前轮可以为空）
     */
    private Map<String, String> environment;
}