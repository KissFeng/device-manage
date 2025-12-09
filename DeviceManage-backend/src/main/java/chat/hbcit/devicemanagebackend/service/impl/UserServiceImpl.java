package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.enums.UserRole;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.UserMapper;
import chat.hbcit.devicemanagebackend.service.UserService;
import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类
 *
 * @author DeviceManage
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    public PageResult<User> getUserList(String username, String role, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<User> list = userMapper.selectUserList(username, role, offset, size);
        Long total = userMapper.countUsers(username, role);
        return new PageResult<>(total, page, size, list);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(User user) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(authentication.getName());
        User currentUser = userMapper.selectById(currentUserId);
        
        // 检查权限：只有 ROOT 可以创建 ADMIN 或 ROOT，ADMIN 只能创建 USER
        if (user.getRole() != null) {
            if (!UserRole.canManage(currentUser.getRole(), user.getRole())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "无权创建该角色的用户");
            }
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, user.getUsername());
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "用户名已存在");
        }

        // 验证角色
        if (user.getRole() != null && UserRole.fromCode(user.getRole()) == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "角色无效");
        }

        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userMapper.insert(user);
        logger.info("创建用户成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, User user) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(authentication.getName());
        User currentUser = userMapper.selectById(currentUserId);
        
        User existUser = getUserById(id);
        
        // 检查权限：只有 ROOT 可以修改 ADMIN 或 ROOT，ADMIN 只能修改 USER
        if (!UserRole.canManage(currentUser.getRole(), existUser.getRole())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权修改该用户");
        }

        // 如果修改角色，需要再次检查权限
        if (user.getRole() != null && !user.getRole().equals(existUser.getRole())) {
            if (!UserRole.canManage(currentUser.getRole(), user.getRole())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "无权将该用户设置为该角色");
            }
        }

        // 更新字段
        if (user.getRealName() != null) {
            existUser.setRealName(user.getRealName());
        }
        if (user.getRole() != null) {
            if (UserRole.fromCode(user.getRole()) == null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "角色无效");
            }
            existUser.setRole(user.getRole());
        }
        if (user.getStatus() != null) {
            existUser.setStatus(user.getStatus());
        }

        userMapper.updateById(existUser);
        logger.info("更新用户成功，用户ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(authentication.getName());
        User currentUser = userMapper.selectById(currentUserId);
        
        User user = getUserById(id);
        
        // 检查权限：只有 ROOT 可以删除 ADMIN 或 ROOT，ADMIN 只能删除 USER
        if (!UserRole.canManage(currentUser.getRole(), user.getRole())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权删除该用户");
        }
        
        // 不能删除自己
        if (currentUserId.equals(id)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "不能删除自己的账号");
        }

        // 删除所有与该用户相关的借用记录（包括所有状态的记录）
        LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
        borrowWrapper.eq(BorrowRecord::getUserId, id);
        Long deletedBorrowCount = borrowRecordMapper.selectCount(borrowWrapper);
        if (deletedBorrowCount > 0) {
            borrowRecordMapper.delete(borrowWrapper);
            logger.info("删除用户相关的借用记录，用户ID：{}，删除记录数：{}", id, deletedBorrowCount);
        }

        // 删除用户
        userMapper.deleteById(id);
        logger.info("删除用户成功，用户ID：{}，用户名：{}", id, user.getUsername());
    }
}

