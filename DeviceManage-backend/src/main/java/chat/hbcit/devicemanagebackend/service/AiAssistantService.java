package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.dto.AiChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Spring AI + DeepSeek 智能体服务接口
 */
public interface AiAssistantService {

    /**
     * 按角色获取快捷提问建议
     *
     * @param role 当前用户角色
     * @return 快捷提问列表
     */
    List<String> getSuggestions(String role);

    /**
     * 发起流式智能体对话（通过 SSE 实时推送工具调用步骤与回答文本）
     *
     * @param userId  当前用户ID
     * @param role    当前用户角色
     * @param request 对话请求
     * @return SseEmitter
     */
    SseEmitter streamChat(Long userId, String role, AiChatRequest request);
}
