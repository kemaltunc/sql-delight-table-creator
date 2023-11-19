package com.tunc.sqldelightcreator

import com.tunc.sqldelightcreator.sqldelight.SqlDelightHelper
import com.tunc.sqldelightcreator.sqldelight.getVariableTypes
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import javax.swing.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.awt.*


class SqlDelightAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val module: Module? = findModuleForFile(project, e.dataContext)
        val selectedDirectory: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (module != null && selectedDirectory != null) {
            val moduleDialog = ModuleDialog(project, module, selectedDirectory)
            moduleDialog.show()
        } else {
            Messages.showErrorDialog("Couldn't determine module or selected directory.", "Error")
        }
    }

    private fun findModuleForFile(project: Project?, dataContext: DataContext): Module? {
        val virtualFile = LangDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return null
        return ProjectRootManager.getInstance(project!!).fileIndex.getModuleForFile(virtualFile)
    }

    private class ModuleDialog(
            private val project: Project?,
            private val module: Module,
            private val selectedDirectory: VirtualFile
    ) : DialogWrapper(true) {
        private val inputRows: MutableList<JPanel> = ArrayList()
        private val inputFields: MutableList<JTextField> = ArrayList()
        private val notNullCheckBoxes: MutableList<JCheckBox> = ArrayList()
        private val primaryKeyCheckBoxes: MutableList<JCheckBox> = ArrayList()
        private val comboBoxes: MutableList<ComboBox<String>> = ArrayList()
        private val mainPanel: JPanel
        private val dynamicInputPanel: JPanel
        private var tableNameTextField: JTextField? = null


        init {
            title = "SQLDelight Table Creator"
            mainPanel = JPanel(BorderLayout())

            dynamicInputPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            createInitialInputRow()

            mainPanel.add(dynamicInputPanel, BorderLayout.CENTER)

            init()
        }

        private fun createInitialInputRow() {
            val tableNameRowPanel = JPanel()
            tableNameTextField = JTextField()

            tableNameTextField!!.toolTipText = "Enter table name"

            tableNameTextField!!.preferredSize = Dimension(200, 30)
            tableNameRowPanel.add(JLabel("Table Name:"))
            tableNameRowPanel.add(tableNameTextField)

            dynamicInputPanel.add(tableNameRowPanel)

            createDynamicInputButton()
        }

        private fun createDynamicInputButton() {
            val addButton = JButton("Add Input")
            addButton.addActionListener { _ ->
                addInputRow()

                pack()
            }

            dynamicInputPanel.add(addButton)
        }

        private fun addInputRow() {
            val rowPanel = JPanel()
            val fieldNameTextField = JTextField()
            val fieldTypeComboBox = ComboBox<String>()
            val nullableCheckBox = JCheckBox("Not Null").apply {
                isSelected = true
            }
            val primaryKeyCheckBox = JCheckBox("PRIMARY KEY AUTOINCREMENT")
            fieldTypeComboBox.model = DefaultComboBoxModel(getVariableTypes())

            fieldNameTextField.toolTipText = "Enter field name"

            fieldNameTextField.preferredSize = Dimension(200, 30)

            rowPanel.add(JLabel("Field ${inputRows.size + 1}:"))
            rowPanel.add(fieldNameTextField)
            rowPanel.add(fieldTypeComboBox)
            rowPanel.add(nullableCheckBox)
            rowPanel.add(primaryKeyCheckBox)

            inputRows.add(rowPanel)
            inputFields.add(fieldNameTextField)
            comboBoxes.add(fieldTypeComboBox)
            notNullCheckBoxes.add(nullableCheckBox)
            primaryKeyCheckBoxes.add(primaryKeyCheckBox)

            dynamicInputPanel.add(rowPanel)

        }

        override fun createCenterPanel(): JComponent {
            mainPanel.preferredSize = Dimension(800, 900)

            return mainPanel
        }

        override fun doOKAction() {

            val validationInfo = validateInputs()
            if (validationInfo != null) {
                Messages.showErrorDialog(validationInfo.message, "Error")
                return
            }

            val resultMessage = processUserInputs()

            val tableName = tableNameTextField!!.text


            val outputPath = "${selectedDirectory.path}/$tableName.sq"

            try {
                writeToFile(outputPath, resultMessage)
                Messages.showMessageDialog("Data successfully written to $outputPath", "Success", Messages.getInformationIcon())

                openFile(outputPath)
            } catch (ex: IOException) {
                ex.printStackTrace()
                Messages.showErrorDialog("Error writing to file: ${ex.message}", "Error")
            }

            super.doOKAction()
        }

        private fun validateInputs(): ValidationInfo? {
            val tableName = inputFields[0].text
            if (tableName.isEmpty()) {
                return ValidationInfo("Table name cannot be empty", inputFields[0])
            }

            for (i in 1 until inputFields.size) {
                val fieldName = inputFields[i].text
                if (fieldName.isEmpty()) {
                    return ValidationInfo("Field name in row ${i + 1} cannot be empty", inputFields[i])
                }
            }

            return null
        }

        private fun processUserInputs(): String {
            return SqlDelightHelper.createTable(tableNameTextField!!.text, inputFields, comboBoxes, notNullCheckBoxes, primaryKeyCheckBoxes)
        }

        @Throws(IOException::class)
        private fun writeToFile(fileName: String, content: String) {
            BufferedWriter(FileWriter(fileName)).use { writer ->
                writer.write(content)
            }
        }

        private fun openFile(fileName: String) {
            try {
                val file = LocalFileSystem.getInstance().findFileByPath(fileName)
                if (file != null && file.isValid) {
                    val descriptor = OpenFileDescriptor(project!!, file)
                    val editorManager = FileEditorManager.getInstance(project!!)
                    editorManager.openTextEditor(descriptor, true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Messages.showErrorDialog("Error opening file: ${e.message}", "Error")
            }
        }
    }

}