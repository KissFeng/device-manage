package chat.hbcit.devicemanagebackend.common;

import chat.hbcit.devicemanagebackend.constants.ResponseCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 *
 * @param <T> 数据类型
 * @author DeviceManage
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     *
     * @return 响应结果
     */
    public static <T> Result<T> success() {
        return new Result<>(ResponseCode.SUCCESS, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResponseCode.SUCCESS, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 响应消息
     * @return 响应结果
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ResponseCode.SUCCESS, message, null);
    }

    /**
     * 成功响应（自定义消息和数据）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @return 响应结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResponseCode.SUCCESS, message, data);
    }

    /**
     * 失败响应
     *
     * @param message 错误消息
     * @return 响应结果
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResponseCode.INTERNAL_ERROR, message, null);
    }

    /**
     * 失败响应（自定义状态码）
     *
     * @param code    状态码
     * @param message 错误消息
     * @return 响应结果
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}

