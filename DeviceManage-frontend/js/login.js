/**
 * 登录页面逻辑
 */

document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');
    const loginBtn = document.getElementById('loginBtn');
    
    // 如果已登录，跳转到主页
    if (isAuthenticated()) {
        const userInfo = getUserInfo();
        if (userInfo.role === 'ADMIN' || userInfo.role === 'ROOT') {
            window.location.href = '/admin/dashboard.html';
        } else {
            window.location.href = '/user/devices.html';
        }
        return;
    }
    
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        
        // 验证输入
        if (!username || !password) {
            showError('请输入用户名和密码');
            return;
        }
        
        // 显示加载状态
        loginBtn.disabled = true;
        loginBtn.innerHTML = '<span class="loading"></span> 登录中...';
        errorMessage.style.display = 'none';
        
        // 前端临时登录校验（用于测试，无需后端）
        if (username === 'abc' && password === '123') {
            // 模拟登录成功
            setTimeout(() => {
                // 生成模拟Token
                const mockToken = 'mock_token_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
                
                // 模拟用户信息（管理员角色，可以看到所有功能）
                const mockUserInfo = {
                    id: 999,
                    username: 'abc',
                    realName: '测试管理员',
                    role: 'ADMIN',
                    status: 'ACTIVE'
                };
                
                // 保存Token和用户信息
                setToken(mockToken);
                setUserInfo(mockUserInfo);
                
                // 跳转到管理员仪表盘
                window.location.href = '/admin/dashboard.html';
            }, 500); // 模拟网络延迟
            return;
        }
        
        try {
            const response = await API.auth.login(username, password);
            
            if (response.code === 200) {
                // 保存Token和用户信息
                setToken(response.data.token);
                setUserInfo(response.data.userInfo);
                
                // 根据角色跳转
                if (response.data.userInfo.role === 'ADMIN' || response.data.userInfo.role === 'ROOT') {
                    window.location.href = '/admin/dashboard.html';
                } else {
                    window.location.href = '/user/devices.html';
                }
            } else {
                showError(response.message || '登录失败');
            }
        } catch (error) {
            showError(error.message || '登录失败，请检查网络连接');
        } finally {
            // 恢复按钮状态
            loginBtn.disabled = false;
            loginBtn.innerHTML = '<span>登录</span><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"></path><path d="M12 5l7 7-7 7"></path></svg>';
        }
    });
    
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
    }
});

