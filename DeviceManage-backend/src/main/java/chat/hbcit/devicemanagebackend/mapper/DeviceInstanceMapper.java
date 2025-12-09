package chat.hbcit.devicemanagebackend.mapper;

import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备实例Mapper接口
 *
 * @author DeviceManage
 */
public interface DeviceInstanceMapper extends BaseMapper<DeviceInstance> {
    /**
     * 根据设备ID查询所有设备实例
     *
     * @param deviceId 设备ID
     * @return 设备实例列表
     */
    List<DeviceInstance> selectByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 查询设备实例（包含是否已借出信息）
     *
     * @param deviceId 设备ID
     * @return 设备实例列表（包含borrowed字段）
     */
    List<DeviceInstance> selectWithBorrowStatus(@Param("deviceId") Long deviceId);
}

