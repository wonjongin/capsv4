package kr.dgucaps.capsv4.service;

import kr.dgucaps.capsv4.dto.request.*;
import kr.dgucaps.capsv4.dto.response.FindUserIdResponse;
import kr.dgucaps.capsv4.dto.response.GetUserResponse;
import kr.dgucaps.capsv4.dto.response.JwtToken;
import kr.dgucaps.capsv4.entity.User;
import kr.dgucaps.capsv4.exception.DuplicateUserException;
import kr.dgucaps.capsv4.repository.UserRepository;
import kr.dgucaps.capsv4.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void createUser(CreateUserRequest request) {
        isDuplicated(request.getUserId());
        userRepository.save(request.toEntity(passwordEncoder.encode(request.getPassword())));
    }

    public JwtToken login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        return jwtTokenProvider.generateToken(authentication);
    }

    public FindUserIdResponse findUserId(FindUserIdRequest request) {
        User user = userRepository.findByNameAndEmail(request.getName(), request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("해당 정보와 일치하는 회원이 존재하지 않습니다."));
        return FindUserIdResponse.builder().userId(user.getUserId()).build();
    }

    public void validateUserId(String userId) {
        isDuplicated(userId);
    }

    public GetUserResponse getUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원을 찾을 수 없습니다."));
        return GetUserResponse.builder()
                .id(user.getId())
                .permission(user.getPermission())
                .userId(user.getUserId())
                .name(user.getName())
                .grade(user.getGrade())
                .email(user.getEmail())
                .comment(user.getComment())
                .imageName(user.getImageName())
                .point(user.getPoint())
                .totalPoint(user.getTotalPoint())
                .lastLoginDate(user.getLastLoginDate())
                .isDeleted(user.getIsDeleted())
                .build();

    }

    @Transactional
    public void updateUser(String userId, ModifyUserRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원을 찾을 수 없습니다."));
        if (request.getPassword() != null)
            user.updatePassword(passwordEncoder.encode(request.getPassword()));
        if (request.getComment() != null)
            user.updateComment(request.getComment());
    }

    @Transactional
    public void deleteUser(String userId) {
        if (userRepository.existsByUserId(userId)) {
            userRepository.deleteByUserId(userId);
        } else {
            throw new UsernameNotFoundException("해당 회원을 찾을 수 없습니다.");
        }
    }

    public JwtToken renewalToken(TokenRenewalRequest request) {
        String token = request.getRefreshToken();
        if (jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            return jwtTokenProvider.generateToken(authentication);
        }
        return null;
    }

    private void isDuplicated(String userId) {
        if (userRepository.existsByUserId(userId)) {
            throw new DuplicateUserException("이미 사용중인 사용자명 입니다.");
        }
    }
}
