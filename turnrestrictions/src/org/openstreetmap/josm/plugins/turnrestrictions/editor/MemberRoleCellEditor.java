// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;

import java.awt.Component;
import java.util.Arrays;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompTextField;

/**
 * The cell editor for member roles of relation members in a turn restriction.
 *
 */
public class MemberRoleCellEditor extends AbstractCellEditor implements TableCellEditor {
    //private static Logger logger = Logger.getLogger(MemberRoleCellEditor.class.getName());

    private AutoCompTextField<String> editor = null;

    /**
     * constructor
     */
    public MemberRoleCellEditor() {
        editor = new AutoCompTextField<>(0, false);
        editor.getModel().addAllElements(Arrays.asList("from", "via", "to"));
    }

    /**
     * replies the table cell editor
     */
    @Override
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        String role = (String) value;
        editor.setText(role);
        return editor;
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getText();
    }
}
