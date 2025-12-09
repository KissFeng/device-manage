package chat.hbcit.devicemanagebackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页结果DTO
 *
 * @param <T> 数据类型
 * @author DeviceManage
 */
@Data
public class PageResult<T> {
    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 数据列表
     */
    private List<T> list;

    public PageResult() {
    }

    public PageResult(Long total, Integer page, Integer size, List<T> list) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.list = list;
    }
}

