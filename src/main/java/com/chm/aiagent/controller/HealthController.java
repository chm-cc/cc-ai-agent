package com.chm.aiagent.controller;

import com.chm.aiagent.common.Result;
import com.chm.aiagent.exception.BusinessException;
import com.chm.aiagent.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("health")
public class HealthController {

    @GetMapping
    public Result<String> healthCheck() {
        return Result.ok("ok");
    }

    /** 演示：触发异常 → 全局异常处理器自动捕获 */
    @GetMapping("/test-error")
    public Result<?> testError(@RequestParam(defaultValue = "0") int type) {
        return switch (type) {
            case 1 -> throw new BusinessException(ErrorCode.BAD_REQUEST, "演示参数错误");
            case 2 -> throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            default -> Result.ok("无异常，一切正常");
        };
    }
}