package com.jtools.mybatislog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColorPanel
import com.intellij.ui.Gray
import com.intellij.ui.components.JBCheckBox
import com.lhstack.tools.plugins.IPlugin
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class PluginImpl : IPlugin {

    companion object {
        val ansiColorMap = mutableMapOf<String, String>()

        val colorMap = mutableMapOf<String, Color>()

        val componentCache = mutableMapOf<String, JComponent>()

        init {
            ansiColorMap["黑色"] = "30"
            ansiColorMap["红色"] = "31"
            ansiColorMap["绿色"] = "32"
            ansiColorMap["黄色"] = "33"
            ansiColorMap["蓝色"] = "34"
            ansiColorMap["紫色"] = "35"
            ansiColorMap["青色"] = "36"
            ansiColorMap["白色"] = "37"

            // 前景色 - 亮色
            ansiColorMap["亮黑色"] = "90"
            ansiColorMap["亮红色"] = "91"
            ansiColorMap["亮绿色"] = "92"
            ansiColorMap["亮黄色"] = "93"
            ansiColorMap["亮蓝色"] = "94"
            ansiColorMap["亮紫色"] = "95"
            ansiColorMap["亮青色"] = "96"
            ansiColorMap["亮白色"] = "97"

            colorMap["30"] = Gray._0 // 黑色
            colorMap["31"] = Color(170, 0, 0) // 红色
            colorMap["32"] = Color(0, 170, 0) // 绿色
            colorMap["33"] = Color(170, 85, 0) // 黄色
            colorMap["34"] = Color(0, 0, 170) // 蓝色
            colorMap["35"] = Color(170, 0, 170) // 紫色
            colorMap["36"] = Color(0, 170, 170) // 青色
            colorMap["37"] = Gray._170 // 白色


            // 亮色
            colorMap["90"] = Color(85, 85, 85) // 亮黑色
            colorMap["91"] = Color(255, 85, 85) // 亮红色
            colorMap["92"] = Color(85, 255, 85) // 亮绿色
            colorMap["93"] = Color(255, 255, 85) // 亮黄色
            colorMap["94"] = Color(85, 85, 255) // 亮蓝色
            colorMap["95"] = Color(255, 85, 255) // 亮紫色
            colorMap["96"] = Color(85, 255, 255) // 亮青色
            colorMap["97"] = Gray._255 // 亮白色
        }
    }

    override fun install() {
        StarterJavaProgramPatcher.registry()
    }

    override fun unInstall() {
        StarterJavaProgramPatcher.unRegistry()
    }


    override fun createPanel(project: Project): JComponent = componentCache.computeIfAbsent(project.locationHash) {
        JPanel(VerticalLayout()).also {
            it.add(JPanel(BorderLayout()).apply {
                this.add(JLabel("开启Sql日志控制台输出: ", JLabel.LEFT), BorderLayout.WEST)
                this.add(JBCheckBox().apply {
                    this.isSelected = PluginState.getInstance(project).enabled
                    this.addActionListener {
                        PluginState.getInstance(project).enabled = this.isSelected
                    }
                }, BorderLayout.CENTER)
            })
            it.add(JPanel(BorderLayout()).apply {
                this.add(JLabel("控制台日志颜色: ", JLabel.LEFT), BorderLayout.WEST)
                this.add(JPanel(HorizontalLayout()).apply {
                    val colorPanel = ColorPanel().apply {
                        this.selectedColor = colorMap[PluginState.getInstance(project).ansiCode]
                    }
                    val comboBox = ComboBox<String>(ansiColorMap.keys.toTypedArray())
                    comboBox.selectedItem = PluginState.getInstance(project).colorName
                    comboBox.addItemListener { e ->
                        PluginState.getInstance(project).colorName = e.item as String
                        colorPanel.selectedColor = colorMap[ansiColorMap[e.item as String]]
                        PluginState.getInstance(project).ansiCode = ansiColorMap[e.item as String]!!
                    }
                    this.add(comboBox, BorderLayout.WEST)
                    this.add(colorPanel, BorderLayout.NORTH)
                }, BorderLayout.CENTER)
            })
        }
    }

    override fun closePanel(project: Project, pluginPanel: JComponent) {
        super.closePanel(project, pluginPanel)
        componentCache.remove(project.locationHash)
    }

    override fun installRestart(): Boolean = true

    override fun support(jToolsVersion: Int): Boolean = jToolsVersion >= 103

    override fun pluginIcon(): Icon? = IconLoader.findIcon("icons/mybatis.svg", PluginImpl::class.java)

    override fun pluginTabIcon(): Icon? = IconLoader.findIcon("icons/mybatis_tab.svg", PluginImpl::class.java)

    override fun pluginName(): String = "jtools-mybatis-log"

    override fun pluginDesc(): String = "jtools-mybatis-log"

    override fun pluginVersion(): String = "v1.0.2"
}