/**
 * 布局相关功能
 * 创建导航栏、处理用户信息显示等
 */

/**
 * 创建导航栏
 */
function createNavbar(menuItems = []) {
    const userInfo = getUserInfo();
    if (!userInfo) {
        window.location.href = '/index.html';
        return;
    }
    
    const navbar = document.createElement('nav');
    navbar.className = 'navbar';
    
    const navbarContent = document.createElement('div');
    navbarContent.className = 'navbar-content';
    
    // Logo
    const brandLink = document.createElement('a');
    brandLink.href = (userInfo.role === 'ADMIN' || userInfo.role === 'ROOT') ? '/admin/dashboard.html' : '/user/devices.html';
    brandLink.className = 'navbar-brand';
    brandLink.innerHTML = `
        <img src="../static/logo.png" alt="光影资产" style="width: 32px; height: 32px; object-fit: contain;">
        <span>光影资产</span>
    `;
    navbarContent.appendChild(brandLink);
    
    // 菜单
    const menuDiv = document.createElement('div');
    menuDiv.className = 'navbar-menu';
    menuItems.forEach(item => {
        const menuItem = document.createElement('a');
        menuItem.href = item.href;
        menuItem.className = `navbar-item ${item.active ? 'active' : ''}`;
        if (item.icon) {
            menuItem.innerHTML = `
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${item.icon}</svg>
                <span>${item.text}</span>
            `;
        } else {
            menuItem.textContent = item.text;
        }
        menuDiv.appendChild(menuItem);
    });
    navbarContent.appendChild(menuDiv);
    
    // 用户信息
    const userDiv = document.createElement('div');
    userDiv.className = 'navbar-user';
    
    const userInfoDiv = document.createElement('div');
    userInfoDiv.className = 'user-info';
    let roleBadge = '';
    if (userInfo.role === 'ROOT') {
        roleBadge = '<span class="badge badge-danger">超级管理员</span>';
    } else if (userInfo.role === 'ADMIN') {
        roleBadge = '<span class="badge badge-info">管理员</span>';
    } else {
        roleBadge = '<span class="badge badge-secondary">用户</span>';
    }
    
    userInfoDiv.innerHTML = `
        <div class="user-avatar">${userInfo.realName ? userInfo.realName.charAt(0) : userInfo.username.charAt(0).toUpperCase()}</div>
        <span class="user-name">${userInfo.realName || userInfo.username}</span>
        ${roleBadge}
    `;
    userDiv.appendChild(userInfoDiv);
    
    // 退出按钮
    const logoutBtn = document.createElement('button');
    logoutBtn.className = 'logout-btn';
    logoutBtn.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
        退出
    `;
    logoutBtn.addEventListener('click', logout);
    userDiv.appendChild(logoutBtn);
    
    navbarContent.appendChild(userDiv);
    navbar.appendChild(navbarContent);
    
    return navbar;
}

/**
 * 创建页面布局
 */
function createLayout(menuItems = []) {
    const layout = document.createElement('div');
    layout.className = 'layout';
    
    const navbar = createNavbar(menuItems);
    const mainContent = document.createElement('main');
    mainContent.className = 'main-content';
    const container = document.createElement('div');
    container.className = 'container';
    
    mainContent.appendChild(container);
    layout.appendChild(navbar);
    layout.appendChild(mainContent);
    
    document.body.appendChild(layout);
    
    // 等待 DOM 完全加载后再移动页面内容
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            movePageContentToContainer(container);
        });
    } else {
        // DOM 已经加载完成，立即移动
        movePageContentToContainer(container);
    }
    
    return {
        layout,
        navbar,
        content: container,
    };
}

/**
 * 将页面内容移动到容器中
 */
function movePageContentToContainer(container) {
    if (!container) return;
    
    // 查找所有需要移动的元素
    const selectors = [
        '.page-header',
        '.toolbar',
        '.filter-bar',
        '.stats-grid',
        '.quick-actions',
        '.tabs',
        '#devicesList',
        '#categoriesList',
        '#usersList',
        '#recordsList',
        '#borrowList'
    ];
    
    const movedElements = new Set();
    
    // 按选择器查找并移动
    selectors.forEach(selector => {
        try {
            const elements = document.querySelectorAll(selector);
            elements.forEach(el => {
                if (el && el.parentNode && el.parentNode !== container && !movedElements.has(el)) {
                    // 确保元素在 body 中
                    if (el.parentNode === document.body || el.parentNode.tagName === 'BODY') {
                        container.appendChild(el);
                        movedElements.add(el);
                    }
                }
            });
        } catch (e) {
            console.warn('移动元素失败:', selector, e);
        }
    });
    
    // 移动 body 中的其他直接子元素（除了 script、style、layout 和 modal）
    const bodyChildren = Array.from(document.body.children);
    bodyChildren.forEach(child => {
        if (child.tagName !== 'SCRIPT' && 
            child.tagName !== 'STYLE' && 
            child.className !== 'layout' &&
            !child.id?.includes('Modal') &&
            !child.classList.contains('modal') &&
            !movedElements.has(child) &&
            child.parentNode === document.body) {
            // 检查是否是已知的选择器元素
            let isKnownElement = false;
            for (const sel of selectors) {
                try {
                    if (child.matches && child.matches(sel)) {
                        isKnownElement = true;
                        break;
                    }
                } catch (e) {
                    // 忽略匹配错误
                }
            }
            
            if (!isKnownElement) {
                container.appendChild(child);
                movedElements.add(child);
            }
        }
    });
    
    // 如果容器还是空的，尝试再次查找（延迟执行）
    if (container.children.length === 0) {
        setTimeout(() => {
            movePageContentToContainer(container);
        }, 100);
    }
}

/**
 * 显示加载状态
 */
function showLoading(element) {
    element.innerHTML = `
        <div class="empty-state">
            <div class="loading" style="width: 40px; height: 40px; margin: 0 auto;"></div>
            <p style="margin-top: 16px;">加载中...</p>
        </div>
    `;
}

/**
 * 显示空状态
 */
function showEmptyState(message = '暂无数据', icon = null) {
    const iconSvg = icon || `
        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
    `;
    return `
        <div class="empty-state">
            ${iconSvg}
            <p>${message}</p>
        </div>
    `;
}

/**
 * 显示错误消息
 */
function showError(message, container) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.style.display = 'block';
    errorDiv.textContent = message;
    
    if (container) {
        container.innerHTML = '';
        container.appendChild(errorDiv);
    } else {
        // 在页面顶部显示
        const existingError = document.querySelector('.page-error');
        if (existingError) {
            existingError.remove();
        }
        errorDiv.className = 'error-message page-error';
        const mainContent = document.querySelector('.main-content .container');
        if (mainContent) {
            mainContent.insertBefore(errorDiv, mainContent.firstChild);
        }
    }
}

/**
 * 创建分页组件
 * 使用事件委托避免 CSP 问题
 */
function createPagination(currentPage, totalPages, onPageChange) {
    if (totalPages <= 1) return '';
    
    const paginationDiv = document.createElement('div');
    paginationDiv.className = 'pagination';
    paginationDiv.setAttribute('data-current-page', currentPage);
    paginationDiv.setAttribute('data-total-pages', totalPages);
    
    // 上一页
    const prevBtn = document.createElement('button');
    prevBtn.textContent = '上一页';
    prevBtn.disabled = currentPage === 1;
    if (!prevBtn.disabled) {
        prevBtn.addEventListener('click', () => onPageChange(currentPage - 1));
    }
    paginationDiv.appendChild(prevBtn);
    
    // 页码
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    if (startPage > 1) {
        const firstBtn = document.createElement('button');
        firstBtn.textContent = '1';
        firstBtn.addEventListener('click', () => onPageChange(1));
        paginationDiv.appendChild(firstBtn);
        
        if (startPage > 2) {
            const ellipsis = document.createElement('button');
            ellipsis.textContent = '...';
            ellipsis.disabled = true;
            paginationDiv.appendChild(ellipsis);
        }
    }
    
    for (let i = startPage; i <= endPage; i++) {
        const pageBtn = document.createElement('button');
        pageBtn.textContent = i.toString();
        if (i === currentPage) {
            pageBtn.classList.add('active');
        } else {
            pageBtn.addEventListener('click', () => onPageChange(i));
        }
        paginationDiv.appendChild(pageBtn);
    }
    
    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            const ellipsis = document.createElement('button');
            ellipsis.textContent = '...';
            ellipsis.disabled = true;
            paginationDiv.appendChild(ellipsis);
        }
        const lastBtn = document.createElement('button');
        lastBtn.textContent = totalPages.toString();
        lastBtn.addEventListener('click', () => onPageChange(totalPages));
        paginationDiv.appendChild(lastBtn);
    }
    
    // 下一页
    const nextBtn = document.createElement('button');
    nextBtn.textContent = '下一页';
    nextBtn.disabled = currentPage === totalPages;
    if (!nextBtn.disabled) {
        nextBtn.addEventListener('click', () => onPageChange(currentPage + 1));
    }
    paginationDiv.appendChild(nextBtn);
    
    return paginationDiv;
}

