package chat.hbcit.devicemanagebackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 借用申请请求DTO
 *
 * @author DeviceManage
 */
@Data
public class BorrowApplyRequest {
    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /**
     * 借用目的
     */
    @NotBlank(message = "借用目的不能为空")
    private String purpose;

    /**
     * 预计归还日期
     */
    @NotNull(message = "预计归还日期不能为空")
    private LocalDate expectedReturnDate;
}

