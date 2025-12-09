package chat.hbcit.devicemanagebackend.mapper;

import chat.hbcit.devicemanagebackend.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 *
 * @author DeviceManage
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 分页查询用户列表
     *
     * @param username 用户名（模糊查询）
     * @param role     角色
     * @param offset   偏移量
     * @param size     每页数量
     * @return 用户列表
     */
    List<User> selectUserList(@Param("username") String username,
                              @Param("role") String role,
                              @Param("offset") Integer offset,
                              @Param("size") Integer size);

    /**
     * 统计用户总数
     *
     * @param username 用户名（模糊查询）
     * @param role     角色
     * @return 用户总数
     */
    Long countUsers(@Param("username") String username,
                    @Param("role") String role);
}

