// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.tageditor.editor;

import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.TagTableModel;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.plugins.tageditor.preset.AdvancedTag;

@SuppressWarnings("serial")
public class TagEditorModel extends TagTableModel {
    //static private final Logger logger = Logger.getLogger(TagEditorModel.class.getName());

    private DefaultComboBoxModel<TaggingPreset> appliedPresets = null;

    /**
     * constructor
     */
    public TagEditorModel(DefaultListSelectionModel rowSelectionModel, DefaultListSelectionModel colSelectionModel) {
        super(null);
        appliedPresets = new DefaultComboBoxModel<>();
    }

    /**
     * applies the tags defined for a preset item to the tag model.
     *
     * Mandatory tags are added to the list of currently edited tags.
     * Optional tags are not added.
     * The model remembers the currently applied presets.
     *
     * @param item  the preset item. Must not be null.
     * @exception IllegalArgumentException thrown, if item is null
     *
     */
    public void applyPreset(TaggingPreset item) {
        if (item == null)
            throw new IllegalArgumentException("argument 'item' must not be null");
        // check whether item is already applied
        //
        for (int i = 0; i < appliedPresets.getSize(); i++) {
            if (appliedPresets.getElementAt(i).equals(item))
                // abort - preset already applied
                return;
        }

        // apply the tags proposed by the preset
        //
        for (AdvancedTag tag : AdvancedTag.forTaggingPreset(item)) {
            if (!tag.isOptional()) {
                String key = tag.getKey();
                String value = tag.getValue();
                if (get(key) == null) {
                    put(key, value);
                } else {
                    // only overwrite an existing value if the preset
                    // proposes a value. I.e. don't overwrite
                    // existing values for tag 'name' with an empty string
                    //
                    if (value != null) {
                        put(key, value);
                    }
                }
            }
        }

        // remember the preset and make it the current preset
        //
        appliedPresets.addElement(item);
        appliedPresets.setSelectedItem(item);
        fireTableDataChanged();
    }


    /**
     * applies a tag given by a {@see Tag} to the model
     *
     * @param pair the key value pair
     */
    public void applyKeyValuePair(Tag pair) {
        put(pair.getKey(), pair.getValue());
        fireTableDataChanged();
    }

    public DefaultComboBoxModel<TaggingPreset> getAppliedPresetsModel() {
        return appliedPresets;
    }

    public void removeAppliedPreset(TaggingPreset item) {
        if (item == null)
            return;
        for (AdvancedTag tag: AdvancedTag.forTaggingPreset(item)) {
            String key = tag.getKey();
            String value = tag.getValue();
            // Remove if the value is null or the value is known
            if (value != null && value.equals(get(key).toString())) {
                value = null;
            }
            if (value == null)
                put(key, null);
        }
        appliedPresets.removeElement(item);
        fireTableDataChanged();
    }

    public void clearAppliedPresets() {
        appliedPresets.removeAllElements();
        fireTableDataChanged();
    }

    public void highlightCurrentPreset() {
        fireTableDataChanged();
    }

    /**
     * updates the tags of the primitives in the current selection with the
     * values in the current tag model
     *
     */
    public void updateJOSMSelection() {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        Collection<OsmPrimitive> selection = dataSet.getSelected();
        if (selection == null)
            return;
        Command command = new ChangePropertyCommand(dataSet, selection, getTags());

        // executes the commands and adds them to the undo/redo chains
        UndoRedoHandler.getInstance().add(command);
    }

    /**
     * initializes the model with the tags in the current JOSM selection
     */
    public void initFromJOSMSelection() {
        Collection<OsmPrimitive> selection = MainApplication.getLayerManager().getEditDataSet().getSelected();
        clear();
        for (OsmPrimitive element : selection) {
            for (String key : element.keySet()) {
                String value = element.get(key);
                put(key, value);
            }
        }
    }
}
