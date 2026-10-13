package com.volunteerintegration.mypage;

import com.volunteerintegration.volunteer.Volunteer;
import com.volunteerintegration.volunteer.VolunteerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MyPageService {

    private static final int CHART_MONTHS = 6;

    private final RecordRepository recordRepository;
    private final FavoriteRepository favoriteRepository;
    private final VolunteerRepository volunteerRepository;

    public MyPageService(RecordRepository recordRepository, FavoriteRepository favoriteRepository,
                         VolunteerRepository volunteerRepository) {
        this.recordRepository = recordRepository;
        this.favoriteRepository = favoriteRepository;
        this.volunteerRepository = volunteerRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(long memberId) {
        return recordRepository.summary(memberId);
    }

    /** 최근 6개월 봉사시간 막대그래프 (기록 없는 달은 0). */
    @Transactional(readOnly = true)
    public List<MonthlyHours> monthlyHours(long memberId, YearMonth current) {
        YearMonth first = current.minusMonths(CHART_MONTHS - 1L);
        Map<String, BigDecimal> totals = recordRepository.monthlyVolunteerHours(memberId, first.atDay(1));
        BigDecimal max = totals.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        List<MonthlyHours> result = new ArrayList<>();
        for (int i = 0; i < CHART_MONTHS; i++) {
            YearMonth ym = first.plusMonths(i);
            String key = ym.toString(); // YYYY-MM
            BigDecimal hours = totals.getOrDefault(key, BigDecimal.ZERO);
            int percent = max.signum() == 0 ? 0
                    : hours.multiply(BigDecimal.valueOf(100)).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue();
            result.add(new MonthlyHours(key, ym.getMonthValue() + "월", hours, percent));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MyRecord> records(long memberId) {
        return recordRepository.findByMember(memberId, 50);
    }

    @Transactional
    public void addRecord(long memberId, RecordType type, RecordForm form) {
        String volunteerId = blankToNull(form.getVolunteerId());
        if (volunteerId != null && volunteerRepository.findById(volunteerId).isEmpty()) {
            volunteerId = null;
        }
        BigDecimal hours = type == RecordType.VOLUNTEER ? form.getHours() : null;
        Long amount = type == RecordType.DONATION ? form.getAmount() : null;
        recordRepository.insert(memberId, type, form.getActivityDate(), blankToNull(form.getPlace()),
                hours, amount, blankToNull(form.getMemo()), volunteerId);
    }

    @Transactional
    public boolean deleteRecord(long memberId, long recordId) {
        return recordRepository.delete(memberId, recordId) > 0;
    }

    @Transactional(readOnly = true)
    public List<Volunteer> favorites(long memberId) {
        return favoriteRepository.findVolunteers(memberId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(long memberId, String volunteerId) {
        return favoriteRepository.exists(memberId, volunteerId);
    }

    /** 찜 토글. 찜 상태가 되면 true. */
    @Transactional
    public boolean toggleFavorite(long memberId, String volunteerId) {
        if (favoriteRepository.exists(memberId, volunteerId)) {
            favoriteRepository.remove(memberId, volunteerId);
            return false;
        }
        favoriteRepository.add(memberId, volunteerId);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Volunteer> recommendations(long memberId, String memberSido) {
        return volunteerRepository.recommend(memberId, memberSido, 6);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
