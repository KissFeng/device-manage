package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.AiChatRequest;
import chat.hbcit.devicemanagebackend.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 光影资产 AI 智能体控制器（基于 Spring AI + DeepSeek）
 */
@Tag(name = "AI 智能体助手", description = "基于 Spring AI 与 DeepSeek 官方 API 的角色感知智能体，支持实时工具调用轨迹推流")
@RestController
@RequestMapping("/ai")
public class AiAssistantController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Operation(summary = "获取当前角色的快捷提问建议", description = "根据用户角色（USER / ADMIN / ROOT）返回对应的引导问题")
    @GetMapping("/suggestions")
    public Result<List<String>> getSuggestions() {
        String role = extractRole(SecurityContextHolder.getContext().getAuthentication());
        return Result.success("获取成功", aiAssistantService.getSuggestions(role));
    }

    @Operation(summary = "流式智能体对话", description = "通过 SSE 实时推送工具调用步骤（step）与 DeepSeek 流式回答（token）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody AiChatRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = parseUserId(auth);
        String role = extractRole(auth);
        return aiAssistantService.streamChat(userId, role, request);
    }

    private Long parseUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return 0L;
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String extractRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return "USER";
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith("ROLE_")) {
                return name.substring(5);
            }
        }
        return "USER";
    }
}
