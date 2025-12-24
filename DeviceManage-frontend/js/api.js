/**
 * API 请求封装
 * 统一处理请求、响应和错误
 */

// 后端API地址
const API_BASE_URL = (function() {
    const hostname = window.location.hostname;
    // 如果是本地开发环境
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:9011';
    }
    // 如果是HTTPS环境（生产环境反向代理），使用 /prod-api 前缀（需要Nginx配置转发）
    // 或者如果你希望直接使用当前域名根路径转发，可以使用 window.location.origin
    if (window.location.protocol === 'https:') {
        return '/prod-api'; 
    }
    // 默认回退（HTTP IP访问）
    return 'http://43.138.28.205:9011';
})();

/**
 * 获取Token
 */
function getToken() {
    return localStorage.getItem('token');
}

/**
 * 设置Token
 */
function setToken(token) {
    localStorage.setItem('token', token);
}

/**
 * 移除Token
 */
function removeToken() {
    localStorage.removeItem('token');
}

/**
 * 获取用户信息
 */
function getUserInfo() {
    const userInfo = localStorage.getItem('userInfo');
    return userInfo ? JSON.parse(userInfo) : null;
}

/**
 * 设置用户信息
 */
function setUserInfo(userInfo) {
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
}

/**
 * 移除用户信息
 */
function removeUserInfo() {
    localStorage.removeItem('userInfo');
}

/**
 * 统一请求函数
 */
async function request(url, options = {}) {
    const token = getToken();
    
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
    };
    
    // 添加Token到请求头
    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`;
    }
    
    // 合并选项
    const finalOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...(options.headers || {}),
        },
    };
    
    try {
        const response = await fetch(`${API_BASE_URL}${url}`, finalOptions);
        
        // 处理401未授权
        if (response.status === 401) {
            removeToken();
            removeUserInfo();
            if (window.location.pathname !== '/index.html' && window.location.pathname !== '/') {
                window.location.href = '/index.html';
            }
            throw new Error('登录已过期，请重新登录');
        }
        
        // 处理403权限不足
        if (response.status === 403) {
            // 尝试解析错误响应
            let errorMessage = '权限不足';
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                }
            } catch (e) {
                // 如果解析失败，使用默认错误消息
            }
            throw new Error(errorMessage);
        }
        
        // 检查响应是否有内容
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            if (!response.ok) {
                throw new Error(`请求失败: ${response.status} ${response.statusText}`);
            }
            return { code: 200, message: '操作成功', data: null };
        }
        
        const data = await response.json();
        
        // 如果响应不成功，抛出错误
        if (data.code !== 200) {
            throw new Error(data.message || '请求失败');
        }
        
        return data;
    } catch (error) {
        console.error('API请求错误:', error);
        throw error;
    }
}

/**
 * GET请求
 */
async function get(url, params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const finalUrl = queryString ? `${url}?${queryString}` : url;
    return request(finalUrl, {
        method: 'GET',
    });
}

/**
 * POST请求
 */
async function post(url, data = {}) {
    return request(url, {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

/**
 * PUT请求
 */
async function put(url, data = {}) {
    return request(url, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

/**
 * DELETE请求
 */
async function del(url) {
    return request(url, {
        method: 'DELETE',
    });
}

/**
 * API接口定义
 */
const API = {
    // 认证相关
    auth: {
        login: (username, password) => post('/auth/login', { username, password }),
        logout: () => post('/auth/logout'),
        getUserInfo: () => get('/user/info'),
    },
    
    // 用户管理（管理员）
    users: {
        list: (params) => get('/admin/users', params),
        create: (data) => post('/admin/users', data),
        update: (id, data) => put(`/admin/users/${id}`, data),
        delete: (id) => del(`/admin/users/${id}`),
        detail: (id) => get(`/admin/users/${id}`),
    },
    
    // 分类管理
    categories: {
        list: () => get('/categories'), // 用户端和管理员都可以访问
        create: (data) => post('/admin/categories', data), // 仅管理员
        update: (id, data) => put(`/admin/categories/${id}`, data), // 仅管理员
        delete: (id) => del(`/admin/categories/${id}`), // 仅管理员
    },
    
    // 设备管理（管理员）
    devices: {
        list: (params) => get('/admin/devices', params),
        create: (data) => post('/admin/devices', data),
        update: (id, data) => put(`/admin/devices/${id}`, data),
        updateStatus: (id, status) => put(`/admin/devices/${id}/status`, { status }),
        delete: (id) => del(`/admin/devices/${id}`),
        detail: (id) => get(`/admin/devices/${id}`),
    },
    
    // 设备浏览（用户）
    deviceBrowse: {
        list: (params) => get('/devices', params),
    },
    
    // 借用管理
    borrow: {
        apply: (data) => post('/borrow/apply', data),
        myRecords: (params) => get('/borrow/my-records', params),
    },
    
    // 借用审批（管理员）
    borrowAdmin: {
        pending: (params) => get('/admin/borrow/pending', params),
        approve: (id, data) => put(`/admin/borrow/${id}/approve`, data),
        return: (id, data) => put(`/admin/borrow/${id}/return`, data),
        records: (params) => get('/admin/borrow/records', params),
    },
};

