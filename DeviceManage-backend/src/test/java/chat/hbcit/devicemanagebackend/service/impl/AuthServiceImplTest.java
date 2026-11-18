package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.dto.LoginRequest;
import chat.hbcit.devicemanagebackend.dto.LoginResponse;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.enums.UserRole;
import chat.hbcit.devicemanagebackend.enums.UserStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.UserMapper;
import chat.hbcit.devicemanagebackend.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void loginShouldThrowWhenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("missing");
        req.setPassword("123456");
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.UNAUTHORIZED));
    }

    @Test
    void loginShouldThrowWhenPasswordMismatch() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hashed");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.UNAUTHORIZED));
    }

    @Test
    void loginShouldThrowWhenUserDisabled() {
        LoginRequest req = new LoginRequest();
        req.setUsername("bob");
        req.setPassword("123456");
        User user = new User();
        user.setUsername("bob");
        user.setPassword("hashed");
        user.setStatus(UserStatus.INACTIVE.getCode());
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.FORBIDDEN));
    }

    @Test
    void loginShouldSucceedAndReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("zhaojie");
        req.setPassword("123456");
        User user = new User();
        user.setId(10L);
        user.setUsername("zhaojie");
        user.setRealName("赵杰");
        user.setPassword("hashed");
        user.setRole(UserRole.USER.getCode());
        user.setStatus(UserStatus.ACTIVE.getCode());

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(10L, "zhaojie", UserRole.USER.getCode())).thenReturn("jwt-token-xyz");

        LoginResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token-xyz");
        assertThat(response.getUserInfo().getUsername()).isEqualTo("zhaojie");
        assertThat(response.getUserInfo().getRealName()).isEqualTo("赵杰");
    }
}
