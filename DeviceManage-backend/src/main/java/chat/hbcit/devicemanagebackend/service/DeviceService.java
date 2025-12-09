package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.Device;

/**
 * 设备服务接口
 *
 * @author DeviceManage
 */
public interface DeviceService {
    /**
     * 分页查询设备列表（管理员）
     *
     * @param name       设备名称（模糊查询）
     * @param model      型号（模糊查询）
     * @param status     状态
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    PageResult<Device> getDeviceList(String name, String model, String status, Long categoryId, Integer page, Integer size);

    /**
     * 分页查询设备列表（用户端）
     *
     * @param name       设备名称（模糊查询）
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    PageResult<Device> getDeviceListForUser(String name, Long categoryId, Integer page, Integer size);

    /**
     * 根据ID获取设备详情
     *
     * @param id 设备ID
     * @return 设备信息
     */
    Device getDeviceById(Long id);

    /**
     * 创建设备
     *
     * @param device 设备信息
     * @return 创建的设备
     */
    Device createDevice(Device device);

    /**
     * 更新设备信息
     *
     * @param id     设备ID
     * @param device 设备信息
     */
    void updateDevice(Long id, Device device);

    /**
     * 更新设备状态
     *
     * @param id     设备ID
     * @param status 状态
     */
    void updateDeviceStatus(Long id, String status);

    /**
     * 删除设备
     *
     * @param id 设备ID
     */
    void deleteDevice(Long id);
}

