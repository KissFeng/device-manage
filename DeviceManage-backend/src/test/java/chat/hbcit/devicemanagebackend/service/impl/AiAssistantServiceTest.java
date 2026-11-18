package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.ai.AgentStepEvent;
import chat.hbcit.devicemanagebackend.ai.tools.AdminInsightTools;
import chat.hbcit.devicemanagebackend.ai.tools.UserDeviceTools;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.entity.DeviceInstance;
import chat.hbcit.devicemanagebackend.entity.User;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.scheduler.BorrowOverdueScheduler;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
import chat.hbcit.devicemanagebackend.service.DeviceService;
import chat.hbcit.devicemanagebackend.service.UserService;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import chat.hbcit.devicemanagebackend.vo.DeviceInstanceVO;
import chat.hbcit.devicemanagebackend.vo.DeviceVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private CategoryService categoryService;

    @Mock
    private DeviceServiceImpl deviceServiceImpl;

    @Mock
    private DeviceService deviceService;

    @Mock
    private BorrowService borrowService;

    @Mock
    private UserService userService;

    @Mock
    private DeviceInstanceService deviceInstanceService;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private DeviceInstanceMapper deviceInstanceMapper;

    @Mock
    private BorrowOverdueScheduler overdueScheduler;

    @Test
    void getSuggestionsShouldDifferentiateUserAndAdmin() {
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(chatClient);

        List<String> userSug = service.getSuggestions("USER");
        List<String> adminSug = service.getSuggestions("ADMIN");

        assertThat(userSug).hasSize(4);
        assertThat(userSug.get(0)).contains("借用");
        assertThat(adminSug).hasSize(4);
        assertThat(adminSug.get(1)).contains("管理员");
    }

    @Test
    void buildToolsForRoleShouldIsolateAdminToolsFromNormalUser() {
        AiAssistantServiceImpl service = new AiAssistantServiceImpl(chatClient);

        Object[] userTools = service.buildToolsForRole(10L, "USER", step -> {});
        Object[] adminTools = service.buildToolsForRole(1L, "ADMIN", step -> {});

        assertThat(userTools).hasSize(1);
        assertThat(userTools[0]).isInstanceOf(UserDeviceTools.class);

        assertThat(adminTools).hasSize(2);
        assertThat(adminTools[0]).isInstanceOf(UserDeviceTools.class);
        assertThat(adminTools[1]).isInstanceOf(AdminInsightTools.class);
    }

    @Test
    void userDeviceToolsShouldQueryAndSubmitBorrowWithStepEvents() {
        List<AgentStepEvent> events = new ArrayList<>();
        UserDeviceTools tools = new UserDeviceTools(10L, categoryService, deviceServiceImpl, borrowService, events::add);

        Category cat = new Category();
        cat.setId(1L);
        cat.setName("相机机身");
        when(categoryService.getAllCategories()).thenReturn(List.of(cat));

        DeviceInstanceVO freeInst = new DeviceInstanceVO();
        freeInst.setId(101L);
        freeInst.setStatus(DeviceStatus.NORMAL.getCode());
        freeInst.setBorrowed(false);

        DeviceVO dev = new DeviceVO();
        dev.setId(5L);
        dev.setName("Sony A7M4");
        dev.setOverallStatus(DeviceStatus.NORMAL.getCode());
        dev.setInstances(List.of(freeInst));

        when(deviceServiceImpl.getDeviceListWithInstances("Sony", null, null, null, 1, 50))
                .thenReturn(new PageResult<>(1L, 1, 50, List.of(dev)));

        BorrowRecord saved = new BorrowRecord();
        saved.setId(88L);
        saved.setDeviceId(5L);
        saved.setDeviceInstanceId(101L);
        saved.setStatus(BorrowStatus.PENDING.getCode());
        saved.setExpectedReturnDate(LocalDate.now().plusDays(2));
        when(borrowService.applyBorrow(eq(10L), any(BorrowRecord.class))).thenReturn(saved);

        List<Map<String, Object>> cats = tools.listDeviceCategories();
        List<Map<String, Object>> devs = tools.searchAvailableDevices("Sony", null);
        Map<String, Object> applyRes = tools.submitBorrowApplication(
                5L, LocalDate.now().plusDays(2).toString(), "宣传片拍摄");

        assertThat(cats).hasSize(1);
        assertThat(devs).hasSize(1);
        assertThat(applyRes.get("success")).isEqualTo(true);
        assertThat(applyRes.get("recordId")).isEqualTo(88L);
        assertThat(events).extracting(AgentStepEvent::getStatus)
                .containsExactly("running", "done", "running", "done", "running", "done");
    }

    @Test
    void adminInsightToolsShouldSupportUsersQueryApproveReturnAndInstanceStatus() {
        List<AgentStepEvent> events = new ArrayList<>();
        AdminInsightTools tools = new AdminInsightTools(
                1L, deviceService, categoryService, userService, borrowService,
                deviceInstanceService, borrowRecordMapper, deviceInstanceMapper,
                overdueScheduler, events::add);

        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRealName("管理员");
        adminUser.setRole("ADMIN");
        adminUser.setPassword("secret-hash");
        when(userService.getUserList(null, "ADMIN", 1, 50))
                .thenReturn(new PageResult<>(1L, 1, 50, List.of(adminUser)));

        List<Map<String, Object>> admins = tools.listUsers(null, "ADMIN");
        assertThat(admins).hasSize(1);
        assertThat(admins.get(0)).doesNotContainKey("password");
        assertThat(admins.get(0).get("username")).isEqualTo("admin");

        Map<String, Object> approveRes = tools.approveBorrowApplication(12L, "APPROVE", "同意借出");
        assertThat(approveRes.get("success")).isEqualTo(true);
        verify(borrowService).approveBorrow(12L, 1L, "APPROVE", "同意借出");

        Map<String, Object> returnRes = tools.confirmDeviceReturn(12L, "完好");
        assertThat(returnRes.get("success")).isEqualTo(true);
        verify(borrowService).confirmReturn(12L, "完好");

        Map<String, Object> statusRes = tools.updateDeviceInstanceStatus(101L, "MAINTENANCE", "卡口检修");
        assertThat(statusRes.get("success")).isEqualTo(true);
        verify(deviceInstanceService).updateInstance(eq(101L), any(DeviceInstance.class));

        // 覆盖 getSystemOverviewStats / listPendingBorrowApplications / queryBorrowRecords / searchDeviceInstances
        when(deviceService.getDeviceList(null, null, null, null, 1, 1)).thenReturn(new PageResult<>(17L, 1, 1, List.of()));
        when(categoryService.getAllCategories()).thenReturn(List.of(new Category()));
        when(userService.getUserList(null, null, 1, 1)).thenReturn(new PageResult<>(5L, 1, 1, List.of()));
        when(borrowService.getPendingRecords(null, null, 1, 1)).thenReturn(new PageResult<>(1L, 1, 1, List.of()));
        when(borrowService.getAllRecords(BorrowStatus.APPROVED.getCode(), null, null, 1, 1)).thenReturn(new PageResult<>(2L, 1, 1, List.of()));
        Map<String, Object> stats = tools.getSystemOverviewStats();
        assertThat(stats.get("totalDevices")).isEqualTo(17L);

        BorrowRecordVO vo = new BorrowRecordVO();
        vo.setId(20L);
        vo.setDeviceName("Sony A7M4");
        vo.setUsername("user1");
        when(borrowService.getPendingRecords(null, null, 1, 20)).thenReturn(new PageResult<>(1L, 1, 20, List.of(vo)));
        when(borrowService.getAllRecords(null, null, null, 1, 20)).thenReturn(new PageResult<>(1L, 1, 20, List.of(vo)));
        assertThat(tools.listPendingBorrowApplications(null, null)).hasSize(1);
        assertThat(tools.queryBorrowRecords(null, null, null)).hasSize(1);

        DeviceInstance inst = new DeviceInstance();
        inst.setId(101L);
        inst.setInstanceName("A7M4-001");
        when(deviceInstanceMapper.selectList(any())).thenReturn(List.of(inst));
        assertThat(tools.searchDeviceInstances("A7M4")).hasSize(1);
    }

    @Test
    void userDeviceToolsShouldQueryMyBorrowRecords() {
        List<AgentStepEvent> events = new ArrayList<>();
        UserDeviceTools tools = new UserDeviceTools(10L, categoryService, deviceServiceImpl, borrowService, events::add);
        BorrowRecordVO vo = new BorrowRecordVO();
        vo.setId(30L);
        vo.setDeviceName("佳能 EOS R5");
        vo.setStatus("PENDING");
        when(borrowService.getMyRecords(10L, null, 1, 20)).thenReturn(new PageResult<>(1L, 1, 20, List.of(vo)));

        List<Map<String, Object>> records = tools.getMyBorrowRecords(null);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).get("deviceName")).isEqualTo("佳能 EOS R5");
    }
}
