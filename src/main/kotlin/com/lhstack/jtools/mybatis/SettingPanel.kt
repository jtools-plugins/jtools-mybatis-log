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
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import java.io.StringReader
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.UIManager
import javax.swing.table.DefaultTableModel

class SettingPanel(val project: Project, val tempProps: TempProps, val updated: (TempProps) -> Unit = {}) : JPanel(),
    Disposable {

    val selectBox = JBCheckBox()
    val change = AtomicBoolean(true)
    val colorPanel = ColorPanel().apply {
        this.setEditable(false)
        this.selectedColor = Const.colorMap[tempProps.ansiCode]
        this.preferredSize = Dimension(72, 24)
        this.minimumSize = preferredSize
        this.maximumSize = preferredSize
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
    private val sqlFormatEnableCheckBox = JBCheckBox("格式化 SQL")
    /** 记录每种 SQL 类型是否需要从控制台日志中排除。 */
    private val excludeSqlTypeCheckBoxes = linkedMapOf(
        "SELECT" to JBCheckBox("SELECT"),
        "INSERT" to JBCheckBox("INSERT"),
        "UPDATE" to JBCheckBox("UPDATE"),
        "DELETE" to JBCheckBox("DELETE")
    )
    private val excludeTableModel = object : DefaultTableModel(arrayOf("排除的包/类"), 0), ItemRemovable {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }
    private val excludeTable = JBTable(excludeTableModel)

    init {
        // Initialize UI components first
        sqlFormatComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED && change.get()) {
                saveConfig()
            }
        }
        sqlFormatEnableCheckBox.addActionListener {
            if (change.get()) {
                sqlFormatComboBox.isEnabled = sqlFormatEnableCheckBox.isSelected
                saveConfig()
            }
        }
        // SQL 类型过滤选项变化后立即同步到临时配置,Apply 时由设置页统一写入文件。
        excludeSqlTypeCheckBoxes.values.forEach { checkBox ->
            checkBox.addActionListener {
                if (change.get()) {
                    saveConfig()
                }
            }
        }

        layout = BorderLayout()
        border = JBUI.Borders.empty()

        selectBox.apply {
            text = "输出完整 SQL 和执行耗时"
            isSelected = tempProps.enabled
            addActionListener {
                tempProps.enabled = isSelected
                updated.invoke(tempProps)
            }
        }
        comboBox.selectedItem = tempProps.colorName
        comboBox.addItemListener { e ->
            if (e.stateChange != ItemEvent.SELECTED) {
                return@addItemListener
            }
            val colorName = e.item as String
            val ansiCode = Const.ansiColorMap[colorName] ?: return@addItemListener
            tempProps.colorName = colorName
            tempProps.ansiCode = ansiCode
            colorPanel.selectedColor = Const.colorMap[ansiCode]
            updated.invoke(tempProps)
        }

        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(12, 16, 16, 16)
            alignmentX = LEFT_ALIGNMENT
        }
        content.add(section("输出", "控制是否打印以及日志颜色") {
            add(optionRow("启用日志", selectBox))
            add(optionRow("日志颜色", inline(comboBox, colorPanel)))
        })
        content.add(Box.createVerticalStrut(12))

        content.add(section("格式化", "PlSql 对应 Oracle 语法，分页会输出 ROWNUM") {
            add(optionRow("启用格式化", sqlFormatEnableCheckBox))
            add(optionRow("SQL 方言", sqlFormatComboBox))
        })
        content.add(Box.createVerticalStrut(12))

        content.add(section("过滤", "勾选后对应类型的 SQL 不输出") {
            add(optionRow("排除类型", inline(*excludeSqlTypeCheckBoxes.values.toTypedArray())))
        })
        content.add(Box.createVerticalStrut(12))

        content.add(section("排除范围", "匹配到的包或类不会生成 SQL 日志") {
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
            add(decorator.createPanel())
        })

        add(JScrollPane(content).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            viewport.background = content.background
        }, BorderLayout.CENTER)

        loadConfig()
    }

    private fun mutedColor(): java.awt.Color = UIManager.getColor("Label.disabledForeground") ?: java.awt.Color.GRAY

    private fun section(title: String, description: String, content: JPanel.() -> Unit): JPanel {
        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = LEFT_ALIGNMENT
            isOpaque = false
            border = JBUI.Borders.empty(10, 12, 12, 12)
            content()
        }
        return JPanel(BorderLayout()).apply {
            alignmentX = LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(mutedColor()),
                JBUI.Borders.empty()
            )
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(8, 12, 8, 12)
                add(JLabel(title).apply {
                    font = font.deriveFont(font.style or java.awt.Font.BOLD)
                }, BorderLayout.WEST)
                add(JLabel(description).apply {
                    foreground = mutedColor()
                    border = JBUI.Borders.emptyLeft(12)
                }, BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
    }

    private fun optionRow(label: String, control: java.awt.Component): JPanel {
        return JPanel(GridBagLayout()).apply {
            alignmentX = LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 32)
            isOpaque = false
            border = JBUI.Borders.empty(3, 0)
            add(JLabel(label).apply {
                preferredSize = Dimension(88, 24)
            }, GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
            })
            add(control, GridBagConstraints().apply {
                gridx = 1
                gridy = 0
                weightx = 1.0
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.NONE
            })
        }
    }

    private fun inline(vararg components: java.awt.Component): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            components.forEachIndexed { index, component ->
                if (index > 0) {
                    add(Box.createHorizontalStrut(12))
                }
                add(component)
            }
        }
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

            // 根据配置文件恢复需要排除的 SQL 类型,未知类型不会影响界面加载。
            val excludeSqlTypes = props.getProperty("excludeSqlTypes", "")
                .split(",")
                .map { it.trim().uppercase(Locale.ROOT) }
                .filter { it.isNotEmpty() }
                .toSet()
            excludeSqlTypeCheckBoxes.forEach { (sqlType, checkBox) ->
                checkBox.isSelected = sqlType in excludeSqlTypes
            }

        } catch (e: Exception) {
            Notifier.warn(project, "解析配置内容失败,已按默认值展示: ${e.message}")
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

            // 仅保存界面支持的 SQL 类型,并使用统一的大写格式供 Agent 解析。
            val excludeSqlTypes = excludeSqlTypeCheckBoxes
                .filterValues { it.isSelected }
                .keys
                .joinToString(",")
            props.setProperty("excludeSqlTypes", excludeSqlTypes)

            tempProps.configJsonValue = renderProperties(props)
            updated(tempProps)

        } catch (e: Exception) {
            Notifier.warn(project, "生成配置内容失败: ${e.message}")
        }
    }

    /**
     * 手写序列化而不用 Properties.store: 后者每次都会写入当前时间戳注释,
     * 导致内容实质未变时 isModified 仍为 true,Apply 按钮会无故亮起。
     */
    private fun renderProperties(props: Properties): String {
        val builder = StringBuilder()
        props.stringPropertyNames().sorted().forEach { key ->
            builder.append(key).append('=').append(props.getProperty(key)).append('\n')
        }
        return builder.toString()
    }

    fun reset() {
        colorPanel.selectedColor = Const.colorMap[tempProps.ansiCode]
        comboBox.selectedItem = tempProps.colorName
        selectBox.isSelected = tempProps.enabled
        loadConfig()
    }

    override fun dispose() {
        // No specific resources to dispose for now
    }
}
