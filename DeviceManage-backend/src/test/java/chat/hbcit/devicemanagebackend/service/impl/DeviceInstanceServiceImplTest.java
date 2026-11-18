package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceInstanceServiceImplTest {

    @InjectMocks
    private DeviceInstanceServiceImpl instanceService;

    @Mock
    private DeviceInstanceMapper deviceInstanceMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Test
    void getInstancesByDeviceIdShouldReturnListWhenDeviceExists() {
        when(deviceMapper.selectById(1L)).thenReturn(new Device());
        DeviceInstance inst = new DeviceInstance();
        inst.setId(11L);
        inst.setInstanceName("A7M4-01");
        when(deviceInstanceMapper.selectByDeviceId(1L)).thenReturn(List.of(inst));

        List<DeviceInstance> result = instanceService.getInstancesByDeviceId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstanceName()).isEqualTo("A7M4-01");
    }

    @Test
    void createInstanceShouldThrowWhenDeviceNotFound() {
        DeviceInstance input = new DeviceInstance();
        input.setDeviceId(99L);
        when(deviceMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> instanceService.createInstance(input))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.BAD_REQUEST));
    }

    @Test
    void createInstanceShouldSetDefaultStatusAndInsert() {
        DeviceInstance input = new DeviceInstance();
        input.setDeviceId(1L);
        input.setInstanceName("FX3-01");
        when(deviceMapper.selectById(1L)).thenReturn(new Device());
        when(deviceInstanceMapper.selectCount(any())).thenReturn(0L);

        DeviceInstance created = instanceService.createInstance(input);

        assertThat(created.getStatus()).isEqualTo(DeviceStatus.NORMAL.getCode());
        verify(deviceInstanceMapper).insert(input);
    }

    @Test
    void updateInstanceShouldUpdateFieldsWhenValid() {
        DeviceInstance exist = new DeviceInstance();
        exist.setId(5L);
        exist.setDeviceId(1L);
        exist.setInstanceName("OLD-01");
        when(deviceInstanceMapper.selectById(5L)).thenReturn(exist);
        when(deviceInstanceMapper.selectCount(any())).thenReturn(0L);

        DeviceInstance update = new DeviceInstance();
        update.setInstanceName("NEW-01");
        update.setLocation("摄影棚 A");
        update.setStatus(DeviceStatus.MAINTENANCE.getCode());
        update.setDescription("例行保养");

        instanceService.updateInstance(5L, update);

        assertThat(exist.getInstanceName()).isEqualTo("NEW-01");
        assertThat(exist.getLocation()).isEqualTo("摄影棚 A");
        assertThat(exist.getStatus()).isEqualTo(DeviceStatus.MAINTENANCE.getCode());
        verify(deviceInstanceMapper).updateById(exist);
    }
}
