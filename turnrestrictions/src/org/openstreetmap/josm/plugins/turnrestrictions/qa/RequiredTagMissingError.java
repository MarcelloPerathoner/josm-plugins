// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.qa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.tagging.TagTableModel;

/**
 * Issue if a required tag is missing in the current turn restriction.
 */
public class RequiredTagMissingError extends Issue {
    //private static final Logger logger = Logger.getLogger(RequiredTagMissingError.class.getName());
    private String tagKey;
    private String tagValue;

    /**
     * Create the issue
     *
     * @param parent the issues model
     * @param tagKey the tag key
     * @param tagValue the tag value
     */
    public RequiredTagMissingError(IssuesModel parent, String tagKey, String tagValue) {
        super(parent, Severity.ERROR);
        this.tagKey = tagKey;
        this.tagValue = tagValue;
        actions.add(new AddTagAction());
    }

    @Override
    public String getText() {
        return tr("The required tag <tt>{0}={1}</tt> is missing.",
                this.tagKey,
                this.tagValue
        );
    }

    private class AddTagAction extends AbstractAction {
        AddTagAction() {
            putValue(NAME, tr("Add missing tag"));
            putValue(SHORT_DESCRIPTION, tr("Add the missing tag {0}={1}", tagKey, tagValue));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TagTableModel model = getIssuesModel().getEditorModel().getTagTableModel();
            if (model.get(tagKey) == null)
                model.put(tagKey, tagValue);
        }
    }
}
