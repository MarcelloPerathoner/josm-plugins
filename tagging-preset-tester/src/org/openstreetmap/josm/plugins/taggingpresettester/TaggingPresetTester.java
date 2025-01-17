package org.openstreetmap.josm.plugins.taggingpresettester;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.tools.GBC;

/**
 * The tagging presets tester window
 */
public class TaggingPresetTester extends JFrame {

    private TaggingPresetSelector taggingPresetSelector;
    private final String[] args;
    private JPanel taggingPresetPanel = new JPanel(new BorderLayout());
    private JPanel panel = new JPanel(new GridBagLayout());

    public final void reload() {
        TaggingPresets taggingPresets = MainApplication.getTaggingPresets();
        for (String url : args)
            taggingPresets.addSourceFromUrl(url);
        taggingPresets.reInit(null, null);
    }

    public final void reselect() {
        taggingPresetPanel.removeAll();
        TaggingPreset preset = taggingPresetSelector.getSelectedPreset();
        if (preset == null)
            return;
        Collection<OsmPrimitive> x;
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds != null) {
            x = ds.getSelected();
        } else {
            x = makeFakeSuitablePrimitive(preset);
            // See #21829: DataIntegrityProblemException: Primitive must be part of the dataset
            DataSet tmp = new DataSet();
            x.forEach(tmp::addPrimitiveRecursive);
        }
        JPanel p = preset.createPanel(x);
        if (p != null) {
            p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            taggingPresetPanel.add(p, BorderLayout.NORTH);
        }
        panel.validate();
        panel.repaint();
    }

    public TaggingPresetTester(String[] args) {
        super(tr("Tagging Preset Tester"));
        this.args = args;
        taggingPresetSelector = new TaggingPresetSelector(true, true);
        taggingPresetSelector.setMinimumSize(new Dimension(150,250));
        taggingPresetSelector.setPreferredSize(new Dimension(300,500));
        taggingPresetPanel.setMinimumSize(new Dimension(150,250));
        taggingPresetPanel.setPreferredSize(new Dimension(300,500));
        reload();

        panel.add(taggingPresetSelector, GBC.std(0,0).fill(GBC.BOTH).weight(0.5, 1.0));
        panel.add(taggingPresetPanel, GBC.std(1,0).fill(GBC.BOTH).weight(0.5, 1.0));
        taggingPresetSelector.addSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && taggingPresetSelector.getSelectedPreset() != null) {
                reselect();
            }
        });
        reselect();

        JButton b = new JButton(tr("Reload"));
        b.addActionListener(e -> {
            TaggingPreset p = taggingPresetSelector.getSelectedPreset();
            reload();
            if (p!=null) taggingPresetSelector.setSelectedPreset(p);
        });
        panel.add(b, GBC.std(0,1).span(2,1).fill(GBC.HORIZONTAL));

        setContentPane(panel);
        setMinimumSize(new Dimension(400,300));
        setSize(600,500);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            JFileChooser c = new JFileChooser();
            if (c.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
                return;
            args = new String[]{c.getSelectedFile().getPath()};
        }
        JFrame f = new TaggingPresetTester(args);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

    private Collection<OsmPrimitive> makeFakeSuitablePrimitive(TaggingPreset preset) {
        if (preset.typeMatches(Collections.singleton(TaggingPresetType.NODE))) {
            return Collections.<OsmPrimitive>singleton(new Node());
        } else if (preset.typeMatches(Collections.singleton(TaggingPresetType.WAY))) {
            return Collections.<OsmPrimitive>singleton(new Way());
        } else if (preset.typeMatches(Collections.singleton(TaggingPresetType.RELATION))) {
            return Collections.<OsmPrimitive>singleton(new Relation());
        } else if (preset.typeMatches(Collections.singleton(TaggingPresetType.CLOSEDWAY))) {
            Way w = new Way();
            w.addNode(new Node(new LatLon(0,0)));
            w.addNode(new Node(new LatLon(0,1)));
            w.addNode(new Node(new LatLon(1,1)));
            w.addNode(new Node(new LatLon(0,0)));
            return Collections.<OsmPrimitive>singleton(w);
        } else {
            return Collections.emptySet();
        }
    }
}
