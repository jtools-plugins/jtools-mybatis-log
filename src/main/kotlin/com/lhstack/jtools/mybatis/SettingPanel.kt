package com.lhstack.jtools.mybatis

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI.Borders
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.collections.get

class SettingPanel(val project: Project, val tempProps: TempProps, updated: (TempProps) -> Unit = {}) : JPanel(),
    Disposable {

    val selectBox = JBCheckBox()
    val change = AtomicBoolean(true)
    val colorPanel = ColorPanel().apply {
        this.setEditable(false)
        this.selectedColor = Const.colorMap[tempProps.ansiCode]
    }
    val configJsonPathField = JTextField(tempProps.configJsonPath).also {
        it.preferredSize = Dimension(200, 35)
        it.toolTipText = tempProps.configJsonPath
        it.isEditable = false
    }

    var propertiesField: MultiLanguageTextField

    val comboBox = ComboBox<String>(Const.ansiColorMap.keys.toTypedArray())

    init {
        val fileType = FileTypeManager.getInstance().findFileTypeByName("Properties")
        propertiesField =
            object : MultiLanguageTextField(fileType as LanguageFileType, project, tempProps.configJsonValue, true) {
                override fun getPreferredSize(): Dimension {
                    val dimension = super.getPreferredSize()
                    return Dimension(dimension.width, 300)
                }
            }.also {
                it.addDocumentListener(object: DocumentListener{
                    override fun beforeDocumentChange(event: DocumentEvent?) {

                    }

                    override fun documentChanged(event: DocumentEvent) {
                        if(change.get()) {
                            tempProps.configJsonValue = event.document.text
                            updated(tempProps)
                        }
                    }
                })
                it.border = Borders.emptyTop(10)
            }
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

//        this.add(JPanel(BorderLayout()).apply {
//            this.add(JLabel("配置文件路径: ", JLabel.LEFT), BorderLayout.WEST)
//            this.add(JPanel(HorizontalLayout()).apply {
//                val textFieldWithBrowseButton = TextFieldWithBrowseButton(configJsonPathField) {
//                    var file =
//                        VirtualFileManager.getInstance().findFileByUrl("file://" + tempProps.configJsonPath)
//                    var chooseFile = FileChooser.chooseFile(
//                        FileChooserDescriptor(true, false, false, false, false, false)
//                            .withFileFilter { it.extension == "properties" || it.extension == "txt" },
//                        project,
//                        file
//                    )
//                    if(chooseFile != null){
//                        configJsonPathField.text = chooseFile.path
//                        tempProps.configJsonPath = chooseFile.path
//                        updated.invoke(tempProps)
//                    }
//                }
//                this.add(textFieldWithBrowseButton)
//            }, BorderLayout.CENTER)
//        })
        this.add(JPanel(BorderLayout()).apply {
            this.add(JPanel(HorizontalLayout()).also {
                it.add(JLabel("配置内容: ", JLabel.LEFT))
                it.add(JButton("模板").also {
                    it.addActionListener {
                        change.set(false)
                        propertiesField.text = """
                            # 排除的包名,Use Spring AntPathMatcher 多个用,隔开
                            excludePackages=a.b.*,c.d.*,a.**.c.d
                            # sql格式化类型,支持: Db2,MariaDb,MySql,N1ql,PlSql,PostgreSql,Redshift,SparkSql,StandardSql,TSql
                            sqlFormatType=Mysql
                        """.trimIndent()
                        change.set(true)
                    }
                })
            }, BorderLayout.NORTH)
            this.add(propertiesField, BorderLayout.CENTER)
        })
    }

    fun reset() {
        colorPanel.selectedColor = Const.colorMap[tempProps.ansiCode]
        comboBox.selectedItem = tempProps.colorName
        selectBox.isSelected = tempProps.enabled
        configJsonPathField.text = tempProps.configJsonPath
        propertiesField.text = tempProps.configJsonValue
    }

    override fun dispose() {
        Disposer.dispose(propertiesField)
    }
}