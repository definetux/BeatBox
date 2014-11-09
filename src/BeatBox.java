import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;

public class BeatBox {

    public static final int TACTS = 16;
    public static final int FIELDS_COUNT = 256;
    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
            "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
            "High Agogo", "Open Hi Conga"
    };
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        BeatBox beatBox = new BeatBox();
        beatBox.buildGui();
    }

    public void buildGui() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buildTrackAndStart();
            }
        });
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sequencer.stop();
            }
        });
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeTempo((float)1.03);
            }
        });
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeTempo((float).97);
            }
        });
        buttonBox.add(downTempo);

        JButton clear = new JButton("Clear");
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkboxesClear();
            }
        });
        buttonBox.add(clear);

        JButton save = new JButton("Save");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean[] checkBoxState = new boolean[256];

                for (int i = 0; i < 256; i++) {
                    JCheckBox check = (JCheckBox)checkboxList.get(i);
                    if (check.isSelected()) {
                        checkBoxState[i] = true;
                    }
                }

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.showSaveDialog(theFrame);
                try {
                    FileOutputStream fs = new FileOutputStream(fileChooser.getSelectedFile());
                    ObjectOutputStream os = new ObjectOutputStream(fs);
                    os.writeObject(checkBoxState);
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        buttonBox.add(save);

        JButton load = new JButton("Load");
        load.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean[] checkBoxState = null;

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.showOpenDialog(theFrame);
                try {
                    FileInputStream fs = new FileInputStream(fileChooser.getSelectedFile());
                    ObjectInputStream is = new ObjectInputStream(fs);
                    checkBoxState = (boolean[])is.readObject();
                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }

                for (int i = 0; i < FIELDS_COUNT; i++) {
                    JCheckBox check = (JCheckBox)checkboxList.get(i);
                    check.setSelected(checkBoxState[i]);
                }

                sequencer.stop();
                buildTrackAndStart();
            }
        });
        buttonBox.add(load);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < TACTS; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(TACTS, TACTS);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < FIELDS_COUNT; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    private void checkboxesClear() {
        for(JCheckBox item : checkboxList) {
            item.setSelected(false);
        }
    }

    private void changeTempo(float factor) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor((float) (tempoFactor * factor));
    }

    public void setUpMidi() {

        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();

            sequence = new Sequence( Sequence.PPQ, 4 );
            Track track = sequence.createTrack();
            sequencer.setTempoInBPM( 120 );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart() {
        int[] trackList = null;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < TACTS; i++) {
            trackList = new int[TACTS];

            int key = instruments[i];
            for (int j = 0; j < TACTS; j++) {
                JCheckBox jc = (JCheckBox)checkboxList.get(j + (TACTS * i));
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            }

            makeTracks(trackList);
            track.add(makeEvent(176, 1, 127, 0, TACTS));
        }

        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeTracks(int[] trackList) {
        for (int i = 0; i < TACTS; i++) {
            int key = trackList[i];

            if (key != 0) {
                track.add(makeEvent(144, 9, key, 100, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }

    public static MidiEvent makeEvent( int comd, int chan, int one, int two, int tick ) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage(  );
            a.setMessage( comd, chan, one ,two );
            event = new MidiEvent( a, tick );
        } catch ( Exception e ) {

        }
        return event;
    }
}
