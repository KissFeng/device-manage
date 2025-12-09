package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.service.DeviceService;
import chat.hbcit.devicemanagebackend.service.impl.DeviceServiceImpl;
import chat.hbcit.devicemanagebackend.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备浏览控制器（用户端）
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/devices")
public class DeviceController {
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceServiceImpl deviceServiceImpl;

    /**
     * 分页查询设备列表（用户端）
     *
     * @param name       设备名称（模糊查询）
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<DeviceVO>> getDeviceList(@RequestParam(required = false) String name,
                                                       @RequestParam(required = false) Long categoryId,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size) {
        // 用户端只显示状态为"正常"的设备，且包含设备实例
        // 由于 overallStatus 是计算出来的，需要在应用层过滤，并补充数据
        // 策略：从第1页开始查询，过滤后取第N页的数据
        List<DeviceVO> allNormalDevices = new ArrayList<>();
        int currentPage = 1;
        int maxPages = Math.max(page + 2, 10); // 至少查询到当前页+2页，或至少10页
        int fetchedPages = 0;
        
        // 循环获取数据，直到获取足够的数据或没有更多数据
        while (fetchedPages < maxPages) {
            PageResult<DeviceVO> pageResult = deviceServiceImpl.getDeviceListForUserWithInstances(name, categoryId, currentPage, size);
            
            // 如果当前页没有数据，说明没有更多数据了
            if (pageResult.getList().isEmpty()) {
                break;
            }
            
            // 过滤出状态为"正常"的设备
            List<DeviceVO> normalDevices = pageResult.getList().stream()
                    .filter(vo -> "NORMAL".equals(vo.getOverallStatus()))
                    .collect(java.util.stream.Collectors.toList());
            
            allNormalDevices.addAll(normalDevices);
            
            // 如果已经获取了足够的数据（至少能填满当前页），可以提前退出
            if (allNormalDevices.size() >= page * size) {
                // 如果当前页数据不足（说明是最后一页），退出循环
                if (pageResult.getList().size() < size) {
                    break;
                }
            }
            
            // 如果当前页数据不足（说明是最后一页），退出循环
            if (pageResult.getList().size() < size) {
                break;
            }
            
            // 继续查询下一页
            currentPage++;
            fetchedPages++;
        }
        
        // 计算当前页的数据范围
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, allNormalDevices.size());
        
        // 如果索引超出范围，返回空列表
        List<DeviceVO> pageData;
        if (fromIndex >= allNormalDevices.size()) {
            pageData = new ArrayList<>();
        } else {
            pageData = allNormalDevices.subList(fromIndex, toIndex);
        }
        
        // 计算总数（需要重新查询所有符合条件的设备数量）
        long total = countNormalDevices(name, categoryId);
        
        PageResult<DeviceVO> result = new PageResult<>(total, page, size, pageData);
        return Result.success("获取成功", result);
    }
    
    /**
     * 统计状态为"正常"的设备总数
     */
    private long countNormalDevices(String name, Long categoryId) {
        // 获取所有设备并过滤
        int currentPage = 1;
        int size = 100; // 每次查询100条
        long total = 0;
        int maxPages = 100; // 最多查询100页
        int fetchedPages = 0;
        
        while (fetchedPages < maxPages) {
            PageResult<DeviceVO> pageResult = deviceServiceImpl.getDeviceListForUserWithInstances(name, categoryId, currentPage, size);
            
            if (pageResult.getList().isEmpty()) {
                break;
            }
            
            // 统计状态为"正常"的设备数量
            long normalCount = pageResult.getList().stream()
                    .filter(vo -> "NORMAL".equals(vo.getOverallStatus()))
                    .count();
            
            total += normalCount;
            
            // 如果当前页数据不足，说明没有更多数据了
            if (pageResult.getList().size() < size) {
                break;
            }
            
            currentPage++;
            fetchedPages++;
        }
        
        return total;
    }
}

