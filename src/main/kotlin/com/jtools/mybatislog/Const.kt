package com.jtools.mybatislog

import com.intellij.ui.Gray
import java.awt.Color
import javax.swing.JComponent

data object Const {
    val ansiColorMap = mutableMapOf<String, String>()

    val colorMap = mutableMapOf<String, Color>()



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
        colorMap["90"] = Gray._85 // 亮黑色
        colorMap["91"] = Color(255, 85, 85) // 亮红色
        colorMap["92"] = Color(85, 255, 85) // 亮绿色
        colorMap["93"] = Color(255, 255, 85) // 亮黄色
        colorMap["94"] = Color(85, 85, 255) // 亮蓝色
        colorMap["95"] = Color(255, 85, 255) // 亮紫色
        colorMap["96"] = Color(85, 255, 255) // 亮青色
        colorMap["97"] = Gray._255 // 亮白色
    }
}