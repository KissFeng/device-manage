package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 借用服务实现类
 *
 * @author DeviceManage
 */
@Service
public class BorrowServiceImpl implements BorrowService {
    private static final Logger logger = LoggerFactory.getLogger(BorrowServiceImpl.class);

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceInstanceMapper deviceInstanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord applyBorrow(Long userId, BorrowRecord record) {
        // 验证设备是否存在
        if (record.getDeviceId() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备ID不能为空");
        }
        
        Device device = deviceMapper.selectById(record.getDeviceId());
        if (device == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在");
        }

        // 验证预计归还日期
        if (record.getExpectedReturnDate() == null || record.getExpectedReturnDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "预计归还日期无效");
        }

        // 自动选择一个可用的设备实例
        List<DeviceInstance> instances = deviceInstanceMapper.selectWithBorrowStatus(record.getDeviceId());
        DeviceInstance availableInstance = null;
        
        for (DeviceInstance instance : instances) {
            // 检查实例状态是否为正常
            if (!DeviceStatus.NORMAL.getCode().equals(instance.getStatus())) {
                continue;
            }
            
            // 检查实例是否已被借用
            LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BorrowRecord::getDeviceInstanceId, instance.getId())
                    .in(BorrowRecord::getStatus, BorrowStatus.PENDING.getCode(), BorrowStatus.APPROVED.getCode());
            Long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);
            
            if (borrowCount == 0) {
                availableInstance = instance;
                break; // 找到第一个可用实例就停止
            }
        }
        
        if (availableInstance == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该设备暂无可用实例");
        }

        // 验证设备实例ID
        if (availableInstance.getId() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备实例ID不能为空");
        }

        // 设置设备实例ID
        record.setDeviceInstanceId(availableInstance.getId());
        record.setQuantity(1); // 设备实例模式，数量固定为1

        // 再次验证设备实例ID已正确设置
        if (record.getDeviceInstanceId() == null) {
            logger.error("设备实例ID设置失败，设备ID：{}，可用实例：{}", record.getDeviceId(), availableInstance);
            throw new BusinessException(ResponseCode.BAD_REQUEST, "设备实例ID不能为空");
        }

        record.setUserId(userId);
        record.setStatus(BorrowStatus.PENDING.getCode());
        borrowRecordMapper.insert(record);

        logger.info("提交借用申请成功，记录ID：{}，用户ID：{}，设备ID：{}，设备实例ID：{}", 
                record.getId(), userId, record.getDeviceId(), record.getDeviceInstanceId());
        return record;
    }

    @Override
    public PageResult<chat.hbcit.devicemanagebackend.vo.BorrowRecordVO> getMyRecords(Long userId, String status, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<chat.hbcit.devicemanagebackend.vo.BorrowRecordVO> list = borrowRecordMapper.selectMyRecords(userId, status, offset, size);
        Long total = borrowRecordMapper.countMyRecords(userId, status);
        return new PageResult<>(total, page, size, list);
    }

    @Override
    public PageResult<chat.hbcit.devicemanagebackend.vo.BorrowRecordVO> getPendingRecords(String deviceName, String username, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<chat.hbcit.devicemanagebackend.vo.BorrowRecordVO> list = borrowRecordMapper.selectPendingRecords(deviceName, username, offset, size);
        Long total = borrowRecordMapper.countPendingRecords(deviceName, username);
        return new PageResult<>(total, page, size, list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBorrow(Long recordId, Long approveUserId, String action, String remark) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "借用记录不存在");
        }

        if (!BorrowStatus.PENDING.getCode().equals(record.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该申请已处理，无法重复审批");
        }

        if ("APPROVE".equals(action)) {
            // 批准：检查设备实例是否可用
            if (record.getDeviceInstanceId() == null) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "设备实例ID不能为空");
            }

            DeviceInstance instance = deviceInstanceMapper.selectById(record.getDeviceInstanceId());
            if (instance == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "设备实例不存在");
            }

            if (!DeviceStatus.NORMAL.getCode().equals(instance.getStatus())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "设备实例不可借用");
            }

            // 检查设备实例是否已被其他记录借用
            LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(BorrowRecord::getDeviceInstanceId, record.getDeviceInstanceId())
                    .in(BorrowRecord::getStatus, BorrowStatus.PENDING.getCode(), BorrowStatus.APPROVED.getCode())
                    .ne(BorrowRecord::getId, recordId);
            Long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);
            if (borrowCount > 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "该设备实例已被其他申请借用");
            }

            // 更新借用记录状态
            record.setStatus(BorrowStatus.APPROVED.getCode());
            record.setApproveUserId(approveUserId);
            record.setApproveTime(LocalDateTime.now());
            record.setApproveRemark(remark);
            borrowRecordMapper.updateById(record);

            logger.info("审批通过，记录ID：{}，设备实例ID：{}", recordId, record.getDeviceInstanceId());
        } else if ("REJECT".equals(action)) {
            // 拒绝：更新借用记录状态
            record.setStatus(BorrowStatus.REJECTED.getCode());
            record.setApproveUserId(approveUserId);
            record.setApproveTime(LocalDateTime.now());
            record.setApproveRemark(remark);
            borrowRecordMapper.updateById(record);

            logger.info("审批拒绝，记录ID：{}", recordId);
        } else {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "操作类型无效");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(Long recordId, String returnRemark) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "借用记录不存在");
        }

        if (!BorrowStatus.APPROVED.getCode().equals(record.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该记录不是已批准状态，无法确认归还");
        }

        // 更新借用记录状态
        record.setStatus(BorrowStatus.RETURNED.getCode());
        record.setActualReturnDate(LocalDate.now());
        record.setReturnRemark(returnRemark);
        borrowRecordMapper.updateById(record);

        logger.info("确认归还成功，记录ID：{}，设备实例ID：{}", recordId, record.getDeviceInstanceId());
    }

    @Override
    public PageResult<chat.hbcit.devicemanagebackend.vo.BorrowRecordVO> getAllRecords(String status, String deviceName, String username, Integer page, Integer size) {
        Integer offset = (page - 1) * size;
        List<chat.hbcit.devicemanagebackend.vo.BorrowRecordVO> list = borrowRecordMapper.selectAllRecords(status, deviceName, username, offset, size);
        Long total = borrowRecordMapper.countAllRecords(status, deviceName, username);
        return new PageResult<>(total, page, size, list);
    }
}

