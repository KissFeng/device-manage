package chat.hbcit.devicemanagebackend.service.impl;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import chat.hbcit.devicemanagebackend.entity.Category;
import chat.hbcit.devicemanagebackend.exception.BusinessException;
import chat.hbcit.devicemanagebackend.mapper.CategoryMapper;
import chat.hbcit.devicemanagebackend.mapper.DeviceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getAllCategoriesShouldReturnFromRedisWhenCacheHit() throws Exception {
        Category c = new Category();
        c.setId(1L);
        c.setName("相机机身");
        String cachedJson = objectMapper.writeValueAsString(List.of(c));

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CategoryServiceImpl.CATEGORIES_CACHE_KEY)).thenReturn(cachedJson);

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("相机机身");
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void getAllCategoriesShouldQueryDbAndWriteCacheWhenCacheMiss() {
        Category c = new Category();
        c.setId(2L);
        c.setName("专业镜头");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CategoryServiceImpl.CATEGORIES_CACHE_KEY)).thenReturn(null);
        when(categoryMapper.selectList(null)).thenReturn(List.of(c));

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("专业镜头");
        verify(categoryMapper).selectList(null);
        verify(valueOperations).set(eq(CategoryServiceImpl.CATEGORIES_CACHE_KEY), anyString(), any(Duration.class));
    }

    @Test
    void getAllCategoriesShouldDegradeToDbWhenRedisUnavailable() {
        Category c = new Category();
        c.setId(3L);
        c.setName("灯光系统");

        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));
        when(categoryMapper.selectList(null)).thenReturn(List.of(c));

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("灯光系统");
        verify(categoryMapper).selectList(null);
    }

    @Test
    void createCategoryShouldThrowWhenDuplicateName() {
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("相机机身");
        when(categoryMapper.selectList(null)).thenReturn(List.of(existing));

        Category input = new Category();
        input.setName(" 相机机身 ");

        assertThatThrownBy(() -> categoryService.createCategory(input))
                .isInstanceOf(BusinessException.class)
                .hasMessage("分类名称已存在");
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void createCategoryShouldInsertAndEvictCache() {
        when(categoryMapper.selectList(null)).thenReturn(List.of());
        Category input = new Category();
        input.setName("  云台配件  ");

        Category created = categoryService.createCategory(input);

        assertThat(created.getName()).isEqualTo("云台配件");
        verify(categoryMapper).insert(input);
        verify(stringRedisTemplate).delete(CategoryServiceImpl.CATEGORIES_CACHE_KEY);
    }

    @Test
    void deleteCategoryShouldThrowWhenHasDevices() {
        Category cat = new Category();
        cat.setId(10L);
        cat.setName("音频录制");
        when(categoryMapper.selectById(10L)).thenReturn(cat);
        when(deviceMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> categoryService.deleteCategory(10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ResponseCode.BAD_REQUEST))
                .hasMessage("该分类下存在设备，无法删除");
        verify(categoryMapper, never()).deleteById(anyLong());
    }
}
