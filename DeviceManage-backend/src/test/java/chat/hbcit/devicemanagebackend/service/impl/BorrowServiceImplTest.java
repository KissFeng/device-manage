package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    @InjectMocks
    private BorrowServiceImpl borrowService;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceInstanceMapper deviceInstanceMapper;

    @Test
    void applyBorrowShouldThrowWhenDeviceIdMissing() {
        BorrowRecord record = new BorrowRecord();
        record.setExpectedReturnDate(LocalDate.now().plusDays(1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.applyBorrow(1L, record));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("设备ID不能为空", exception.getMessage());
        verifyNoInteractions(deviceMapper, deviceInstanceMapper, borrowRecordMapper);
    }

    @Test
    void applyBorrowShouldThrowWhenDeviceNotFound() {
        BorrowRecord record = new BorrowRecord();
        record.setDeviceId(100L);
        record.setExpectedReturnDate(LocalDate.now().plusDays(2));
        when(deviceMapper.selectById(100L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.applyBorrow(1L, record));

        assertEquals(ResponseCode.NOT_FOUND, exception.getCode());
        assertEquals("设备不存在", exception.getMessage());
        verify(deviceMapper).selectById(100L);
        verifyNoInteractions(deviceInstanceMapper);
        verify(borrowRecordMapper, never()).insert(any(BorrowRecord.class));
    }

    @Test
    void applyBorrowShouldThrowWhenExpectedReturnDateInvalid() {
        BorrowRecord record = new BorrowRecord();
        record.setDeviceId(200L);
        record.setExpectedReturnDate(LocalDate.now().minusDays(1));
        Device device = new Device();
        device.setId(200L);
        when(deviceMapper.selectById(200L)).thenReturn(device);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.applyBorrow(2L, record));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("预计归还日期无效", exception.getMessage());
        verifyNoInteractions(deviceInstanceMapper);
        verify(borrowRecordMapper, never()).insert(any(BorrowRecord.class));
    }

    @Test
    void applyBorrowShouldThrowWhenNoDeviceInstanceAvailable() {
        BorrowRecord record = new BorrowRecord();
        record.setDeviceId(300L);
        record.setExpectedReturnDate(LocalDate.now().plusDays(3));
        Device device = new Device();
        device.setId(300L);
        when(deviceMapper.selectById(300L)).thenReturn(device);

        DeviceInstance busyInstance = new DeviceInstance();
        busyInstance.setId(10L);
        busyInstance.setStatus(DeviceStatus.NORMAL.getCode());
        when(deviceInstanceMapper.selectWithBorrowStatus(300L)).thenReturn(List.of(busyInstance));
        when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.applyBorrow(3L, record));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("该设备暂无可用实例", exception.getMessage());
        verify(borrowRecordMapper, never()).insert(any(BorrowRecord.class));
    }

    @Test
    void applyBorrowShouldSucceedWhenInstanceAvailable() {
        BorrowRecord record = new BorrowRecord();
        record.setDeviceId(400L);
        record.setExpectedReturnDate(LocalDate.now().plusDays(5));
        record.setPurpose("拍摄活动");

        Device device = new Device();
        device.setId(400L);
        when(deviceMapper.selectById(400L)).thenReturn(device);

        DeviceInstance availableInstance = new DeviceInstance();
        availableInstance.setId(20L);
        availableInstance.setDeviceId(400L);
        availableInstance.setStatus(DeviceStatus.NORMAL.getCode());
        when(deviceInstanceMapper.selectWithBorrowStatus(400L)).thenReturn(List.of(availableInstance));
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        BorrowRecord result = borrowService.applyBorrow(5L, record);

        ArgumentCaptor<BorrowRecord> captor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrowRecordMapper).insert(captor.capture());
        BorrowRecord savedRecord = captor.getValue();

        assertEquals(availableInstance.getId(), result.getDeviceInstanceId());
        assertEquals(availableInstance.getId(), savedRecord.getDeviceInstanceId());
        assertEquals(1, result.getQuantity());
        assertEquals(1, savedRecord.getQuantity());
        assertEquals(5L, result.getUserId());
        assertEquals(5L, savedRecord.getUserId());
        assertEquals(BorrowStatus.PENDING.getCode(), result.getStatus());
        assertEquals(BorrowStatus.PENDING.getCode(), savedRecord.getStatus());
    }

    @Test
    void approveBorrowShouldThrowWhenRecordNotFound() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.approveBorrow(1L, 9L, "APPROVE", "同意"));

        assertEquals(ResponseCode.NOT_FOUND, exception.getCode());
        assertEquals("借用记录不存在", exception.getMessage());
    }

    @Test
    void approveBorrowShouldThrowWhenStatusNotPending() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.APPROVED.getCode());
        when(borrowRecordMapper.selectById(2L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.approveBorrow(2L, 9L, "APPROVE", "重复"));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("该申请已处理，无法重复审批", exception.getMessage());
    }

    @Test
    void approveBorrowShouldThrowWhenDeviceInstanceNotFound() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.PENDING.getCode());
        record.setDeviceInstanceId(33L);
        when(borrowRecordMapper.selectById(3L)).thenReturn(record);
        when(deviceInstanceMapper.selectById(33L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.approveBorrow(3L, 10L, "APPROVE", "验证"));

        assertEquals(ResponseCode.NOT_FOUND, exception.getCode());
        assertEquals("设备实例不存在", exception.getMessage());
    }

    @Test
    void approveBorrowShouldThrowWhenDeviceInstanceUnavailable() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.PENDING.getCode());
        record.setDeviceInstanceId(44L);
        when(borrowRecordMapper.selectById(4L)).thenReturn(record);

        DeviceInstance instance = new DeviceInstance();
        instance.setId(44L);
        instance.setStatus(DeviceStatus.MAINTENANCE.getCode());
        when(deviceInstanceMapper.selectById(44L)).thenReturn(instance);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.approveBorrow(4L, 11L, "APPROVE", "维护"));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("设备实例不可借用", exception.getMessage());
    }

    @Test
    void approveBorrowShouldThrowWhenDeviceInstanceAlreadyBorrowed() {
        BorrowRecord record = new BorrowRecord();
        record.setId(5L);
        record.setStatus(BorrowStatus.PENDING.getCode());
        record.setDeviceInstanceId(55L);
        when(borrowRecordMapper.selectById(5L)).thenReturn(record);

        DeviceInstance instance = new DeviceInstance();
        instance.setId(55L);
        instance.setStatus(DeviceStatus.NORMAL.getCode());
        when(deviceInstanceMapper.selectById(55L)).thenReturn(instance);
        when(borrowRecordMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.approveBorrow(5L, 12L, "APPROVE", "冲突"));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("该设备实例已被其他申请借用", exception.getMessage());
    }

    @Test
    void approveBorrowShouldApproveSuccessfully() {
        BorrowRecord record = new BorrowRecord();
        record.setId(6L);
        record.setStatus(BorrowStatus.PENDING.getCode());
        record.setDeviceInstanceId(66L);
        when(borrowRecordMapper.selectById(6L)).thenReturn(record);

        DeviceInstance instance = new DeviceInstance();
        instance.setId(66L);
        instance.setStatus(DeviceStatus.NORMAL.getCode());
        when(deviceInstanceMapper.selectById(66L)).thenReturn(instance);
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        borrowService.approveBorrow(6L, 13L, "APPROVE", "通过审批");

        assertEquals(BorrowStatus.APPROVED.getCode(), record.getStatus());
        assertEquals(13L, record.getApproveUserId());
        assertEquals("通过审批", record.getApproveRemark());
        assertNotNull(record.getApproveTime());
        verify(borrowRecordMapper).updateById(record);
    }

    @Test
    void approveBorrowShouldRejectSuccessfully() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.PENDING.getCode());
        when(borrowRecordMapper.selectById(7L)).thenReturn(record);

        borrowService.approveBorrow(7L, 14L, "REJECT", "资料不足");

        assertEquals(BorrowStatus.REJECTED.getCode(), record.getStatus());
        assertEquals(14L, record.getApproveUserId());
        assertEquals("资料不足", record.getApproveRemark());
        assertNotNull(record.getApproveTime());
        verify(borrowRecordMapper).updateById(record);
    }

    @Test
    void approveBorrowShouldThrowWhenActionInvalid() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.PENDING.getCode());
        when(borrowRecordMapper.selectById(8L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.approveBorrow(8L, 15L, "UNKNOWN", "无效指令"));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("操作类型无效", exception.getMessage());
    }

    @Test
    void confirmReturnShouldThrowWhenRecordNotFound() {
        when(borrowRecordMapper.selectById(9L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.confirmReturn(9L, "确认"));

        assertEquals(ResponseCode.NOT_FOUND, exception.getCode());
        assertEquals("借用记录不存在", exception.getMessage());
    }

    @Test
    void confirmReturnShouldThrowWhenStatusNotApproved() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.PENDING.getCode());
        when(borrowRecordMapper.selectById(10L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> borrowService.confirmReturn(10L, "状态错误"));

        assertEquals(ResponseCode.BAD_REQUEST, exception.getCode());
        assertEquals("该记录不是已批准状态，无法确认归还", exception.getMessage());
    }

    @Test
    void confirmReturnShouldUpdateRecordSuccessfully() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus(BorrowStatus.APPROVED.getCode());
        when(borrowRecordMapper.selectById(11L)).thenReturn(record);

        LocalDate today = LocalDate.now();
        borrowService.confirmReturn(11L, "完好归还");

        assertEquals(BorrowStatus.RETURNED.getCode(), record.getStatus());
        assertEquals(today, record.getActualReturnDate());
        assertEquals("完好归还", record.getReturnRemark());
        verify(borrowRecordMapper).updateById(record);
    }
}
