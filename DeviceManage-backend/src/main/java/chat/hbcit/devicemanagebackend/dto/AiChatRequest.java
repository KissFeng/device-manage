package chat.hbcit.devicemanagebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 智能体对话请求 DTO
 */
@Schema(description = "智能体对话请求")
public class AiChatRequest {

    @NotBlank(message = "问题内容不能为空")
    @Schema(description = "用户当前输入的问题", example = "现在有哪些相机可以借用？")
    private String message;

    @Schema(description = "历史对话消息列表（用于多轮上下文）")
    private List<ChatMessageItem> history;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<ChatMessageItem> getHistory() { return history; }
    public void setHistory(List<ChatMessageItem> history) { this.history = history; }

    public static class ChatMessageItem {
        private String role;    // user | assistant
        private String content;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
