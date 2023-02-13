// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.tageditor.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagTable;
import org.openstreetmap.josm.gui.tagging.TagTableModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.plugins.tageditor.ac.IAutoCompletionListListener;

/**
 * TagEditor is a {@link JPanel} which consists of a two sub panels:
 * <ul>
 *   <li>a small area in which a drop-down box with a list of presets is displayed.
 *       Two buttons allow to highlight and remove a presets respectively.</li>
 *   <li>the main table with the tag names and tag values
 * </ul>
 *
 *  This component depends on a couple of other components which have to be
 *  injected with the respective setter methods:
 *  <ul>
 *    <li>an instance of {@link TagEditorModel} - use {@see #setTagEditorModel(TagEditorModel)}</li>
 *    <li>an instance of {@link AutoCompletionCache} - inject it using {@see #setAutoCompletionCache(AutoCompletionCache)}.
 *      The table cell editor used by the table in this component uses the AutoCompletionCache in order to
 *      build up a list of auto completion values from the current data set</li>
 *    <li>an instance of {@link AutoCompletionList} - inject it using {@see #setAutoCompletionList(AutoCompletionList)}.
 *      The table cell editor used by the table in this component uses the AutoCompletionList
 *      to build up a list of auto completion values from the set of  standardized
 *      OSM tags</li>
 *  </ul>O
 *
 *  Typical usage is therefore:
 *  <pre>
 *     AutoCompletionList autoCompletionList = .... // init the autocompletion list
 *     AutoCompletionCache autoCompletionCache = ... // init the auto completion cache
 *     TagEditorModel model = ... // init the tag editor model
 *
 *     TagEditor tagEditor = new TagEditor();
 *     tagEditor.setTagEditorModel(model);
 *     tagEditor.setAutoCompletionList(autoCompletionList);
 *     tagEditor.setAutoCompletionCache(autoCompletionCache);
 *  </pre>
 */
public class TagEditor extends JPanel implements IAutoCompletionListListener {

    private static final Logger logger = Logger.getLogger(TagEditor.class.getName());

    private TagEditorModel tagEditorModel;
    private TagTable tblTagEditor;
    private PresetManager presetManager;

    /**
     * builds the GUI
     *
     */
    protected void build() {
        setLayout(new BorderLayout());

        add(new TagEditorPanel(tagEditorModel, 0), BorderLayout.CENTER);

        // build the preset manager which shows a list of applied presets
        //
        presetManager = new PresetManager();
        presetManager.setModel(tagEditorModel);
        add(presetManager, BorderLayout.NORTH);
    }

    /**
     * constructor
     */
    public TagEditor() {
        build();
    }

    /**
     * replies the tag editor model
     * @return the tag editor model
     */
    public TagTableModel getTagEditorModel() {
        return tagEditorModel;
    }

    public void clearSelection() {
        tblTagEditor.getSelectionModel().clearSelection();
    }

    public void stopEditing() {
        TableCellEditor editor = tblTagEditor.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }

    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        tblTagEditor.setAutoCompletionList(autoCompletionList);
    }

    public void setAutoCompletionManager(AutoCompletionManager autocomplete) {
        tblTagEditor.setAutoCompletionManager(autocomplete);
    }

    @Override
    public void autoCompletionItemSelected(String item) {
        logger.info("autocompletion item selected ...");
        TagSpecificationAwareTagCellEditor editor = (TagSpecificationAwareTagCellEditor) tblTagEditor.getCellEditor();
        if (editor != null) {
            editor.autoCompletionItemSelected(item);
        }
    }

    public void requestFocusInTopLeftCell() {
        tblTagEditor.requestFocusInCell(0, 0);
    }

    public TagTableModel getModel() {
        return tagEditorModel;
    }
}
