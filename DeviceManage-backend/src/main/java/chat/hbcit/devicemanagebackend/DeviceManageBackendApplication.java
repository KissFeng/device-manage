package chat.hbcit.devicemanagebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 设备管理后端应用启动类
 *
 * @author DeviceManage
 */
@SpringBootApplication
@EnableScheduling
public class DeviceManageBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceManageBackendApplication.class, args);
    }

}
