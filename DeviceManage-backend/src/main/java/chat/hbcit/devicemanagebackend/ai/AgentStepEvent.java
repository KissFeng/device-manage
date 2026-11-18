package chat.hbcit.devicemanagebackend.ai;

import java.util.function.Consumer;

/**
 * 智能体工具执行步骤事件（用于向前端 SSE 推送类 Codex 的实时思考与工具调用轨迹）
 */
public class AgentStepEvent {
    private String id;
    private String status;   // running | done | error
    private String icon;     // search | folder | tool | check | alert
    private String title;    // e.g. 正在查询 可借用设备列表 / 已列出 设备分类（共 5 项）
    private String detail;   // 可选补充参数或结果摘要
    private Long durationMs;

    public AgentStepEvent() {
    }

    public AgentStepEvent(String id, String status, String icon, String title, String detail, Long durationMs) {
        this.id = id;
        this.status = status;
        this.icon = icon;
        this.title = title;
        this.detail = detail;
        this.durationMs = durationMs;
    }

    public static void emit(Consumer<AgentStepEvent> emitter, AgentStepEvent event) {
        if (emitter != null) {
            emitter.accept(event);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
