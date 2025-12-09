package chat.hbcit.devicemanagebackend.mapper;

import chat.hbcit.devicemanagebackend.entity.Device;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备Mapper接口
 *
 * @author DeviceManage
 */
public interface DeviceMapper extends BaseMapper<Device> {
    /**
     * 分页查询设备列表（管理员）
     *
     * @param name       设备名称（模糊查询）
     * @param model      型号（模糊查询）
     * @param status     状态
     * @param categoryId 分类ID
     * @param offset     偏移量
     * @param size       每页数量
     * @return 设备列表
     */
    List<Device> selectDeviceList(@Param("name") String name,
                                  @Param("model") String model,
                                  @Param("status") String status,
                                  @Param("categoryId") Long categoryId,
                                  @Param("offset") Integer offset,
                                  @Param("size") Integer size);

    /**
     * 统计设备总数（管理员）
     *
     * @param name       设备名称（模糊查询）
     * @param model      型号（模糊查询）
     * @param status     状态
     * @param categoryId 分类ID
     * @return 设备总数
     */
    Long countDevices(@Param("name") String name,
                      @Param("model") String model,
                      @Param("status") String status,
                      @Param("categoryId") Long categoryId);

    /**
     * 分页查询设备列表（用户端，只显示正常状态的设备）
     *
     * @param name       设备名称（模糊查询）
     * @param categoryId 分类ID
     * @param offset     偏移量
     * @param size       每页数量
     * @return 设备列表
     */
    List<Device> selectDeviceListForUser(@Param("name") String name,
                                         @Param("categoryId") Long categoryId,
                                         @Param("offset") Integer offset,
                                         @Param("size") Integer size);

    /**
     * 统计设备总数（用户端）
     *
     * @param name       设备名称（模糊查询）
     * @param categoryId 分类ID
     * @return 设备总数
     */
    Long countDevicesForUser(@Param("name") String name,
                             @Param("categoryId") Long categoryId);
}

