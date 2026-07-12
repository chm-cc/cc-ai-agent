package com.chm.aiagent.controller;

import com.chm.aiagent.common.Result;
import com.chm.aiagent.dto.StatisticsVO;
import com.chm.aiagent.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public Result<StatisticsVO> get(@RequestParam(defaultValue = "30") int days) {
        if (days < 1) days = 7;
        if (days > 365) days = 30;
        return Result.ok(statisticsService.getStatistics(days));
    }
}
