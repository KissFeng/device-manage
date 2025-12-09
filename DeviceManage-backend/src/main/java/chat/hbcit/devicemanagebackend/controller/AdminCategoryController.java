package chat.hbcit.devicemanagebackend.controller;

import chat.hbcit.devicemanagebackend.common.Result;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员设备分类管理控制器
 *
 * @author DeviceManage
 */
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    @GetMapping
    public Result<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success("获取成功", categories);
    }

    /**
     * 根据ID获取分类详情
     *
     * @param id 分类ID
     * @return 分类信息
     */
    @GetMapping("/{id}")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        return Result.success("获取成功", category);
    }

    /**
     * 创建分类
     *
     * @param category 分类信息
     * @return 创建的分类
     */
    @PostMapping
    public Result<Category> createCategory(@Valid @RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(category);
        return Result.success("创建成功", createdCategory);
    }

    /**
     * 更新分类信息
     *
     * @param id       分类ID
     * @param category 分类信息
     * @return 响应结果
     */
    @PutMapping("/{id}")
    public Result<?> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        categoryService.updateCategory(id, category);
        return Result.success("更新成功");
    }

    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 响应结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}

