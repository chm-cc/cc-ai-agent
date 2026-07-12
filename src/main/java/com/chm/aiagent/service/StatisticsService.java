package com.chm.aiagent.service;

import com.chm.aiagent.dto.AgentVO;
import com.chm.aiagent.dto.StatisticsVO;
import com.chm.aiagent.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;
    private final AgentService agentService;

    public StatisticsVO getStatistics(int days) {
        LocalDate since = LocalDate.now().minusDays(days - 1);

        StatisticsVO vo = new StatisticsVO();

        // 1. 总览
        StatisticsVO.Overview overview = new StatisticsVO.Overview();
        overview.setTotalConversations(statisticsRepository.countTotalConversations());
        overview.setTotalMessages(statisticsRepository.countTotalMessages());
        overview.setTodayConversations(statisticsRepository.countTodayConversations());
        overview.setTodayMessages(statisticsRepository.countTodayMessages());
        vo.setOverview(overview);

        // 2. Agent 分解
        Map<String, AgentVO> agentMap = new LinkedHashMap<>();
        for (AgentVO a : agentService.listAll()) {
            agentMap.put(a.getId(), a);
        }

        List<StatisticsVO.AgentBreakdown> breakdowns = new ArrayList<>();
        List<Map<String, Object>> rows = statisticsRepository.agentBreakdown();
        for (Map<String, Object> row : rows) {
            String agentId = (String) row.get("agent_id");
            StatisticsVO.AgentBreakdown ab = new StatisticsVO.AgentBreakdown();
            ab.setAgentId(agentId);
            AgentVO agent = agentMap.get(agentId);
            ab.setAgentName(agent != null ? agent.getName() : agentId);
            ab.setConversations(toInt(row.get("conversations")));
            ab.setMessages(toInt(row.get("messages")));
            double avg = 0;
            Object avgObj = row.get("avg_per_conv");
            if (avgObj instanceof Number n) avg = n.doubleValue();
            ab.setAvgPerConv(Math.round(avg * 10.0) / 10.0);
            breakdowns.add(ab);
        }
        vo.setAgentBreakdown(breakdowns);

        // 3. 每日趋势
        List<Map<String, Object>> convDaily = statisticsRepository.dailyConversations(since);
        List<Map<String, Object>> msgDaily = statisticsRepository.dailyMessages(since);

        // 构建日期索引
        Map<String, Integer> convMap = new LinkedHashMap<>();
        for (Map<String, Object> row : convDaily) {
            convMap.put(row.get("day").toString(), toInt(row.get("cnt")));
        }
        Map<String, Integer> msgMap = new LinkedHashMap<>();
        for (Map<String, Object> row : msgDaily) {
            msgMap.put(row.get("day").toString(), toInt(row.get("cnt")));
        }

        List<StatisticsVO.DailyTrend> trends = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = since.plusDays(i).toString();
            StatisticsVO.DailyTrend dt = new StatisticsVO.DailyTrend();
            dt.setDate(date);
            dt.setConversations(convMap.getOrDefault(date, 0));
            dt.setMessages(msgMap.getOrDefault(date, 0));
            trends.add(dt);
        }
        vo.setDailyTrend(trends);

        return vo;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) return Integer.parseInt(s);
        return 0;
    }
}
