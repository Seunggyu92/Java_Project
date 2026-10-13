package com.volunteerintegration.auth;

import com.volunteerintegration.common.LoginMember;
import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import com.volunteerintegration.member.DuplicateEmailException;
import com.volunteerintegration.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    // ------------------------------------------------------------- 로그인

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("redirect", safeRedirect(redirect));
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult bindingResult,
                        @RequestParam(required = false) String redirect,
                        HttpServletRequest request, Model model) {
        model.addAttribute("redirect", safeRedirect(redirect));
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        Optional<LoginMember> member = memberService.login(loginForm.getEmail(), loginForm.getPassword());
        if (member.isEmpty()) {
            bindingResult.reject("loginFail", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "auth/login";
        }
        startSession(request, member.get());
        return "redirect:" + safeRedirect(redirect);
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    // ------------------------------------------------------------ 회원가입

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        addSignupChoices(model);
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupForm signupForm, BindingResult bindingResult,
                         HttpServletRequest request, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            addSignupChoices(model);
            return "auth/signup";
        }
        try {
            LoginMember member = memberService.signup(
                    signupForm.getName(), signupForm.getEmail(), signupForm.getPassword(),
                    signupForm.getPhone(), signupForm.getBloodType(), signupForm.getSido(),
                    signupForm.getInterests());
            startSession(request, member);
        } catch (DuplicateEmailException e) {
            bindingResult.rejectValue("email", "duplicate", "이미 가입된 이메일입니다.");
            addSignupChoices(model);
            return "auth/signup";
        }
        redirectAttributes.addFlashAttribute("message", "가입을 환영합니다.");
        return "redirect:/mypage";
    }

    private static void addSignupChoices(Model model) {
        model.addAttribute("bloodTypes", List.of("A", "B", "O", "AB"));
        model.addAttribute("regions", Region.values());
        model.addAttribute("categories", Category.mainCategories());
    }

    // ------------------------------------------------------------- helpers

    /** 세션 고정 공격을 막기 위해 로그인할 때 세션 ID를 새로 받는다. */
    private static void startSession(HttpServletRequest request, LoginMember member) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(LoginMember.SESSION_KEY, member);
    }

    /** 외부 사이트로 튕기는 오픈 리다이렉트를 막는다. 같은 사이트 경로만 허용. */
    static String safeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank()
                || !redirect.startsWith("/") || redirect.startsWith("//") || redirect.contains("\\")) {
            return "/";
        }
        return redirect;
    }
}
