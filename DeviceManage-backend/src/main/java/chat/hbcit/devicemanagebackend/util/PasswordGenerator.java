package chat.hbcit.devicemanagebackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成BCrypt加密后的密码
 * 
 * 使用方法：
 * 1. 在IDE中直接运行main方法
 * 2. 复制输出的SQL语句到数据库执行
 *
 * @author DeviceManage
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成123456的BCrypt加密值
        String password = "123456";
        String encodedPassword = encoder.encode(password);
        
        System.out.println("============================================");
        System.out.println("密码生成工具");
        System.out.println("============================================");
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt加密后的密码: " + encodedPassword);
        System.out.println();
        System.out.println("SQL更新语句（将admin密码重置为123456）:");
        System.out.println("============================================");
        System.out.println("USE device_manage;");
        System.out.println("UPDATE users SET password = '" + encodedPassword + "' WHERE username = 'admin';");
        System.out.println("============================================");
        System.out.println();
        System.out.println("注意：BCrypt每次生成的加密值都不同，但都能验证通过原始密码");
    }
}

