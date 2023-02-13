// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.editor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.EnumSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagTable;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBox;
import org.openstreetmap.josm.gui.tagging.ac.TagTableUtils;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * AdvancedEditorPanel consists of two advanced editors for parts of the turn
 * restriction data: a tag editor and a relation member editor.
 */
public class AdvancedEditorPanel extends JPanel {

    private TurnRestrictionEditorModel model;
    private TagEditorPanel pnlTagEditor;
    private JTable tblRelationMemberEditor;
    private JSplitPane spEditors;

    /**
     * Creates the panel with the tag editor
     */
    protected JPanel buildTagEditorPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        HtmlPanel msg = new HtmlPanel();
        msg.setText("<html><body>" +
                tr("In the following table you can edit the <strong>raw tags</strong>"
              + " of the OSM relation representing this turn restriction.")
              + "</body></html>"
        );
        pnl.add(msg, BorderLayout.NORTH);
        pnlTagEditor = new TagEditorPanel(model.getTagTableModel(), 0);

        TagTableUtils tagTableUtils = new TagTableUtils(model.getTagTableModel(), this::getContextKey);
        tagTableUtils.setTypes(EnumSet.of(TaggingPresetType.RELATION));

        // setting up the tag table
        AutoCompComboBox<AutoCompletionItem> keyEditor = tagTableUtils.getKeyEditor(null);
        AutoCompComboBox<AutoCompletionItem> valueEditor = tagTableUtils.getValueEditor(null);

        TagTable tagTable = pnlTagEditor.getTable();
        tagTable.setKeyEditor(keyEditor);
        tagTable.setValueEditor(valueEditor);
        tagTable.setRowHeight(keyEditor.getEditorComponent().getPreferredSize().height);

        pnl.add(pnlTagEditor, BorderLayout.CENTER);
        return pnl;
    }

    String getContextKey() {
        TagTable tagTable = pnlTagEditor.getTable();
        int row = tagTable.getEditingRow();
        if (row == -1)
            row = tagTable.getSelectedRow();
        return tagTable.getKey(row);
    }

    /**
     * Builds the panel with the table for editing relation members
     */
    protected JPanel buildMemberEditorPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        HtmlPanel msg = new HtmlPanel();
        msg.setText("<html><body>"
              + tr("In the following table you can edit the <strong>raw members</strong>"
              + " of the OSM relation representing this turn restriction.") + "</body></html>"
        );
        pnl.add(msg, BorderLayout.NORTH);

        tblRelationMemberEditor = new RelationMemberTable(model);
        TableHelper.setRowHeight(tblRelationMemberEditor);
        JScrollPane pane = new JScrollPane(tblRelationMemberEditor);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pnl.add(pane);
        return pnl;
    }

    /**
     * Creates the main split panel
     */
    protected JSplitPane buildSplitPane() {
        spEditors = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        spEditors.setTopComponent(buildTagEditorPanel());
        spEditors.setBottomComponent(buildMemberEditorPanel());
        spEditors.setOneTouchExpandable(false);
        spEditors.setDividerSize(5);
        spEditors.addHierarchyListener(new SplitPaneDividerInitializer());
        return spEditors;
    }

    /**
     * Builds the user interface
     */
    protected void build() {
        setLayout(new BorderLayout());
        add(buildSplitPane(), BorderLayout.CENTER);
    }

    /**
     * Creates the advanced editor
     *
     * @param model the editor model. Must not be null.
     * @throws IllegalArgumentException thrown if model is null
     */
    public AdvancedEditorPanel(TurnRestrictionEditorModel model) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(model, "model");
        this.model = model;
        build();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Plugin/TurnRestrictions#AdvancedEditor"));
    }

    /**
     * Initializes the divider location when the components becomes visible the
     * first time
     */
    class SplitPaneDividerInitializer implements HierarchyListener {
        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            if (isShowing()) {
                spEditors.setDividerLocation(0.5);
                spEditors.removeHierarchyListener(this);
            }
        }
    }
}
