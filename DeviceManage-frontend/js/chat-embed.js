/**
 * 聊天嵌入脚本
 * 在所有页面中加载聊天功能
 */
(function() {
    // 创建并插入聊天脚本
    const chatScript = document.createElement('script');
    chatScript.async = true;
    chatScript.defer = true;
    chatScript.src = 'http://localhost:8080/chat/api/embed?protocol=http&host=localhost:8080&token=444eb8ea7df2e4e7';
    
    // 将脚本添加到页面
    if (document.head) {
        document.head.appendChild(chatScript);
    } else {
        // 如果 head 还未加载，等待 DOM 加载完成
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', function() {
                document.head.appendChild(chatScript);
            });
        } else {
            // 如果 DOM 已加载，直接添加到 body
            document.body.appendChild(chatScript);
        }
    }
})();

