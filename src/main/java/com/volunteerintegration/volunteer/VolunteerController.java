package com.volunteerintegration.volunteer;

import com.volunteerintegration.common.LoginMember;
import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import com.volunteerintegration.facility.FacilityRepository;
import com.volunteerintegration.mypage.MyPageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class VolunteerController {

    private final VolunteerRepository volunteerRepository;
    private final FacilityRepository facilityRepository;
    private final MyPageService myPageService;
    private final int pageSize;

    public VolunteerController(VolunteerRepository volunteerRepository, FacilityRepository facilityRepository,
                               MyPageService myPageService, @Value("${app.page-size:12}") int pageSize) {
        this.volunteerRepository = volunteerRepository;
        this.facilityRepository = facilityRepository;
        this.myPageService = myPageService;
        this.pageSize = pageSize;
    }

    /** 통합 검색: 검색창 한 줄 입력 + 지역·분야·날짜 필터 + 정렬 + 페이징. */
    @GetMapping("/search")
    public String search(@ModelAttribute("cond") VolunteerSearchCondition cond, Model model) {
        VolunteerSearchCondition.Resolved resolved = cond.resolve();
        model.addAttribute("resolved", resolved);
        model.addAttribute("result", volunteerRepository.search(resolved, pageSize));
        model.addAttribute("regions", Region.values());
        model.addAttribute("categories", List.of(Category.values()));
        return "volunteer/list";
    }

    @GetMapping("/volunteers/{id}")
    public String detail(@PathVariable String id,
                         @SessionAttribute(name = LoginMember.SESSION_KEY, required = false) LoginMember login,
                         Model model) {
        Volunteer volunteer = volunteerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "봉사활동을 찾을 수 없습니다."));
        model.addAttribute("v", volunteer);
        model.addAttribute("facility",
                volunteer.facilityId() == null ? null : facilityRepository.findById(volunteer.facilityId()).orElse(null));
        model.addAttribute("sameOrg", volunteerRepository.findSameOrg(volunteer.orgName(), id, 5));
        model.addAttribute("favorite", login != null && myPageService.isFavorite(login.id(), id));
        return "volunteer/detail";
    }

    @PostMapping("/volunteers/{id}/favorite")
    public String toggleFavorite(@PathVariable String id,
                                 @SessionAttribute(LoginMember.SESSION_KEY) LoginMember login,
                                 RedirectAttributes redirectAttributes) {
        if (volunteerRepository.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "봉사활동을 찾을 수 없습니다.");
        }
        boolean on = myPageService.toggleFavorite(login.id(), id);
        redirectAttributes.addFlashAttribute("message", on ? "관심 봉사에 담았습니다." : "관심 봉사에서 뺐습니다.");
        return "redirect:/volunteers/" + id;
    }
}
