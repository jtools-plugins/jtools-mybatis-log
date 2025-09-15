package com.lhstack.jtools.mybatis

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.collections.get

class SettingPanel(val project: Project,val tempProps: TempProps,updated: (TempProps) -> Unit = {}): JPanel() {

    val selectBox = JBCheckBox()
    val colorPanel = ColorPanel().apply {
        this.setEditable(false)
        this.selectedColor = Const.colorMap[tempProps.ansiCode]
    }
    val comboBox = ComboBox<String>(Const.ansiColorMap.keys.toTypedArray())
    init {
        this.layout = VerticalLayout()
        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("开启Sql日志控制台输出: ", JLabel.LEFT), BorderLayout.WEST)
            this.add(selectBox.apply {
                this.isSelected = tempProps.enabled
                this.addActionListener {
                    tempProps.enabled = this.isSelected
                    updated.invoke(tempProps)
                }
            }, BorderLayout.CENTER)
        })
        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("控制台日志颜色: ", JLabel.LEFT), BorderLayout.WEST)
            this.add(JPanel(HorizontalLayout()).apply {
                comboBox.selectedItem = tempProps.colorName
                comboBox.addItemListener { e ->
                    tempProps.colorName = e.item as String
                    colorPanel.selectedColor = Const.colorMap[Const.ansiColorMap[e.item as String]]
                    tempProps.ansiCode = Const.ansiColorMap[e.item as String]!!
                    updated.invoke(tempProps)
                }
                this.add(comboBox, BorderLayout.WEST)
                this.add(colorPanel, BorderLayout.NORTH)
            }, BorderLayout.CENTER)
        })
    }

    fun reset() {
        colorPanel.selectedColor = Const.colorMap[tempProps.ansiCode]
        comboBox.selectedItem = tempProps.colorName
        selectBox.isSelected = tempProps.enabled
    }
}