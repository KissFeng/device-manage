package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理控制器
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    @Autowired
    private UserService userService;

    /**
     * 分页查询用户列表
     *
     * @param username 用户名（模糊查询）
     * @param role     角色
     * @param page     页码
     * @param size     每页数量
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<User>> getUserList(@RequestParam(required = false) String username,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size) {
        PageResult<User> result = userService.getUserList(username, role, page, size);
        return Result.success("获取成功", result);
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success("获取成功", user);
    }

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建的用户
     */
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.createUser(user);
        return Result.success("创建成功", createdUser);
    }

    /**
     * 更新用户信息
     *
     * @param id   用户ID
     * @param user 用户信息
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        userService.updateUser(id, user);
        return Result.success("更新成功");
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }
}

