package chat.hbcit.devicemanagebackend.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备视图对象（包含分类名称和设备实例）
 *
 * @author DeviceManage
 */
@Data
public class DeviceVO {
    /**
     * 设备ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 状态（设备模板的默认状态，已废弃）
     */
    private String status;

    /**
     * 设备描述
     */
    private String description;

    /**
     * 设备实例列表
     */
    private List<DeviceInstanceVO> instances;

    /**
     * 设备整体状态（根据实例状态计算）
     * NORMAL-正常，BORROWED-借用中，MAINTENANCE-维修中，SCRAPPED-已报废
     */
    private String overallStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

