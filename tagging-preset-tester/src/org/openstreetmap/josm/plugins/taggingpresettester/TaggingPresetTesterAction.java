package org.openstreetmap.josm.plugins.taggingpresettester;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.preferences.sources.PresetPrefHelper;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Fires up the tagging preset tester
 * @author Immanuel.Scholz
 */
public class TaggingPresetTesterAction extends JosmAction {

    public TaggingPresetTesterAction() {
        super(tr("Tagging Preset Tester"), "tagging-preset-tester",
        tr("Open the tagging preset test tool for previewing tagging preset dialogs."),
        Shortcut.registerShortcut("tools:taggingpresettester",
        tr("Windows: {0}", tr("Tagging Preset Tester")),
        KeyEvent.VK_T, Shortcut.ALT_CTRL_SHIFT), true);
        MainMenu.addAfter(MainApplication.getMenu().windowMenu, this, false, MainApplication.getMenu().changesetManager);
    }

    public TaggingPresetTesterAction(PluginInformation info) {
        this();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String[] taggingPresetSources = PresetPrefHelper.INSTANCE.get()
            .stream().filter(s -> s.active).map(s -> s.name).toArray(String[]::new);

        if (taggingPresetSources.length == 0) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("You have to specify tagging preset sources in the preferences first."));
            return;
        }

        new TaggingPresetTester(taggingPresetSources).setVisible(true);
    }
}
