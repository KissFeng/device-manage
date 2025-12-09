package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.entity.DeviceInstance;

import java.util.List;

/**
 * 设备实例服务接口
 *
 * @author DeviceManage
 */
public interface DeviceInstanceService {
    /**
     * 根据设备ID查询所有设备实例
     *
     * @param deviceId 设备ID
     * @return 设备实例列表
     */
    List<DeviceInstance> getInstancesByDeviceId(Long deviceId);

    /**
     * 根据ID获取设备实例
     *
     * @param id 设备实例ID
     * @return 设备实例
     */
    DeviceInstance getInstanceById(Long id);

    /**
     * 创建设备实例
     *
     * @param instance 设备实例信息
     * @return 创建的设备实例
     */
    DeviceInstance createInstance(DeviceInstance instance);

    /**
     * 更新设备实例信息
     *
     * @param id       设备实例ID
     * @param instance 设备实例信息
     */
    void updateInstance(Long id, DeviceInstance instance);

    /**
     * 删除设备实例
     *
     * @param id 设备实例ID
     */
    void deleteInstance(Long id);
}

