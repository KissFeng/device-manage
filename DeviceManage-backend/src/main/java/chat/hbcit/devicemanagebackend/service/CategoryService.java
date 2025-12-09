package chat.hbcit.devicemanagebackend.service;

import chat.hbcit.devicemanagebackend.entity.Category;

import java.util.List;

/**
 * 设备分类服务接口
 *
 * @author DeviceManage
 */
public interface CategoryService {
    /**
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    List<Category> getAllCategories();

    /**
     * 根据ID获取分类详情
     *
     * @param id 分类ID
     * @return 分类信息
     */
    Category getCategoryById(Long id);

    /**
     * 创建分类
     *
     * @param category 分类信息
     * @return 创建的分类
     */
    Category createCategory(Category category);

    /**
     * 更新分类信息
     *
     * @param id       分类ID
     * @param category 分类信息
     */
    void updateCategory(Long id, Category category);

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);
}

