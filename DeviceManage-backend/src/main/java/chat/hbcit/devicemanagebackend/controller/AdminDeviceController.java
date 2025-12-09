package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.service.DeviceService;
import chat.hbcit.devicemanagebackend.service.impl.DeviceServiceImpl;
import chat.hbcit.devicemanagebackend.vo.DeviceVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员设备管理控制器
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/admin/devices")
public class AdminDeviceController {
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceServiceImpl deviceServiceImpl;

    /**
     * 分页查询设备列表
     *
     * @param name       设备名称（模糊查询）
     * @param model      型号（模糊查询）
     * @param status     状态
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<DeviceVO>> getDeviceList(@RequestParam(required = false) String name,
                                                       @RequestParam(required = false) String model,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Long categoryId,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size) {
        PageResult<DeviceVO> result = deviceServiceImpl.getDeviceListWithInstances(name, model, status, categoryId, page, size);
        return Result.success("获取成功", result);
    }

    /**
     * 根据ID获取设备详情
     *
     * @param id 设备ID
     * @return 设备信息
     */
    @GetMapping("/{id}")
    public Result<DeviceVO> getDeviceById(@PathVariable Long id) {
        DeviceVO device = deviceServiceImpl.getDeviceByIdWithInstances(id);
        return Result.success("获取成功", device);
    }

    /**
     * 创建设备
     *
     * @param device 设备信息
     * @return 创建的设备
     */
    @PostMapping
    public Result<Device> createDevice(@Valid @RequestBody Device device) {
        Device createdDevice = deviceService.createDevice(device);
        return Result.success("创建成功", createdDevice);
    }

    /**
     * 更新设备信息
     *
     * @param id     设备ID
     * @param device 设备信息
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public Result<?> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        deviceService.updateDevice(id, device);
        return Result.success("更新成功");
    }

    /**
     * 更新设备状态
     *
     * @param id     设备ID
     * @param params 请求参数（包含status）
     * @return 响应结果
     */
    @PutMapping("/{id}/status")
    public Result<?> updateDeviceStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String status = params.get("status");
        if (status == null || status.isEmpty()) {
            return Result.error(400, "状态不能为空");
        }
        deviceService.updateDeviceStatus(id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 删除设备
     *
     * @param id 设备ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success("删除成功");
    }
}

