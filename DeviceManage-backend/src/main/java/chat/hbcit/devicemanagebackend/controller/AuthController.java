package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.LoginRequest;
import chat.hbcit.devicemanagebackend.dto.LoginResponse;
import chat.hbcit.devicemanagebackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author DeviceManage
 */
@Tag(name = "认证与登录", description = "用户登录签发 JWT 令牌与退出登录")
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Operation(summary = "用户登录", description = "校验用户名与密码并签发 JWT Token（匿名可访问）")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户退出登录
     *
     * @return 响应结果
     */
    @Operation(summary = "退出登录", description = "前端清除令牌并退出会话")
    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success("退出成功");
    }
}

