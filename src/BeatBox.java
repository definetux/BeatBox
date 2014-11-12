import sun.text.resources.FormatData_iw_IL;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class BeatBox {

    public static final int TACTS = 16;
    public static final int FIELDS_COUNT = 256;
    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence = null;
    Track track;
    JFrame theFrame;
    JList incomingList;
    JTextField userMessage;
    JTextField txtUserName;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectInputStream in;
    ObjectOutputStream out;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
            "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
            "High Agogo", "Open Hi Conga"
    };
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        BeatBox beatBox = new BeatBox();
        beatBox.startUp();
    }

    public void startUp() {
        try {
            Socket socket = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setUpMidi();
        buildGui();
    }

    public void buildGui() {
        theFrame = new JFrame("Cyber BeatBox");
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        txtUserName = new JTextField(10);
        txtUserName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userName = txtUserName.getText();
            }
        });
        buttonBox.add(txtUserName);

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

        JButton sendIt = new JButton("sendIt");
        sendIt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean[] checkboxState = new boolean[FIELDS_COUNT];
                for (int i = 0; i < FIELDS_COUNT; i++) {
                    JCheckBox check = (JCheckBox) checkboxList.get(i);
                    if (check.isSelected()) {
                        checkboxState[i] = true;
                    }
                }
                String messageToSend = null;
                try {
                    out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
                    out.writeObject(checkboxState);
                } catch (Exception ex) {
                    System.out.println("Sorry dude. Could not send it to the server");
                }
            }
        });
        buttonBox.add(sendIt);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selected = (String) incomingList.getSelectedValue();
                    if( selected != null) {
                        boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                        changeSequence(selectedState);
                        sequencer.stop();
                        buildTrackAndStart();
                    }
                }
            }
        });
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

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

    private void changeSequence(boolean[] selectedState) {
        for (int i = 0; i < FIELDS_COUNT; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (selectedState[i]) {
                check.setSelected(true);
            } else {
                check.setSelected(false);
            }
        }
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
            track = sequence.createTrack();
            sequencer.setTempoInBPM( 120 );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < TACTS; i++) {
            trackList = new ArrayList<Integer>();

            for (int j = 0; j < TACTS; j++) {
                JCheckBox jc = (JCheckBox)checkboxList.get(j + (TACTS * i));
                if (jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(new Integer(key));
                } else {
                    trackList.add(null);
                }
            }

            makeTracks(trackList);
            track.add(makeEvent(192, 9, 1, 0, TACTS - 1));
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

    private void makeTracks(ArrayList trackList) {
        Iterator it = trackList.iterator();
        for (int i = 0; i < TACTS; i++) {
            Integer num = (Integer) it.next();
            if (num != null) {
                int numKey = num.intValue();
                track.add(makeEvent(144, 9, numKey, 100, i));
                track.add(makeEvent(128, 9, numKey, 100, i + 1));
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

    private class RemoteReader implements Runnable {
        boolean[] checkboxState = null;
        String nameToShow = null;
        Object object = null;

        @Override
        public void run() {
            try {
                while ((object = in.readObject()) != null) {
                    System.out.println("got an object from server");
                    System.out.println(object.getClass());
                    nameToShow = (String)object;
                    checkboxState = (boolean[])in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
