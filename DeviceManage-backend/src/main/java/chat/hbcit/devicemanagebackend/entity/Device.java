package chat.hbcit.devicemanagebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实体类
 *
 * @author DeviceManage
 */
@Data
@TableName("devices")
public class Device {
    /**
     * 设备ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 状态：NORMAL-正常，MAINTENANCE-维修中，SCRAPPED-已报废
     * 注意：此状态为设备模板的默认状态，实际状态由设备实例决定
     */
    private String status;

    /**
     * 设备描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

