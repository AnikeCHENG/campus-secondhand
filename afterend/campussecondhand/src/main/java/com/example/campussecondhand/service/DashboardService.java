package com.example.campussecondhand.service;

import com.example.campussecondhand.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;

    public Map<String, Object> stats(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> counts = dashboardRepository.stats(userId);
        if (counts != null) {
            result.put("sellingCount", toLong(counts.get("sellingCount")));
            result.put("pendingOrderCount", toLong(counts.get("pendingOrderCount")));
            result.put("unreadMessageCount", toLong(counts.get("unreadMessageCount")));
        } else {
            result.put("sellingCount", 0L);
            result.put("pendingOrderCount", 0L);
            result.put("unreadMessageCount", 0L);
        }
        result.put("activeUsers", dashboardRepository.activeUsers());
        return result;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
