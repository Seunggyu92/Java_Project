package com.volunteerintegration.facility;

import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import com.volunteerintegration.volunteer.VolunteerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FacilityController {

    private final FacilityRepository facilityRepository;
    private final VolunteerRepository volunteerRepository;
    private final int pageSize;

    public FacilityController(FacilityRepository facilityRepository, VolunteerRepository volunteerRepository,
                              @Value("${app.page-size:12}") int pageSize) {
        this.facilityRepository = facilityRepository;
        this.volunteerRepository = volunteerRepository;
        this.pageSize = pageSize;
    }

    @GetMapping("/facilities")
    public String list(@ModelAttribute("cond") FacilitySearchCondition cond, Model model) {
        model.addAttribute("result", facilityRepository.search(cond, pageSize));
        model.addAttribute("regions", Region.values());
        model.addAttribute("categories", List.of(Category.CARE, Category.EDU, Category.BLOOD));
        model.addAttribute("sources", facilityRepository.findSources());
        return "facility/list";
    }

    @GetMapping("/facilities/{id}")
    public String detail(@PathVariable String id, Model model) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시설을 찾을 수 없습니다."));
        model.addAttribute("facility", facility);
        model.addAttribute("linked", facilityRepository.findLinked(id));
        model.addAttribute("careServices", facilityRepository.findCareServices(id));
        model.addAttribute("volunteers", volunteerRepository.findByFacility(id, 10));
        return "facility/detail";
    }

    /** 헌혈의 집: 지역별로 묶어서 보여 준다. */
    @GetMapping("/blood")
    public String bloodCenters(@RequestParam(required = false) String sido, Model model) {
        String region = Region.normalize(sido);
        List<Facility> centers = facilityRepository.findBloodCenters(region);
        Map<String, List<Facility>> bySido = new LinkedHashMap<>();
        for (Facility f : centers) {
            bySido.computeIfAbsent(f.sido() == null ? "기타" : f.sido(), k -> new java.util.ArrayList<>()).add(f);
        }
        model.addAttribute("bySido", bySido);
        model.addAttribute("total", centers.size());
        model.addAttribute("selectedSido", region);
        model.addAttribute("regions", Region.values());
        return "facility/blood";
    }
}
