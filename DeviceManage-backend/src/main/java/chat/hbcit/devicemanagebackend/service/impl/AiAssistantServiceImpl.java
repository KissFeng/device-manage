package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.ai.AgentStepEvent;
import chat.hbcit.devicemanagebackend.ai.tools.AdminInsightTools;
import chat.hbcit.devicemanagebackend.ai.tools.UserDeviceTools;
import chat.hbcit.devicemanagebackend.dto.AiChatRequest;
import chat.hbcit.devicemanagebackend.enums.UserRole;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceInstanceMapper;
import chat.hbcit.devicemanagebackend.scheduler.BorrowOverdueScheduler;
import chat.hbcit.devicemanagebackend.service.AiAssistantService;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.service.DeviceInstanceService;
import chat.hbcit.devicemanagebackend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 基于 Spring AI ChatClient 与 DeepSeek 官方 API 的智能体服务实现
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AiAssistantServiceImpl.class);
    private static final String PLACEHOLDER_KEY = "sk-placeholder-configure-env";

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DeviceServiceImpl deviceService;

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private UserService userService;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private DeviceInstanceService deviceInstanceService;

    @Autowired
    private DeviceInstanceMapper deviceInstanceMapper;

    @Autowired(required = false)
    private BorrowOverdueScheduler overdueScheduler;

    @Autowired
    public AiAssistantServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 供单元测试构造使用
    AiAssistantServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public List<String> getSuggestions(String role) {
        if (isAdminRole(role)) {
            return List.of(
                    "目前全库设备总数、待审批和逾期情况如何？",
                    "帮我列出当前所有管理员和普通用户列表",
                    "帮我列出当前所有待审批的借用申请",
                    "目前有哪些设备借用已经逾期未归还？"
            );
        }
        return List.of(
                "现在有哪些相机或镜头可以借用？",
                "平台目前有哪些设备分类？",
                "帮我查一下我当前的借用申请和审批状态",
                "我想借用灯光或稳定器，目前还有库存吗？"
        );
    }

    @Override
    public SseEmitter streamChat(Long userId, String role, AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (apiKey == null || apiKey.isBlank() || PLACEHOLDER_KEY.equals(apiKey)) {
                    sendEvent(emitter, "step", new AgentStepEvent(
                            "env-check", "error", "alert",
                            "未检测到有效的 DEEPSEEK_API_KEY 环境变量",
                            "请设置环境变量 DEEPSEEK_API_KEY 后重启后端服务", 0L));
                    sendEvent(emitter, "token", Map.of("text",
                            "⚠️ 尚未配置 DeepSeek 官方 API Key。\n\n请在启动后端前设置环境变量：\n`export DEEPSEEK_API_KEY=你的真实Key`\n\n配置完成后重启服务，即可通过 Spring AI 实时调用数据库工具并回答您的咨询。"));
                    sendEvent(emitter, "done", Map.of("status", "ok"));
                    emitter.complete();
                    return;
                }

                Consumer<AgentStepEvent> stepEmitter = step -> sendEvent(emitter, "step", step);
                Object[] tools = buildToolsForRole(userId, role, stepEmitter);
                String systemPrompt = buildSystemPrompt(role);
                List<Message> historyMessages = convertHistory(request.getHistory());

                chatClient.prompt()
                        .system(systemPrompt)
                        .messages(historyMessages)
                        .user(request.getMessage())
                        .tools(tools)
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            if (chunk != null && !chunk.isEmpty()) {
                                sendEvent(emitter, "token", Map.of("text", chunk));
                            }
                        })
                        .blockLast();

                sendEvent(emitter, "done", Map.of("status", "ok"));
                emitter.complete();
            } catch (Exception ex) {
                logger.error("智能体执行异常", ex);
                sendEvent(emitter, "error", Map.of("message", "智能体处理异常：" + ex.getMessage()));
                emitter.complete();
            }
        });

        return emitter;
    }

    Object[] buildToolsForRole(Long userId, String role, Consumer<AgentStepEvent> stepEmitter) {
        UserDeviceTools userTools = new UserDeviceTools(userId, categoryService, deviceService, borrowService, stepEmitter);
        if (isAdminRole(role)) {
            AdminInsightTools adminTools = new AdminInsightTools(
                    userId, deviceService, categoryService, userService, borrowService,
                    deviceInstanceService, borrowRecordMapper, deviceInstanceMapper,
                    overdueScheduler, stepEmitter);
            return new Object[]{userTools, adminTools};
        }
        return new Object[]{userTools};
    }

    boolean isAdminRole(String role) {
        return UserRole.ADMIN.getCode().equalsIgnoreCase(role)
                || UserRole.ROOT.getCode().equalsIgnoreCase(role);
    }

    private String buildSystemPrompt(String role) {
        String today = LocalDate.now().toString();
        if (isAdminRole(role)) {
            return """
                    你是“光影资产（DeviceManage）”平台的管理员运营分析与运维智能助手。今天日期是 %s。
                    你的职责是帮助管理员查询资产统计、用户与管理员列表、待审批申请、逾期借用风险，并协助执行借用审批、归还核验与物理实例状态调整。
                    规则：
                    1. 回答必须基于工具查询到的真实数据，严禁编造数字、用户或设备名称；查询用户列表时注意仅展示角色、账号等非敏感信息。
                    2. 【Human-in-the-Loop 安全确认】对于审批通过/拒绝（approveBorrowApplication）、确认归还（confirmDeviceReturn）、修改物理实例状态（updateDeviceInstanceStatus）等写操作：
                       - 先查询并向管理员复述即将操作的对象（如记录ID、申请人、设备、目标状态或动作），请管理员回复确认；
                       - 待管理员明确确认（或指令中已带“直接确认执行”等明确授权且参数完整）后，再调用写工具落库并返回结果。
                    3. 使用清晰的中文 Markdown（要点列表或简洁表格）汇总呈现结果。
                    """.formatted(today);
        }
        return """
                你是“光影资产（DeviceManage）”平台的器材借用顾问智能助手。今天日期是 %s。
                你的职责是帮助普通用户了解当前可借用器材与库存数量、查询个人借用审批进度，并协助用户提交借用申请。
                规则：
                1. 回答必须基于工具查询到的真实数据，严禁编造设备型号或可用数量。
                2. 【Human-in-the-Loop 安全确认】当用户希望借用某台器材（submitBorrowApplication）时：
                   - 先通过 searchAvailableDevices 确认设备ID与可用库存，并向用户复述“设备名称、预计归还日期（YYYY-MM-DD）、借用目的”，请用户确认；
                   - 待用户回复确认（或用户已在同一句中明确给出完整日期、用途并要求直接提交）后，再调用 submitBorrowApplication 提交申请并返回单号。
                3. 使用清晰的中文 Markdown 列表/表格推荐器材与注意事项。
                """.formatted(today);
    }

    private List<Message> convertHistory(List<AiChatRequest.ChatMessageItem> history) {
        List<Message> messages = new ArrayList<>();
        if (history == null) {
            return messages;
        }
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            AiChatRequest.ChatMessageItem item = history.get(i);
            if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(item.getRole())) {
                messages.add(new AssistantMessage(item.getContent()));
            } else {
                messages.add(new UserMessage(item.getContent()));
            }
        }
        return messages;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException ignored) {
            // 客户端断开连接时忽略
        }
    }
}
