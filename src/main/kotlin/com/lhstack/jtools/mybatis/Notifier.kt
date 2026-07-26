package com.lhstack.jtools.mybatis

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

/**
 * 统一的用户可见提示入口。
 *
 * 使用 Notification 的字符串 groupId 构造器: 该重载从早期版本一直保留至今,
 * 兼容插件声明的 171 ~ 261 全版本区间。
 */
object Notifier {

    private const val GROUP_ID = "JToolsMybatisLog"

    fun warn(project: Project, content: String) {
        notify(project, content, NotificationType.WARNING)
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        Notifications.Bus.notify(Notification(GROUP_ID, "JTools Mybatis Log", content, type), project)
    }
}
