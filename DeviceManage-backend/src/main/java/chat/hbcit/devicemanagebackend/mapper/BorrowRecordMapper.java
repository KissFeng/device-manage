package chat.hbcit.devicemanagebackend.mapper;

import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 借用记录Mapper接口
 *
 * @author DeviceManage
 */
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {
    /**
     * 分页查询我的借用记录
     *
     * @param userId 用户ID
     * @param status 状态
     * @param offset 偏移量
     * @param size   每页数量
     * @return 借用记录列表
     */
    List<BorrowRecordVO> selectMyRecords(@Param("userId") Long userId,
                                       @Param("status") String status,
                                       @Param("offset") Integer offset,
                                       @Param("size") Integer size);

    /**
     * 统计我的借用记录总数
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 记录总数
     */
    Long countMyRecords(@Param("userId") Long userId,
                       @Param("status") String status);

    /**
     * 分页查询待审批申请列表
     *
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param offset     偏移量
     * @param size       每页数量
     * @return 借用记录列表
     */
    List<BorrowRecordVO> selectPendingRecords(@Param("deviceName") String deviceName,
                                           @Param("username") String username,
                                           @Param("offset") Integer offset,
                                           @Param("size") Integer size);

    /**
     * 统计待审批申请总数
     *
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @return 记录总数
     */
    Long countPendingRecords(@Param("deviceName") String deviceName,
                            @Param("username") String username);

    /**
     * 分页查询所有借用记录（管理员）
     *
     * @param status     状态
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @param offset     偏移量
     * @param size       每页数量
     * @return 借用记录列表
     */
    List<BorrowRecordVO> selectAllRecords(@Param("status") String status,
                                       @Param("deviceName") String deviceName,
                                       @Param("username") String username,
                                       @Param("offset") Integer offset,
                                       @Param("size") Integer size);

    /**
     * 统计所有借用记录总数（管理员）
     *
     * @param status     状态
     * @param deviceName 设备名称（模糊查询）
     * @param username   申请人用户名（模糊查询）
     * @return 记录总数
     */
    Long countAllRecords(@Param("status") String status,
                        @Param("deviceName") String deviceName,
                        @Param("username") String username);
}

