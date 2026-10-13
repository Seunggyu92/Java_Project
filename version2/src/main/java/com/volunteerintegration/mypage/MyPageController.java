package com.volunteerintegration.mypage;

import com.volunteerintegration.common.LoginMember;
import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import com.volunteerintegration.member.Member;
import com.volunteerintegration.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** 마이페이지: 대시보드, 내 정보, 활동 기록, 찜, 추천. 로그인 필수 (LoginInterceptor). */
@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final MemberService memberService;
    private final MyPageService myPageService;

    public MyPageController(MemberService memberService, MyPageService myPageService) {
        this.memberService = memberService;
        this.myPageService = myPageService;
    }

    @GetMapping
    public String home(@SessionAttribute(LoginMember.SESSION_KEY) LoginMember login, Model model) {
        Member member = memberService.get(login.id());
        List<Category> interests = memberService.interests(login.id());

        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setName(member.name());
            form.setPhone(member.phone());
            form.setBloodType(member.bloodType());
            form.setSido(member.sido());
            form.setInterests(interests.stream().map(Category::getCode).toList());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("recordForm")) {
            RecordForm form = new RecordForm();
            form.setActivityDate(LocalDate.now());
            model.addAttribute("recordForm", form);
        }
        model.addAttribute("member", member);
        model.addAttribute("interests", interests);
        model.addAttribute("summary", myPageService.summary(login.id()));
        model.addAttribute("monthly", myPageService.monthlyHours(login.id(), YearMonth.now()));
        model.addAttribute("records", myPageService.records(login.id()));
        model.addAttribute("favorites", myPageService.favorites(login.id()));
        model.addAttribute("recommendations", myPageService.recommendations(login.id(), member.sido()));
        model.addAttribute("categories", Category.mainCategories());
        model.addAttribute("regions", Region.values());
        model.addAttribute("recordTypes", RecordType.values());
        model.addAttribute("bloodTypes", List.of("A", "B", "O", "AB"));
        return "mypage/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@SessionAttribute(LoginMember.SESSION_KEY) LoginMember login,
                                @Valid @ModelAttribute ProfileForm profileForm, BindingResult bindingResult,
                                HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            keepErrors(redirectAttributes, "profileForm", profileForm, bindingResult);
            return "redirect:/mypage#profile";
        }
        memberService.updateProfile(login.id(), profileForm.getName(), profileForm.getPhone(),
                profileForm.getBloodType(), profileForm.getSido(), profileForm.getInterests());
        // 헤더에 보이는 이름도 바꾼다.
        request.getSession().setAttribute(LoginMember.SESSION_KEY,
                new LoginMember(login.id(), profileForm.getName().trim(), login.email()));
        redirectAttributes.addFlashAttribute("message", "내 정보를 저장했습니다. 추천 봉사도 새 관심 분야로 바뀌었어요.");
        return "redirect:/mypage#profile";
    }

    @PostMapping("/records")
    public String addRecord(@SessionAttribute(LoginMember.SESSION_KEY) LoginMember login,
                            @Valid @ModelAttribute RecordForm recordForm, BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {
        RecordType type = RecordType.fromCode(recordForm.getType());
        if (type == null) {
            bindingResult.rejectValue("type", "invalid", "종류를 다시 고르세요.");
        } else if (type == RecordType.VOLUNTEER && recordForm.getHours() == null) {
            bindingResult.rejectValue("hours", "required", "봉사시간을 입력하세요.");
        } else if (type == RecordType.DONATION && recordForm.getAmount() == null) {
            bindingResult.rejectValue("amount", "required", "기부 금액을 입력하세요.");
        }
        if (bindingResult.hasErrors()) {
            keepErrors(redirectAttributes, "recordForm", recordForm, bindingResult);
            return "redirect:/mypage#record-form";
        }
        myPageService.addRecord(login.id(), type, recordForm);
        redirectAttributes.addFlashAttribute("message", type.getLabel() + " 기록을 추가했습니다.");
        return "redirect:/mypage#records";
    }

    @PostMapping("/records/{recordId}/delete")
    public String deleteRecord(@SessionAttribute(LoginMember.SESSION_KEY) LoginMember login,
                               @PathVariable long recordId, RedirectAttributes redirectAttributes) {
        boolean deleted = myPageService.deleteRecord(login.id(), recordId);
        redirectAttributes.addFlashAttribute("message", deleted ? "기록을 삭제했습니다." : "삭제할 기록이 없습니다.");
        return "redirect:/mypage#records";
    }

    @PostMapping("/withdraw")
    public String withdraw(@SessionAttribute(LoginMember.SESSION_KEY) LoginMember login, HttpServletRequest request) {
        memberService.withdraw(login.id());
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    /** PRG 패턴에서 검증 오류를 리다이렉트 후 화면까지 넘긴다. */
    private static void keepErrors(RedirectAttributes ra, String name, Object form, BindingResult result) {
        ra.addFlashAttribute(name, form);
        ra.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + name, result);
    }
}
