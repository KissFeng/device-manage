package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.CategoryMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
import chat.hbcit.devicemanagebackend.vo.DeviceVO;
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
class DeviceServiceImplTest {

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private DeviceInstanceMapper deviceInstanceMapper;

    @Mock
    private DeviceInstanceService deviceInstanceService;

    @Test
    void getDeviceListShouldReturnPagedResult() {
        Device d = new Device();
        d.setId(1L);
        d.setName("Sony A7M4");
        when(deviceMapper.selectDeviceList(null, null, null, null, 0, 10)).thenReturn(List.of(d));
        when(deviceMapper.countDevices(null, null, null, null)).thenReturn(1L);

        PageResult<Device> result = deviceService.getDeviceList(null, null, null, null, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    void getDeviceListWithInstancesShouldLoadCategoryAndInstances() {
        Device d = new Device();
        d.setId(1L);
        d.setCategoryId(2L);
        d.setName("Sony FX3");
        when(deviceMapper.selectDeviceList(null, null, null, null, 0, 10)).thenReturn(List.of(d));
        when(deviceMapper.countDevices(null, null, null, null)).thenReturn(1L);

        Category cat = new Category();
        cat.setId(2L);
        cat.setName("电影机");
        when(categoryMapper.selectById(2L)).thenReturn(cat);

        DeviceInstance inst = new DeviceInstance();
        inst.setId(100L);
        inst.setStatus(DeviceStatus.NORMAL.getCode());
        when(deviceInstanceMapper.selectWithBorrowStatus(1L)).thenReturn(List.of(inst));
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        PageResult<DeviceVO> page = deviceService.getDeviceListWithInstances(null, null, null, null, 1, 10);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getCategoryName()).isEqualTo("电影机");
        assertThat(page.getList().get(0).getOverallStatus()).isEqualTo(DeviceStatus.NORMAL.getCode());
    }

    @Test
    void getDeviceByIdWithInstancesShouldReturnVo() {
        Device d = new Device();
        d.setId(3L);
        d.setCategoryId(1L);
        d.setName("大疆 RS4");
        when(deviceMapper.selectById(3L)).thenReturn(d);

        Category cat = new Category();
        cat.setId(1L);
        cat.setName("稳定器");
        when(categoryMapper.selectById(1L)).thenReturn(cat);

        DeviceInstance inst = new DeviceInstance();
        inst.setId(31L);
        inst.setStatus(DeviceStatus.NORMAL.getCode());
        when(deviceInstanceMapper.selectWithBorrowStatus(3L)).thenReturn(List.of(inst));
        when(borrowRecordMapper.selectCount(any())).thenReturn(0L);

        DeviceVO vo = deviceService.getDeviceByIdWithInstances(3L);

        assertThat(vo.getName()).isEqualTo("大疆 RS4");
        assertThat(vo.getInstances()).hasSize(1);
    }

    @Test
    void createDeviceShouldThrowWhenCategoryMissing() {
        Device d = new Device();
        d.setCategoryId(999L);
        when(categoryMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> deviceService.createDevice(d))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.BAD_REQUEST));
    }

    @Test
    void createDeviceShouldInsertAndCreateDefaultInstance() {
        Device d = new Device();
        d.setCategoryId(1L);
        d.setName("Aputure 600d");
        when(categoryMapper.selectById(1L)).thenReturn(new Category());

        Device created = deviceService.createDevice(d);

        assertThat(created.getStatus()).isEqualTo(DeviceStatus.NORMAL.getCode());
        verify(deviceMapper).insert(d);
        verify(deviceInstanceService).createInstance(any(DeviceInstance.class));
    }

    @Test
    void updateDeviceAndStatusShouldPersistChanges() {
        Device exist = new Device();
        exist.setId(8L);
        exist.setName("Old");
        when(deviceMapper.selectById(8L)).thenReturn(exist);
        when(categoryMapper.selectById(2L)).thenReturn(new Category());

        Device update = new Device();
        update.setCategoryId(2L);
        update.setName("New");
        update.setModel("M2");
        update.setDescription("desc");
        deviceService.updateDevice(8L, update);

        assertThat(exist.getName()).isEqualTo("New");
        verify(deviceMapper).updateById(exist);

        deviceService.updateDeviceStatus(8L, DeviceStatus.MAINTENANCE.getCode());
        assertThat(exist.getStatus()).isEqualTo(DeviceStatus.MAINTENANCE.getCode());
    }

    @Test
    void deleteDeviceShouldThrowWhenNotFound() {
        when(deviceMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> deviceService.deleteDevice(404L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.NOT_FOUND));
    }
}
