package chat.hbcit.devicemanagebackend.scheduler;

import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowOverdueSchedulerTest {

    @InjectMocks
    private BorrowOverdueScheduler scheduler;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void scanOverdueRecordsShouldUpdateWarningSummaryWhenOverdueExists() {
        when(borrowRecordMapper.selectList(any())).thenReturn(List.of(new BorrowRecord(), new BorrowRecord()));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        scheduler.scanOverdueRecords();
        Map<String, Object> summary = scheduler.getLatestSummary();

        assertThat(summary.get("overdueCount")).isEqualTo(2);
        assertThat(summary.get("status")).isEqualTo("WARNING");
        verify(valueOperations).set(eq(BorrowOverdueScheduler.OVERDUE_SUMMARY_KEY), eq("2"), any(Duration.class));
    }

    @Test
    void scanOverdueRecordsShouldUpdateOkSummaryAndDegradeWhenRedisFails() {
        when(borrowRecordMapper.selectList(any())).thenReturn(List.of());
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        scheduler.scanOverdueRecords();
        Map<String, Object> summary = scheduler.getLatestSummary();

        assertThat(summary.get("overdueCount")).isEqualTo(0);
        assertThat(summary.get("status")).isEqualTo("OK");
    }
}
