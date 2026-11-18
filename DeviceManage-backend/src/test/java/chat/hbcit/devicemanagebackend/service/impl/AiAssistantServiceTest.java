package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.ai.AgentStepEvent;
import chat.hbcit.devicemanagebackend.ai.tools.AdminInsightTools;
import chat.hbcit.devicemanagebackend.ai.tools.UserDeviceTools;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.enums.DeviceStatus;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.scheduler.BorrowOverdueScheduler;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.service.DeviceService;
import chat.hbcit.devicemanagebackend.service.UserService;
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
    private BorrowRecordMapper borrowRecordMapper;

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
        assertThat(adminSug.get(1)).contains("待审批");
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
    void userDeviceToolsShouldQueryCategoriesAndAvailableDevicesWithStepEvents() {
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

        List<Map<String, Object>> cats = tools.listDeviceCategories();
        List<Map<String, Object>> devs = tools.searchAvailableDevices("Sony", null);

        assertThat(cats).hasSize(1);
        assertThat(devs).hasSize(1);
        assertThat(devs.get(0).get("availableCount")).isEqualTo(1L);
        assertThat(events).extracting(AgentStepEvent::getStatus)
                .containsExactly("running", "done", "running", "done");
    }

    @Test
    void adminInsightToolsShouldScanOverdueRecords() {
        List<AgentStepEvent> events = new ArrayList<>();
        AdminInsightTools tools = new AdminInsightTools(
                deviceService, categoryService, userService, borrowService,
                borrowRecordMapper, overdueScheduler, events::add);

        BorrowRecord overdue = new BorrowRecord();
        overdue.setId(9L);
        overdue.setExpectedReturnDate(LocalDate.now().minusDays(2));
        when(borrowRecordMapper.selectList(any())).thenReturn(List.of(overdue));

        List<Map<String, Object>> list = tools.listOverdueBorrowRecords();

        assertThat(list).hasSize(1);
        assertThat(events).hasSize(2);
        assertThat(events.get(1).getTitle()).contains("1 条逾期记录");
    }
}
