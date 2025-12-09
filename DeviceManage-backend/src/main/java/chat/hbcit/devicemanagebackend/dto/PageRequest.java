package chat.hbcit.devicemanagebackend.dto;

import lombok.Data;

/**
 * 分页请求DTO
 *
 * @author DeviceManage
 */
@Data
public class PageRequest {
    /**
     * 页码，默认1
     */
    private Integer page = 1;

    /**
     * 每页数量，默认10
     */
    private Integer size = 10;

    /**
     * 获取偏移量
     *
     * @return 偏移量
     */
    public Integer getOffset() {
        return (page - 1) * size;
    }
}

