package BeatBox;
import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.awt.event.*;


public class BeatBox {
    JPanel mainPanel;
    ArrayList<JCheckBox> checkBoxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    JList incomingList;
    JTextField userMassage;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String,boolean[]> otherSeqsMap = new HashMap<String,boolean[]>();
    Sequence mySequence = null;



    String [] instrumentNames = {"Bass Drum", "Closed Hi-Hat","Open Hi-Hat","Acoustic Snare","Crash Cymbal","Hand Clap","High Tom","Hi Bongo","Maracas","Whistle",
    "Low Conga","Cowbell","Vibraslap","Low-mid Tom","High Agogo","Open Hi Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {
        new BeatBox().startUp("Woland"); // индефикатор пользователя
    }
    public void startUp(String name){
        userName = name;
        //Открываем соеденение с сервером

        try{
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch(Exception e){
            System.out.println("no connect ");
        }
        setUpMidi();
        buildGUI();
    }
    public void buildGUI(){
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkBoxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("tempo UP");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("tempo DOWN");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton serializet = new JButton("Save");
        serializet.addActionListener(new MySafeListener());
        buttonBox.add(serializet);

        JButton restore = new JButton("Load");
        restore.addActionListener(new MyLoadListener());
        buttonBox.add(restore);

        JButton sendIt = new JButton("sendIt");
        sendIt.addActionListener(new MySendListener());
        buttonBox.add(sendIt);

        userMassage = new JTextField();
        buttonBox.add(userMassage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);


        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for(int i = 0; i < 16; i++){
            nameBox.add(new Label(instrumentNames[i]));
            }

        background.add(BorderLayout.EAST,buttonBox);
        background.add(BorderLayout.WEST,nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER,mainPanel);

        for(int i = 0; i < 256; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        //setUpMidi();

        theFrame.setBounds(50,50, 300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }
    public void setUpMidi(){
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch (Exception e) {e.printStackTrace();}
    }

    public void buildTrackAndStart() {
        ArrayList<Integer> trackList = null ;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for(int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();
            //
            for(int j = 0; j < 16; j++) {
                JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16*i));
                if(jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(new Integer(key));
                 }else{
                    trackList.add(null);
                }

            }
            makeTracks(trackList);
            //track.add(makeEvent(176,1,127,0,16));
        }
        track.add(makeEvent(192,9,1,0,15));
        try{
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
            }catch (Exception e) {e.printStackTrace();}
    }
    public class MyStartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();

        }
    }
    public class MyStopListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();

        }
    }
    public class MyUpTempoListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 1.03)) ;

        }
    }
    public class MyDownTempoListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * 0.97)) ;

        }
    }

    public class MySendListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean [] checkboxState = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = (JCheckBox) checkBoxList.get(i);
                if(check.isSelected()) {
                    checkboxState[i] = true;
                }

            }
            String messageToSend = null;
            try{
                out.writeObject(userName +nextNum++ +":" + userMassage.getText());
                out.writeObject(checkboxState);
            }catch (Exception ex){
                System.out.println("Sorry");
            }
            userMassage.setText("");
        }
    }

    public class MyListSelectionListener implements ListSelectionListener{
        @Override
        public void valueChanged(ListSelectionEvent le) {
            if(!le.getValueIsAdjusting()){
                String selected = (String) incomingList.getSelectedValue();
                if(selected != null){
                    boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class RemoteReader implements Runnable{
        boolean[] checkboxState = null;
        String nameToSow = null;
        Object obj = null;
        public void run() {
            try{
                while ((obj = in.readObject()) != null){
                    System.out.println("got an objeckt from server");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow,checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            }catch (Exception ex){ex.printStackTrace();}
        }
    }

    public class MyPlayMineListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if(mySequence != null) {
                sequence = mySequence;
            }
        }
    }

    public void changeSequence(boolean[] checkboxState){
        for (int i = 0; i < 256; i++) {
            JCheckBox check = (JCheckBox) checkBoxList.get(i);
            if(checkboxState[i]) {
                check.setSelected(true);
            }else{
                check.setSelected(false);
            }
        }
    }
    public void makeTracks(ArrayList list) {
        Iterator it = list.iterator();
        for(int i = 0 ; i < 16; i++){
            Integer num = (Integer) it.next();

            if(num != null ){
                int numKey = num.intValue();
                track.add(makeEvent(144,9,numKey,100,i));
                track.add(makeEvent(128,9,numKey,100,i+1));

            }
        }
    }
    public MidiEvent makeEvent(int comd,int chan,int one,int two,int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd,chan,one,two);
            event = new MidiEvent(a,tick);
        }catch (Exception e) {e.printStackTrace();}
        return event;
    }
    public class MySafeListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = new boolean[256];
            for(int i = 0; i < 256; i++){
                JCheckBox check = (JCheckBox) checkBoxList.get(i);
                if(check.isSelected()){
                    checkboxState[i] = true;
                }
            }
            try {
                JFileChooser fileOpen = new JFileChooser();
                fileOpen.showOpenDialog(theFrame);
                FileOutputStream fileStream = new FileOutputStream(fileOpen.getSelectedFile());
                ObjectOutputStream os = new ObjectOutputStream(fileStream);
                os.writeObject(checkboxState);
            }catch(Exception ex) {ex.printStackTrace();}
        }
    }
    public class MyLoadListener implements ActionListener{
        public void actionPerformed(ActionEvent a) {
            boolean[] checkboxState = null;
            try {
                JFileChooser fileOpen = new JFileChooser();
                fileOpen.showOpenDialog(theFrame);
                FileInputStream fileIn = new FileInputStream(fileOpen.getSelectedFile());
                ObjectInputStream is = new ObjectInputStream(fileIn);
                checkboxState = (boolean[]) is.readObject();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        for(int i = 0; i <256; i++){
            JCheckBox check = (JCheckBox) checkBoxList.get(i);
            if (checkboxState[i]) {
                check.setSelected(true);
                } else {
                check.setSelected(false);
            }

        }
        sequencer.stop();
        buildTrackAndStart();
        }
    }

}
