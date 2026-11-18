package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.enums.UserRole;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser(Long id, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(id), null, List.of())
        );
        User current = new User();
        current.setId(id);
        current.setRole(role);
        when(userMapper.selectById(id)).thenReturn(current);
    }

    @Test
    void getUserListShouldReturnPagedUsers() {
        User u = new User();
        u.setId(10L);
        u.setUsername("zhaojie");
        when(userMapper.selectUserList("zhao", "USER", 0, 10)).thenReturn(List.of(u));
        when(userMapper.countUsers("zhao", "USER")).thenReturn(1L);

        PageResult<User> page = userService.getUserList("zhao", "USER", 1, 10);

        assertThat(page.getTotal()).isEqualTo(1L);
        assertThat(page.getList()).hasSize(1);
    }

    @Test
    void getUserByIdShouldThrowWhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.NOT_FOUND));
    }

    @Test
    void createUserShouldThrowWhenAdminCreatesAdmin() {
        mockCurrentUser(2L, UserRole.ADMIN.getCode());
        User newUser = new User();
        newUser.setUsername("newadmin");
        newUser.setRole(UserRole.ADMIN.getCode());

        assertThatThrownBy(() -> userService.createUser(newUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.FORBIDDEN));
    }

    @Test
    void createUserShouldEncodePasswordAndInsert() {
        mockCurrentUser(1L, UserRole.ROOT.getCode());
        User newUser = new User();
        newUser.setUsername("student1");
        newUser.setPassword("plain123");
        newUser.setRole(UserRole.USER.getCode());

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("plain123")).thenReturn("encoded123");

        User created = userService.createUser(newUser);

        assertThat(created.getPassword()).isEqualTo("encoded123");
        verify(userMapper).insert(newUser);
    }

    @Test
    void updateUserShouldUpdateFieldsWhenAuthorized() {
        mockCurrentUser(1L, UserRole.ROOT.getCode());
        User exist = new User();
        exist.setId(10L);
        exist.setRole(UserRole.USER.getCode());
        when(userMapper.selectById(10L)).thenReturn(exist);

        User update = new User();
        update.setRealName("新名字");
        update.setRole(UserRole.ADMIN.getCode());

        userService.updateUser(10L, update);

        assertThat(exist.getRealName()).isEqualTo("新名字");
        assertThat(exist.getRole()).isEqualTo(UserRole.ADMIN.getCode());
        verify(userMapper).updateById(exist);
    }
}
