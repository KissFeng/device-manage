package chat.hbcit.devicemanagebackend.scheduler;

import chat.hbcit.devicemanagebackend.entity.BorrowRecord;
import chat.hbcit.devicemanagebackend.enums.BorrowStatus;
import chat.hbcit.devicemanagebackend.mapper.BorrowRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 借用逾期定时巡检任务（@Scheduled 扩展组件）
 * 定期扫描已批准但超过预计归还日期仍未归还的借用记录，记录告警并将巡检摘要同步至 Redis。
 */
@Component
public class BorrowOverdueScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BorrowOverdueScheduler.class);
    static final String OVERDUE_SUMMARY_KEY = "device_manage:borrow:overdue_count";

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    private final AtomicReference<Map<String, Object>> latestSummary = new AtomicReference<>(Map.of(
            "overdueCount", 0,
            "lastScanTime", "尚未执行",
            "status", "INIT"
    ));

    /**
     * 每 5 分钟巡检一次逾期未归还借用记录（启动 10 秒后执行首次巡检）
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 10_000)
    public void scanOverdueRecords() {
        try {
            LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BorrowRecord::getStatus, BorrowStatus.APPROVED.getCode())
                    .lt(BorrowRecord::getExpectedReturnDate, LocalDate.now());
            List<BorrowRecord> overdueRecords = borrowRecordMapper.selectList(wrapper);

            int count = overdueRecords == null ? 0 : overdueRecords.size();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("overdueCount", count);
            summary.put("lastScanTime", LocalDateTime.now().toString());
            summary.put("status", count > 0 ? "WARNING" : "OK");
            latestSummary.set(summary);

            if (count > 0) {
                logger.warn("【逾期巡检告警】当前存在 {} 条逾期未归还的设备借用记录，请管理员及时督促归还", count);
            } else {
                logger.info("【逾期巡检完成】当前无逾期未归还设备");
            }

            syncSummaryToRedisQuietly(count);
        } catch (Exception ex) {
            logger.error("【逾期巡检异常】定时扫描失败: {}", ex.getMessage());
        }
    }

    public Map<String, Object> getLatestSummary() {
        return latestSummary.get();
    }

    private void syncSummaryToRedisQuietly(int count) {
        if (stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(OVERDUE_SUMMARY_KEY, String.valueOf(count), Duration.ofMinutes(10));
        } catch (Exception ex) {
            logger.warn("同步逾期巡检数量至 Redis 失败（已降级为仅保留内存摘要）: {}", ex.getMessage());
        }
    }
}
