package com.volunteerintegration.home;

import com.volunteerintegration.domain.Category;
import com.volunteerintegration.facility.FacilityRepository;
import com.volunteerintegration.volunteer.VolunteerRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    private final VolunteerRepository volunteerRepository;
    private final FacilityRepository facilityRepository;

    public HomeController(VolunteerRepository volunteerRepository, FacilityRepository facilityRepository) {
        this.volunteerRepository = volunteerRepository;
        this.facilityRepository = facilityRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", Category.mainCategories());
        // 화면은 분야 코드(ENV 등)로 건수를 찾는다. enum을 맵 키로 넘기면 화면 변환이 실패해 500이 난다.
        Map<String, Long> categoryCounts = new HashMap<>();
        volunteerRepository.countByCategory().forEach((category, count) -> categoryCounts.put(category.getCode(), count));
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("volunteerCount", volunteerRepository.count());
        model.addAttribute("facilityCount", facilityRepository.count());
        model.addAttribute("bloodCenterCount", facilityRepository.countByCategory(Category.BLOOD));
        model.addAttribute("careServiceCount", facilityRepository.countCareServices());
        model.addAttribute("latest", volunteerRepository.findLatest(6));
        return "home";
    }
}
