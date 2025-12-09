package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员设备实例管理控制器
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/admin/device-instances")
public class AdminDeviceInstanceController {
    @Autowired
    private DeviceInstanceService deviceInstanceService;

    /**
     * 根据设备ID查询所有设备实例
     *
     * @param deviceId 设备ID
     * @return 设备实例列表
     */
    @GetMapping("/device/{deviceId}")
    public Result<List<DeviceInstance>> getInstancesByDeviceId(@PathVariable Long deviceId) {
        List<DeviceInstance> instances = deviceInstanceService.getInstancesByDeviceId(deviceId);
        return Result.success("获取成功", instances);
    }

    /**
     * 根据ID获取设备实例详情
     *
     * @param id 设备实例ID
     * @return 设备实例信息
     */
    @GetMapping("/{id}")
    public Result<DeviceInstance> getInstanceById(@PathVariable Long id) {
        DeviceInstance instance = deviceInstanceService.getInstanceById(id);
        return Result.success("获取成功", instance);
    }

    /**
     * 创建设备实例
     *
     * @param instance 设备实例信息
     * @return 创建的设备实例
     */
    @PostMapping
    public Result<DeviceInstance> createInstance(@Valid @RequestBody DeviceInstance instance) {
        DeviceInstance createdInstance = deviceInstanceService.createInstance(instance);
        return Result.success("创建成功", createdInstance);
    }

    /**
     * 更新设备实例信息
     *
     * @param id       设备实例ID
     * @param instance 设备实例信息
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public Result<?> updateInstance(@PathVariable Long id, @RequestBody DeviceInstance instance) {
        deviceInstanceService.updateInstance(id, instance);
        return Result.success("更新成功");
    }

    /**
     * 删除设备实例
     *
     * @param id 设备实例ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteInstance(@PathVariable Long id) {
        deviceInstanceService.deleteInstance(id);
        return Result.success("删除成功");
    }
}

