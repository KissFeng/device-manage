package chat.hbcit.devicemanagebackend.constants;

/**
 * 响应状态码常量
 *
 * @author DeviceManage
 */
public class ResponseCode {
    /**
     * 操作成功
     */
    public static final int SUCCESS = 200;

    /**
     * 请求参数错误
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未登录或Token无效
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 权限不足
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器内部错误
     */
    public static final int INTERNAL_ERROR = 500;

    private ResponseCode() {
    }
}

