package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备分类控制器（用户端）
 *
 * @author DeviceManage
 */
@Tag(name = "用户端-设备分类", description = "查询媒体设备分类列表（优先命中 Redis 缓存）")
@RestController
@RequestMapping("/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类列表（用户端可访问）
     *
     * @return 分类列表
     */
    @Operation(summary = "获取全部分类列表", description = "已认证用户查询设备分类，底层通过 Redis 缓存加速并在异常时自动降级回源 MySQL")
    @GetMapping
    public Result<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success("获取成功", categories);
    }
}

