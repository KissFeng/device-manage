package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.CategoryMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
import chat.hbcit.devicemanagebackend.service.DeviceService;
import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.vo.DeviceInstanceVO;
import chat.hbcit.devicemanagebackend.vo.DeviceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备服务实现类
 *
 * @author DeviceManage
 */
@Service
public class DeviceServiceImpl implements DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private DeviceInstanceMapper deviceInstanceMapper;

    @Autowired
    private DeviceInstanceService deviceInstanceService;

    @Override
    public PageResult<Device> getDeviceList(String name, String model, String status, Long categoryId, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<Device> list = deviceMapper.selectDeviceList(name, model, status, categoryId, offset, size);
        Long total = deviceMapper.countDevices(name, model, status, categoryId);
        return new PageResult<>(total, page, size, list);
    }

    /**
     * 分页查询设备列表（包含设备实例）
     *
     * @param name       设备名称（模糊查询）
     * @param model      型号（模糊查询）
     * @param status     状态
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果（包含设备实例）
     */
    public PageResult<DeviceVO> getDeviceListWithInstances(String name, String model, String status, Long categoryId, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        // 注意：status 参数现在用于筛选 overallStatus，但数据库查询仍使用设备模板的 status
        // 先查询所有设备，然后在内存中根据 overallStatus 筛选
        List<Device> deviceList = deviceMapper.selectDeviceList(name, model, null, categoryId, offset, size);
        Long total = deviceMapper.countDevices(name, model, null, categoryId);

        // 转换为 DeviceVO 并加载设备实例
        List<DeviceVO> voList = deviceList.stream().map(device -> {
            DeviceVO vo = new DeviceVO();
            BeanUtils.copyProperties(device, vo);

            // 加载分类名称
            if (device.getCategoryId() != null) {
                var category = categoryMapper.selectById(device.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }

            // 加载设备实例
            List<DeviceInstance> instances = deviceInstanceMapper.selectWithBorrowStatus(device.getId());
            List<DeviceInstanceVO> instanceVOs = instances.stream().map(inst -> {
                DeviceInstanceVO instanceVO = new DeviceInstanceVO();
                BeanUtils.copyProperties(inst, instanceVO);
                // 检查是否已借出（通过查询借用记录）
                LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
                borrowWrapper.eq(BorrowRecord::getDeviceInstanceId, inst.getId())
                        .in(BorrowRecord::getStatus, BorrowStatus.PENDING.getCode(), BorrowStatus.APPROVED.getCode());
                Long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);
                instanceVO.setBorrowed(borrowCount > 0);
                return instanceVO;
            }).collect(Collectors.toList());
            vo.setInstances(instanceVOs);

            // 计算设备整体状态
            vo.setOverallStatus(calculateDeviceStatus(instanceVOs));

            return vo;
        }).collect(Collectors.toList());

        // 根据 overallStatus 筛选（如果指定了 status 参数）
        if (status != null && !status.isEmpty()) {
            voList = voList.stream()
                    .filter(vo -> status.equals(vo.getOverallStatus()))
                    .collect(Collectors.toList());
            // 重新计算总数（简化处理，实际应该优化为数据库查询）
            total = (long) voList.size();
        }

        return new PageResult<>(total, page, size, voList);
    }

    @Override
    public PageResult<Device> getDeviceListForUser(String name, Long categoryId, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<Device> list = deviceMapper.selectDeviceListForUser(name, categoryId, offset, size);
        Long total = deviceMapper.countDevicesForUser(name, categoryId);
        return new PageResult<>(total, page, size, list);
    }

    /**
     * 分页查询设备列表（用户端，包含设备实例）
     *
     * @param name       设备名称（模糊查询）
     * @param categoryId 分类ID
     * @param page       页码
     * @param size       每页数量
     * @return 分页结果（包含设备实例）
     */
    public PageResult<DeviceVO> getDeviceListForUserWithInstances(String name, Long categoryId, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<Device> deviceList = deviceMapper.selectDeviceListForUser(name, categoryId, offset, size);
        Long total = deviceMapper.countDevicesForUser(name, categoryId);

        // 转换为 DeviceVO 并加载设备实例
        List<DeviceVO> voList = deviceList.stream().map(device -> {
            DeviceVO vo = new DeviceVO();
            BeanUtils.copyProperties(device, vo);

            // 加载分类名称
            if (device.getCategoryId() != null) {
                var category = categoryMapper.selectById(device.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }

            // 加载设备实例，如果没有实例则自动创建一个默认实例
            List<DeviceInstance> instances = deviceInstanceMapper.selectWithBorrowStatus(device.getId());
            if (instances.isEmpty()) {
                // 自动创建一个默认实例
                ensureDeviceHasInstance(device);
                instances = deviceInstanceMapper.selectWithBorrowStatus(device.getId());
            }
            List<DeviceInstanceVO> instanceVOs = instances.stream().map(inst -> {
                DeviceInstanceVO instanceVO = new DeviceInstanceVO();
                BeanUtils.copyProperties(inst, instanceVO);
                // 检查是否已借出
                LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
                borrowWrapper.eq(BorrowRecord::getDeviceInstanceId, inst.getId())
                        .in(BorrowRecord::getStatus, BorrowStatus.PENDING.getCode(), BorrowStatus.APPROVED.getCode());
                Long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);
                instanceVO.setBorrowed(borrowCount > 0);
                return instanceVO;
            }).collect(Collectors.toList());
            vo.setInstances(instanceVOs);

            // 计算设备整体状态
            vo.setOverallStatus(calculateDeviceStatus(instanceVOs));

            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(total, page, size, voList);
    }

    @Override
    public Device getDeviceById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
        return device;
    }

    /**
     * 根据ID获取设备详情（包含设备实例）
     *
     * @param id 设备ID
     * @return 设备信息（包含设备实例）
     */
    public DeviceVO getDeviceByIdWithInstances(Long id) {
        Device device = getDeviceById(id);
        DeviceVO vo = new DeviceVO();
        BeanUtils.copyProperties(device, vo);

        // 加载分类名称
        if (device.getCategoryId() != null) {
            var category = categoryMapper.selectById(device.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 加载设备实例，如果没有实例则自动创建一个默认实例
        List<DeviceInstance> instances = deviceInstanceMapper.selectWithBorrowStatus(id);
        if (instances.isEmpty()) {
            // 自动创建一个默认实例
            ensureDeviceHasInstance(device);
            instances = deviceInstanceMapper.selectWithBorrowStatus(id);
        }
        List<DeviceInstanceVO> instanceVOs = instances.stream().map(inst -> {
            DeviceInstanceVO instanceVO = new DeviceInstanceVO();
            BeanUtils.copyProperties(inst, instanceVO);
            // 检查是否已借出
            LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BorrowRecord::getDeviceInstanceId, inst.getId())
                    .in(BorrowRecord::getStatus, BorrowStatus.PENDING.getCode(), BorrowStatus.APPROVED.getCode());
            Long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);
            instanceVO.setBorrowed(borrowCount > 0);
            return instanceVO;
        }).collect(Collectors.toList());
        vo.setInstances(instanceVOs);

        // 计算设备整体状态
        vo.setOverallStatus(calculateDeviceStatus(instanceVOs));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device createDevice(Device device) {
        // 验证分类是否存在
        if (categoryMapper.selectById(device.getCategoryId()) == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "分类不存在");
        }

        // 验证状态
        if (device.getStatus() != null && DeviceStatus.fromCode(device.getStatus()) == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备状态无效");
        }

        // 设置默认值
        if (device.getStatus() == null) {
            device.setStatus(DeviceStatus.NORMAL.getCode());
        }

        deviceMapper.insert(device);
        logger.info("创建设备成功，设备ID：{}，设备名称：{}", device.getId(), device.getName());
        
        // 自动创建一个默认设备实例
        DeviceInstance defaultInstance = new DeviceInstance();
        defaultInstance.setDeviceId(device.getId());
        defaultInstance.setInstanceName(device.getName() + "-001");
        defaultInstance.setLocation("默认位置");
        defaultInstance.setStatus(DeviceStatus.NORMAL.getCode());
        defaultInstance.setDescription("自动创建的默认实例");
        deviceInstanceService.createInstance(defaultInstance);
        logger.info("自动创建设备实例成功，设备ID：{}，实例名称：{}", device.getId(), defaultInstance.getInstanceName());
        
        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(Long id, Device device) {
        Device existDevice = getDeviceById(id);

        if (device.getCategoryId() != null) {
            if (categoryMapper.selectById(device.getCategoryId()) == null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "分类不存在");
            }
            existDevice.setCategoryId(device.getCategoryId());
        }
        if (device.getName() != null) {
            existDevice.setName(device.getName());
        }
        if (device.getModel() != null) {
            existDevice.setModel(device.getModel());
        }
        if (device.getDescription() != null) {
            existDevice.setDescription(device.getDescription());
        }

        deviceMapper.updateById(existDevice);
        logger.info("更新设备成功，设备ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceStatus(Long id, String status) {
        Device device = getDeviceById(id);

        if (DeviceStatus.fromCode(status) == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备状态无效");
        }

        device.setStatus(status);
        deviceMapper.updateById(device);
        logger.info("更新设备状态成功，设备ID：{}，新状态：{}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {
        Device device = getDeviceById(id);

        // 删除所有与该设备相关的借用记录（包括所有状态的记录）
        LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
        borrowWrapper.eq(BorrowRecord::getDeviceId, id);
        Long deletedBorrowCount = borrowRecordMapper.selectCount(borrowWrapper);
        if (deletedBorrowCount > 0) {
            borrowRecordMapper.delete(borrowWrapper);
            logger.info("删除设备相关的借用记录，设备ID：{}，删除记录数：{}", id, deletedBorrowCount);
        }

        // 删除设备（设备实例会通过数据库外键级联删除）
        deviceMapper.deleteById(id);
        logger.info("删除设备成功，设备ID：{}，设备名称：{}", id, device.getName());
    }

    /**
     * 确保设备有至少一个实例，如果没有则自动创建
     *
     * @param device 设备信息
     */
    private void ensureDeviceHasInstance(Device device) {
        try {
            DeviceInstance defaultInstance = new DeviceInstance();
            defaultInstance.setDeviceId(device.getId());
            defaultInstance.setInstanceName(device.getName() + "-001");
            defaultInstance.setLocation("默认位置");
            defaultInstance.setStatus(DeviceStatus.NORMAL.getCode());
            defaultInstance.setDescription("自动创建的默认实例");
            deviceInstanceService.createInstance(defaultInstance);
            logger.info("自动创建设备实例成功，设备ID：{}，实例名称：{}", device.getId(), defaultInstance.getInstanceName());
        } catch (Exception e) {
            logger.warn("自动创建设备实例失败，设备ID：{}，错误：{}", device.getId(), e.getMessage());
            // 如果创建失败（例如实例名称重复），不抛出异常，让调用方处理
        }
    }

    /**
     * 计算设备整体状态
     * 规则：
     * - 已报废：所有实例都已报废
     * - 借用中：有实例被借用
     * - 维修中：有实例在维修中（且没有借用中的）
     * - 正常：其他情况（所有实例正常且未被借用）
     *
     * @param instances 设备实例列表
     * @return 设备整体状态
     */
    private String calculateDeviceStatus(List<DeviceInstanceVO> instances) {
        if (instances == null || instances.isEmpty()) {
            return DeviceStatus.NORMAL.getCode(); // 没有实例时默认为正常
        }

        boolean hasBorrowed = false;
        boolean hasMaintenance = false;
        boolean allScrapped = true;

        for (DeviceInstanceVO inst : instances) {
            if (inst.getBorrowed() != null && inst.getBorrowed()) {
                hasBorrowed = true;
            }
            if (DeviceStatus.MAINTENANCE.getCode().equals(inst.getStatus())) {
                hasMaintenance = true;
            }
            if (!DeviceStatus.SCRAPPED.getCode().equals(inst.getStatus())) {
                allScrapped = false;
            }
        }

        // 优先级：已报废 > 借用中 > 维修中 > 正常
        if (allScrapped) {
            return DeviceStatus.SCRAPPED.getCode();
        }
        if (hasBorrowed) {
            return "BORROWED"; // 借用中
        }
        if (hasMaintenance) {
            return DeviceStatus.MAINTENANCE.getCode();
        }
        return DeviceStatus.NORMAL.getCode();
    }
}

