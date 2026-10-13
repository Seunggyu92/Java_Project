package com.volunteerintegration.member;

import com.volunteerintegration.common.LoginMember;
import com.volunteerintegration.domain.Category;
import com.volunteerintegration.domain.Region;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class MemberService {

    private static final Set<String> BLOOD_TYPES = Set.of("A", "B", "O", "AB");

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 회원가입. 비밀번호는 BCrypt 해시로만 저장한다. */
    @Transactional
    public LoginMember signup(String name, String email, String rawPassword,
                               String phone, String bloodType, String sido, List<String> interestCodes) {
        String normalizedEmail = normalizeEmail(email);
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
        try {
            long id = memberRepository.insert(
                    normalizedEmail, passwordEncoder.encode(rawPassword), name.trim(),
                    phone.trim(), normalizeBlood(bloodType), Region.normalize(sido));
            memberRepository.replaceInterests(id, toCategories(interestCodes));
            return new LoginMember(id, name.trim(), normalizedEmail);
        } catch (DuplicateKeyException e) {
            // 동시에 같은 이메일로 가입한 경우 (UNIQUE 제약)
            throw new DuplicateEmailException(normalizedEmail);
        }
    }

    @Transactional(readOnly = true)
    public Optional<LoginMember> login(String email, String rawPassword) {
        return memberRepository.findByEmail(normalizeEmail(email))
                .filter(m -> passwordEncoder.matches(rawPassword, m.pwHash()))
                .map(m -> new LoginMember(m.id(), m.name(), m.email()));
    }

    @Transactional(readOnly = true)
    public Member get(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("회원이 없습니다: " + memberId));
    }

    @Transactional(readOnly = true)
    public List<Category> interests(long memberId) {
        return memberRepository.findInterests(memberId);
    }

    @Transactional
    public void updateProfile(long memberId, String name, String phone, String bloodType, String sido,
                               List<String> interestCodes) {
        memberRepository.updateProfile(memberId, name.trim(), phone.trim(), normalizeBlood(bloodType),
                Region.normalize(sido));
        memberRepository.replaceInterests(memberId, toCategories(interestCodes));
    }

    private static String normalizeBlood(String bloodType) {
        String blood = bloodType == null ? "" : bloodType.trim().toUpperCase(Locale.ROOT);
        return BLOOD_TYPES.contains(blood) ? blood : null;
    }

    private static List<Category> toCategories(List<String> interestCodes) {
        if (interestCodes == null) {
            return List.of();
        }
        return interestCodes.stream().map(Category::fromCode).filter(c -> c != null).distinct().toList();
    }

    @Transactional
    public void withdraw(long memberId) {
        memberRepository.delete(memberId); // 관심분야·찜·기록은 ON DELETE CASCADE
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
