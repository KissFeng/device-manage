package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.dto.BorrowApplyRequest;
import chat.hbcit.devicemanagebackend.dto.PageResult;
import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.service.BorrowService;
import chat.hbcit.devicemanagebackend.vo.BorrowRecordVO;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 借用管理控制器
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/borrow")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;

    /**
     * 提交借用申请
     *
     * @param request 借用申请请求
     * @return 创建的借用记录
     */
    @PostMapping("/apply")
    public Result<BorrowRecord> applyBorrow(@Valid @RequestBody BorrowApplyRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(authentication.getName());
        BorrowRecord record = new BorrowRecord();
        BeanUtils.copyProperties(request, record);
        BorrowRecord createdRecord = borrowService.applyBorrow(userId, record);
        return Result.success("申请提交成功", createdRecord);
    }

    /**
     * 获取我的借用记录
     *
     * @param status 状态
     * @param page   页码
     * @param size   每页数量
     * @return 分页结果
     */
    @GetMapping("/my-records")
    public Result<PageResult<BorrowRecordVO>> getMyRecords(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(authentication.getName());
        PageResult<BorrowRecordVO> result = borrowService.getMyRecords(userId, status, page, size);
        return Result.success("获取成功", result);
    }
}

