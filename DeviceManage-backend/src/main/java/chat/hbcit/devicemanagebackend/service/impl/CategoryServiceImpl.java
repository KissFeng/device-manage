package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.entity.Device;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.CategoryMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import chat.hbcit.devicemanagebackend.service.CategoryService;
import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备分类服务实现类
 *
 * @author DeviceManage
 */
@Service
public class CategoryServiceImpl implements CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public List<Category> getAllCategories() {
        return categoryMapper.selectList(null);
    }

    @Override
    public Category getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Category createCategory(Category category) {
        // 检查分类名称是否已存在（去除首尾空格）
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }
        
        // 查询所有分类，检查是否有重名（使用trim比较）
        List<Category> allCategories = categoryMapper.selectList(null);
        for (Category cat : allCategories) {
            String existingName = cat.getName() != null ? cat.getName().trim() : null;
            if (existingName != null && existingName.equals(category.getName())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST, "分类名称已存在");
            }
        }

        categoryMapper.insert(category);
        logger.info("创建分类成功，分类ID：{}，分类名称：{}", category.getId(), category.getName());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, Category category) {
        Category existCategory = getCategoryById(id);

        // 处理名称：去除首尾空格
        String newName = category.getName() != null ? category.getName().trim() : null;
        String oldName = existCategory.getName() != null ? existCategory.getName().trim() : null;

        logger.debug("更新分类 - ID: {}, 新名称: '{}', 旧名称: '{}'", id, newName, oldName);

        // 如果名称为空，不更新名称
        if (newName == null || newName.isEmpty()) {
            logger.warn("分类名称为空，跳过名称更新，分类ID：{}", id);
        } else if (newName.equals(oldName)) {
            // 名称没有改变（trim后相同），不需要检查重名，也不需要更新名称
            logger.debug("分类名称未改变（trim后相同），跳过名称更新和重名检查，分类ID：{}", id);
        } else {
            // 名称改变了，需要检查是否与其他分类重名（排除当前分类）
            logger.debug("分类名称已改变，检查重名 - 新名称: '{}', 旧名称: '{}'", newName, oldName);
            
            // 查询所有分类，使用trim比较检查是否有重名（排除当前分类）
            List<Category> allCategories = categoryMapper.selectList(null);
            for (Category cat : allCategories) {
                // 跳过当前分类
                if (cat.getId().equals(id)) {
                    continue;
                }
                
                // 使用trim比较名称
                String existingName = cat.getName() != null ? cat.getName().trim() : null;
                if (existingName != null && existingName.equals(newName)) {
                    logger.warn("分类名称已存在 - 新名称: '{}', 冲突分类ID: {}, 冲突分类名称: '{}'", 
                            newName, cat.getId(), cat.getName());
                    throw new BusinessException(ResponseCode.BAD_REQUEST, "分类名称已存在");
                }
            }
            
            // 更新名称
            existCategory.setName(newName);
            logger.debug("分类名称更新成功 - ID: {}, 新名称: '{}'", id, newName);
        }

        // 更新描述
        if (category.getDescription() != null) {
            existCategory.setDescription(category.getDescription().trim());
        }

        categoryMapper.updateById(existCategory);
        logger.info("更新分类成功，分类ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);

        // 检查是否有关联设备
        LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
        deviceWrapper.eq(Device::getCategoryId, id);
        Long deviceCount = deviceMapper.selectCount(deviceWrapper);
        if (deviceCount > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该分类下存在设备，无法删除");
        }

        categoryMapper.deleteById(id);
        logger.info("删除分类成功，分类ID：{}，分类名称：{}", id, category.getName());
    }
}

