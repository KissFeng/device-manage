package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备实例服务实现类
 *
 * @author DeviceManage
 */
@Service
public class DeviceInstanceServiceImpl implements DeviceInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceInstanceServiceImpl.class);

    @Autowired
    private DeviceInstanceMapper deviceInstanceMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    public List<DeviceInstance> getInstancesByDeviceId(Long deviceId) {
        // 验证设备是否存在
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }
        return deviceInstanceMapper.selectByDeviceId(deviceId);
    }

    @Override
    public DeviceInstance getInstanceById(Long id) {
        DeviceInstance instance = deviceInstanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备实例不存在");
        }
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceInstance createInstance(DeviceInstance instance) {
        // 验证设备是否存在
        Device device = deviceMapper.selectById(instance.getDeviceId());
        if (device == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备不存在");
        }

        // 验证状态
        if (instance.getStatus() != null && DeviceStatus.fromCode(instance.getStatus()) == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备实例状态无效");
        }

        // 设置默认值
        if (instance.getStatus() == null) {
            instance.setStatus(DeviceStatus.NORMAL.getCode());
        }

        // 验证实例名称是否重复
        LambdaQueryWrapper<DeviceInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceInstance::getDeviceId, instance.getDeviceId())
                .eq(DeviceInstance::getInstanceName, instance.getInstanceName());
        Long count = deviceInstanceMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该设备已存在相同名称的实例");
        }

        deviceInstanceMapper.insert(instance);
        logger.info("创建设备实例成功，实例ID：{}，实例名称：{}", instance.getId(), instance.getInstanceName());
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstance(Long id, DeviceInstance instance) {
        DeviceInstance existInstance = getInstanceById(id);

        if (instance.getInstanceName() != null) {
            // 验证实例名称是否重复（排除自己）
            LambdaQueryWrapper<DeviceInstance> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceInstance::getDeviceId, existInstance.getDeviceId())
                    .eq(DeviceInstance::getInstanceName, instance.getInstanceName())
                    .ne(DeviceInstance::getId, id);
            Long count = deviceInstanceMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "该设备已存在相同名称的实例");
            }
            existInstance.setInstanceName(instance.getInstanceName());
        }
        if (instance.getLocation() != null) {
            existInstance.setLocation(instance.getLocation());
        }
        if (instance.getStatus() != null) {
            if (DeviceStatus.fromCode(instance.getStatus()) == null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "设备实例状态无效");
            }
            existInstance.setStatus(instance.getStatus());
        }
        if (instance.getDescription() != null) {
            existInstance.setDescription(instance.getDescription());
        }

        deviceInstanceMapper.updateById(existInstance);
        logger.info("更新设备实例成功，实例ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstance(Long id) {
        DeviceInstance instance = getInstanceById(id);

        // 检查是否有未归还的借用记录
        LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
        borrowWrapper.eq(BorrowRecord::getDeviceInstanceId, id)
                .in(BorrowRecord::getStatus, BorrowStatus.PENDING.getCode(), BorrowStatus.APPROVED.getCode());
        Long unreturnedCount = borrowRecordMapper.selectCount(borrowWrapper);
        if (unreturnedCount > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该设备实例存在未归还的借用记录，无法删除");
        }

        deviceInstanceMapper.deleteById(id);
        logger.info("删除设备实例成功，实例ID：{}，实例名称：{}", id, instance.getInstanceName());
    }
}

