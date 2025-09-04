package com.jtools.mybatislog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.jtools.mybatislog.PluginImpl.Companion.ansiColorMap
import com.jtools.mybatislog.PluginImpl.Companion.colorMap
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class SettingPanel(val project: Project,val pluginState: PluginState): JPanel() {
    init {
        this.layout = VerticalLayout()
        /*if(showInstallBut) {
            this.add(JPanel(BorderLayout()).apply {
                this.add(JLabel("安装agent: ", JLabel.LEFT), BorderLayout.WEST)
                this.add(JBCheckBox().apply {
                    val file = File(System.getProperty("user.home") + "/.jtools/jtools-mybatis-log/agent.jar")
                    this.isSelected = file.exists()
                    this.addActionListener {
                        pluginState.enabled = this.isSelected
                        if(!file.exists() && this.isSelected){
                            StarterJavaProgramPatcher.install()
                            Notifications.Bus.notify(Notification("com.jtools.mybatislog.jtools-mybatis-log","安装agent成功",NotificationType.INFORMATION))
                        }
                        if(file.exists() && !this.isSelected){
                            file.delete()
                            Notifications.Bus.notify(Notification("com.jtools.mybatislog.jtools-mybatis-log","卸载agent成功",NotificationType.INFORMATION))
                        }
                    }
                }, BorderLayout.CENTER)
            })
        }*/

        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("开启Sql日志控制台输出: ", JLabel.LEFT), BorderLayout.WEST)
            this.add(JBCheckBox().apply {
                this.isSelected = pluginState.enabled
                this.addActionListener {
                    pluginState.enabled = this.isSelected
                }
            }, BorderLayout.CENTER)
        })
        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("控制台日志颜色: ", JLabel.LEFT), BorderLayout.WEST)
            this.add(JPanel(HorizontalLayout()).apply {
                val colorPanel = ColorPanel().apply {
                    this.setEditable(false)
                    this.selectedColor = colorMap[pluginState.ansiCode]
                }
                val comboBox = ComboBox<String>(ansiColorMap.keys.toTypedArray())
                comboBox.selectedItem = pluginState.colorName
                comboBox.addItemListener { e ->
                    pluginState.colorName = e.item as String
                    colorPanel.selectedColor = colorMap[ansiColorMap[e.item as String]]
                    pluginState.ansiCode = ansiColorMap[e.item as String]!!
                }
                this.add(comboBox, BorderLayout.WEST)
                this.add(colorPanel, BorderLayout.NORTH)
            }, BorderLayout.CENTER)
        })
    }
}