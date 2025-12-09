package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.dto.PageRequest;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.User;

/**
 * 用户服务接口
 *
 * @author DeviceManage
 */
public interface UserService {
    /**
     * 分页查询用户列表
     *
     * @param username 用户名（模糊查询）
     * @param role     角色
     * @param page     页码
     * @param size     每页数量
     * @return 分页结果
     */
    PageResult<User> getUserList(String username, String role, Integer page, Integer size);

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);

    /**
     * 更新用户信息
     *
     * @param id   用户ID
     * @param user 用户信息
     */
    void updateUser(Long id, User user);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);
}

