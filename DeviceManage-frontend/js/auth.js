/**
 * 认证相关功能
 * 处理登录状态、权限检查等
 */

/**
 * 检查用户是否已登录
 */
function isAuthenticated() {
    return !!getToken();
}

/**
 * 检查用户是否为超级管理员
 */
function isRoot() {
    const userInfo = getUserInfo();
    return userInfo && userInfo.role === 'ROOT';
}

/**
 * 检查用户是否为管理员（包括 ROOT 和 ADMIN）
 */
function isAdmin() {
    const userInfo = getUserInfo();
    return userInfo && (userInfo.role === 'ADMIN' || userInfo.role === 'ROOT');
}

/**
 * 检查用户是否为普通管理员（不包括 ROOT）
 */
function isAdminOnly() {
    const userInfo = getUserInfo();
    return userInfo && userInfo.role === 'ADMIN';
}

/**
 * 检查用户是否为普通用户
 */
function isUser() {
    const userInfo = getUserInfo();
    return userInfo && userInfo.role === 'USER';
}

/**
 * 权限检查中间件
 * 在需要权限的页面调用此函数
 */
function requireAuth(requiredRole = null) {
    // 检查是否登录
    if (!isAuthenticated()) {
        console.log('[requireAuth] 用户未登录，跳转到登录页');
        window.location.href = '/index.html';
        return false;
    }
    
    // 检查角色权限
    if (requiredRole) {
        const userInfo = getUserInfo();
        if (!userInfo) {
            // 如果没有用户信息，跳转到登录页（可能是 token 过期）
            console.log('[requireAuth] 用户信息不存在，跳转到登录页');
            window.location.href = '/index.html';
            return false;
        }
        
        console.log('[requireAuth] 检查权限 - 需要角色:', requiredRole, '当前用户角色:', userInfo.role);
        
        // ROOT 用户拥有最高权限，可以访问所有页面
        if (userInfo.role === 'ROOT') {
            console.log('[requireAuth] ROOT 用户，允许访问');
            return true;
        }
        
        // ROOT 和 ADMIN 都可以访问管理员页面
        if (requiredRole === 'ADMIN') {
            if (userInfo.role !== 'ADMIN' && userInfo.role !== 'ROOT') {
                console.log('[requireAuth] 权限不足 - 需要 ADMIN 或 ROOT，当前角色:', userInfo.role);
                alert('权限不足');
                window.location.href = '/index.html';
                return false;
            }
        } else if (requiredRole === 'USER') {
            // USER 页面：USER、ADMIN、ROOT 都可以访问（因为管理员也可能需要查看自己的记录）
            if (userInfo.role !== 'USER' && userInfo.role !== 'ADMIN' && userInfo.role !== 'ROOT') {
                console.log('[requireAuth] 权限不足 - 需要 USER、ADMIN 或 ROOT，当前角色:', userInfo.role);
                alert('权限不足');
                window.location.href = '/index.html';
                return false;
            }
        } else if (userInfo.role !== requiredRole) {
            console.log('[requireAuth] 权限不足 - 需要角色:', requiredRole, '当前角色:', userInfo.role);
            alert('权限不足');
            window.location.href = '/index.html';
            return false;
        }
    }
    
    return true;
}

/**
 * 初始化用户信息
 * 从服务器获取最新的用户信息
 */
async function initUserInfo() {
    try {
        const response = await API.auth.getUserInfo();
        if (response.code === 200) {
            setUserInfo(response.data);
            return response.data;
        }
    } catch (error) {
        console.error('获取用户信息失败:', error);
        removeToken();
        removeUserInfo();
        if (window.location.pathname !== '/index.html' && window.location.pathname !== '/') {
            window.location.href = '/index.html';
        }
    }
}

/**
 * 退出登录
 */
async function logout() {
    try {
        await API.auth.logout();
    } catch (error) {
        console.error('退出登录失败:', error);
    } finally {
        removeToken();
        removeUserInfo();
        window.location.href = '/index.html';
    }
}

