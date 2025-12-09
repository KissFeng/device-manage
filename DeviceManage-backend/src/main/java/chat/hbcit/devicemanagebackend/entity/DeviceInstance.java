package chat.hbcit.devicemanagebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实例实体类
 *
 * @author DeviceManage
 */
@Data
@TableName("device_instances")
public class DeviceInstance {
    /**
     * 设备实例ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID（关联设备模板）
     */
    private Long deviceId;

    /**
     * 设备实例名称（如：尼康1、尼康2）
     */
    private String instanceName;

    /**
     * 存放位置
     */
    private String location;

    /**
     * 状态：NORMAL-正常，MAINTENANCE-维修中，SCRAPPED-已报废
     */
    private String status;

    /**
     * 设备实例描述
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

