package com.lhstack.jtools.mybatis

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiClassOwner
import com.intellij.ui.ColorPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ItemRemovable
import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.table.DefaultTableModel

class SettingPanel(val project: Project, val tempProps: TempProps, val updated: (TempProps) -> Unit = {}) : JPanel(),
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

    val comboBox = ComboBox<String>(Const.ansiColorMap.keys.toTypedArray())

    private val sqlFormatComboBox = ComboBox(
        arrayOf(
            "Db2",
            "MariaDb",
            "MySql",
            "N1ql",
            "PlSql",
            "PostgreSql",
            "Redshift",
            "SparkSql",
            "StandardSql",
            "TSql"
        )
    )
    private val sqlFormatEnableCheckBox = JBCheckBox()
    private val excludeTableModel = object : DefaultTableModel(arrayOf("排除的包/类"), 0), ItemRemovable {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }
    private val excludeTable = JBTable(excludeTableModel)

    init {
        // Initialize UI components first
        sqlFormatComboBox.addItemListener {
            if (change.get()) {
                saveConfig()
            }
        }
        sqlFormatEnableCheckBox.addActionListener {
            if (change.get()) {
                sqlFormatComboBox.isEnabled = sqlFormatEnableCheckBox.isSelected
                saveConfig()
            }
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

        // SQL Format Configuration
        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("开启SQL格式化打印: ", JLabel.LEFT), BorderLayout.WEST)
            this.add(sqlFormatEnableCheckBox, BorderLayout.CENTER)
        })
        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("SQL格式化类型: ", JLabel.LEFT), BorderLayout.WEST)
            this.add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                this.add(sqlFormatComboBox)
            }, BorderLayout.CENTER)
        })

        // Exclude Packages/Classes Table
        this.add(JPanel(BorderLayout()).apply {
            this.add(JLabel("排除包/类 (Exclude Packages/Classes): ", JLabel.LEFT), BorderLayout.NORTH)
            val decorator = ToolbarDecorator.createDecorator(excludeTable)
            decorator.setAddAction {
                // Show popup to choose Package or Class
                val popup = JBPopupFactory.getInstance()
                    .createListPopup(object : BaseListPopupStep<String>(
                        "选择类型 (Select Type)",
                        listOf(
                            "添加包 (Add Package)",
                            "添加包 (从类导入/From Class)",
                            "添加类 (Add Class)",
                            "添加自定义 (Add Custom)"
                        )
                    ) {
                        override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                            ApplicationManager.getApplication().invokeLater {
                                if (selectedValue == "添加包 (Add Package)") {
                                    val chooser = PackageChooserDialog("选择包 (Select Package)", project)
                                    if (chooser.showAndGet()) {
                                        chooser.selectedPackages.forEach { pkg ->
                                            addExcludeItem(pkg.qualifiedName + ".*")
                                        }
                                    }
                                } else if (selectedValue == "添加包 (从类导入/From Class)") {
                                    val chooser = TreeClassChooserFactory.getInstance(project)
                                        .createAllProjectScopeChooser("选择类以导入包 (Select Class to Import Package)")
                                    chooser.showDialog()
                                    val selectedClass = chooser.selected
                                    if (selectedClass != null) {
                                        // Extract package name
                                        val packageName = (selectedClass.containingFile as? PsiClassOwner)?.packageName
                                            ?: selectedClass.qualifiedName?.substringBeforeLast('.')
                                        if (!packageName.isNullOrEmpty()) {
                                            addExcludeItem(packageName + ".*")
                                        }
                                    }
                                } else if (selectedValue == "添加类 (Add Class)") {
                                    val chooser = TreeClassChooserFactory.getInstance(project)
                                        .createAllProjectScopeChooser("选择类 (Select Class)")
                                    chooser.showDialog()
                                    val selectedClass = chooser.selected
                                    if (selectedClass != null) {
                                        addExcludeItem(selectedClass.qualifiedName)
                                    }
                                } else if (selectedValue == "添加自定义 (Add Custom)") {
                                    val input = Messages.showInputDialog(
                                        project,
                                        "输入包名或类名 (Enter package or class name):",
                                        "添加自定义 (Add Custom)",
                                        null
                                    )
                                    if (!input.isNullOrEmpty()) {
                                        addExcludeItem(input)
                                    }
                                }
                            }
                            return FINAL_CHOICE
                        }
                    })
                popup.showInCenterOf(excludeTable)
            }
            decorator.setRemoveAction {
                val selectedRows = excludeTable.selectedRows
                if (selectedRows.isNotEmpty()) {
                    // Remove in reverse order
                    for (i in selectedRows.indices.reversed()) {
                        excludeTableModel.removeRow(selectedRows[i])
                    }
                    saveConfig()
                }
            }
            this.add(decorator.createPanel(), BorderLayout.CENTER)
        })

        // Load initial data
        loadConfig()
    }

    private fun addExcludeItem(item: String?) {
        if (!item.isNullOrEmpty()) {
            // Check for duplicates
            for (i in 0 until excludeTableModel.rowCount) {
                if (excludeTableModel.getValueAt(i, 0) == item) {
                    return
                }
            }
            excludeTableModel.addRow(arrayOf(item))
            saveConfig()
        }
    }

    private fun loadConfig() {
        change.set(false)
        try {
            val props = Properties()
            if (tempProps.configJsonValue.isNotEmpty()) {
                props.load(StringReader(tempProps.configJsonValue))
            }

            // Load excludePackages
            val excludePackages = props.getProperty("excludePackages", "")
            excludeTableModel.rowCount = 0 // Clear existing
            if (excludePackages.isNotEmpty()) {
                excludePackages.split(",").forEach {
                    if (it.isNotBlank()) {
                        excludeTableModel.addRow(arrayOf(it.trim()))
                    }
                }
            }

            // Load sqlFormatType
            val sqlFormat = props.getProperty("sqlFormatType", "MySql")
            sqlFormatComboBox.selectedItem = sqlFormat
            val sqlFormatEnable = props.getProperty("sqlFormatEnable", "true").toBoolean()
            sqlFormatEnableCheckBox.isSelected = sqlFormatEnable
            sqlFormatComboBox.isEnabled = sqlFormatEnable

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            change.set(true)
        }
    }

    private fun saveConfig() {
        if (!change.get()) return

        try {
            val props = Properties()
            // Load existing properties to preserve other keys
            if (tempProps.configJsonValue.isNotEmpty()) {
                props.load(StringReader(tempProps.configJsonValue))
            }

            // Update excludePackages
            val excludes = StringBuilder()
            for (i in 0 until excludeTableModel.rowCount) {
                if (excludes.isNotEmpty()) {
                    excludes.append(",")
                }
                excludes.append(excludeTableModel.getValueAt(i, 0))
            }
            props.setProperty("excludePackages", excludes.toString())

            // Update sqlFormatType
            props.setProperty("sqlFormatType", sqlFormatComboBox.selectedItem as String)
            props.setProperty("sqlFormatEnable", sqlFormatEnableCheckBox.isSelected.toString())

            val writer = StringWriter()
            props.store(writer, "Configuration")
            tempProps.configJsonValue = writer.toString()
            updated(tempProps)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        colorPanel.selectedColor = Const.colorMap[tempProps.ansiCode]
        comboBox.selectedItem = tempProps.colorName
        selectBox.isSelected = tempProps.enabled
        configJsonPathField.text = tempProps.configJsonPath

        loadConfig()
    }

    override fun dispose() {
        // No specific resources to dispose for now
    }
}
