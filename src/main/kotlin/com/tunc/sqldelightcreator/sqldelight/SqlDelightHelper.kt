package com.tunc.sqldelightcreator.sqldelight

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JCheckBox
import javax.swing.JTextField

object SqlDelightHelper {
    fun createTable(
            tableName: String,
            inputList: MutableList<JTextField>,
            comboBoxes: MutableList<ComboBox<String>>,
            nullableCheckBoxes: MutableList<JCheckBox>,
            primaryCheckBoxes: MutableList<JCheckBox>
    ): String {

        if (tableName.isEmpty()) {
            return ValidationInfo("Table name cannot be empty").message
        }

        val query = StringBuilder()
        query.append("CREATE TABLE IF NOT EXISTS $tableName (")


        inputList.forEachIndexed { index, jTextField ->
            val nullable = if (nullableCheckBoxes[index].isSelected) " NOT NULL" else ""
            val primaryKey = if (primaryCheckBoxes[index].isSelected) " PRIMARY KEY AUTOINCREMENT" else ""
            query.append("\n" + jTextField.text + " " + comboBoxes[index].selectedItem!!.toString() + primaryKey + "$nullable,")

        }
        query.deleteCharAt(query.lastIndex)

        query.append("\n" + ");")

        query.append("\n\n" + insertItem(tableName, inputList))
        query.append("\n\n" + get(tableName))
        query.append("\n\n" + getAll(tableName))
        query.append("\n\n" + delete(tableName))
        query.append("\n\n" + deleteAll(tableName))


        return query.toString().trimEnd()
    }

    private fun insertItem(tableName: String, inputList: MutableList<JTextField>): String {


        val query = StringBuilder()
        query.append("insert_${tableName}:\nINSERT INTO $tableName (")

        inputList.forEachIndexed { index, jTextField ->
            if (index == inputList.size - 1) {
                query.append(jTextField.text + ")")
            } else {
                query.append(jTextField.text + ", ")
            }
        }

        query.append("\nVALUES (")

        inputList.forEachIndexed { index, jTextField ->
            if (index == inputList.size - 1) {
                query.append("?);")
            } else {
                query.append("?, ")
            }
        }

        return query.toString()

    }

    private fun get(tableName: String): String {
        return "get_${tableName}:\nSELECT * FROM $tableName WHERE id = ?;"
    }

    private fun getAll(tableName: String): String {
        return "select_all_${tableName}:\nSELECT * FROM $tableName;"
    }

    private fun delete(tableName: String): String {
        return "delete_${tableName}:\nDELETE FROM $tableName WHERE id = ?;"
    }


    private fun deleteAll(tableName: String): String {
        return "delete_all_${tableName}:\nDELETE FROM $tableName;"
    }


}