package com.msb.supsale.service;

import com.msb.supsale.model.SaleActivity;
import com.msb.supsale.repository.SaleActivityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
public class SaleActivityService {
    private final SaleActivityRepository saleActivityRepository;

    public SaleActivityService(SaleActivityRepository saleActivityRepository) {
        this.saleActivityRepository = saleActivityRepository;
    }

    public SaleActivity logActivity(UUID leadId, UUID saleUserId, String action, String note) {
        SaleActivity activity = new SaleActivity();
        activity.setLeadId(leadId);
        activity.setSaleUserId(saleUserId);
        activity.setAction(action);
        activity.setNote(note);
        return saleActivityRepository.save(activity);
    }

    public List<SaleActivity> getBySaleUser(UUID saleUserId) {
        return saleActivityRepository.findBySaleUserId(saleUserId);
    }

    public List<SaleActivity> getByLead(UUID leadId) {
        return saleActivityRepository.findByLeadId(leadId);
    }

    public List<Map<String, Object>> getDailyActivity(UUID saleUserId, int days) {
        List<SaleActivity> activities = saleActivityRepository.findBySaleUserId(saleUserId);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        Map<LocalDate, Long> counts = new HashMap<>();
        for (SaleActivity a : activities) {
            LocalDate date = a.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!date.isBefore(startDate)) {
                counts.merge(date, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            result.add(Map.of("date", date.toString(), "count", counts.getOrDefault(date, 0L)));
        }
        return result;
    }
}
