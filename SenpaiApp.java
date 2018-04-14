import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class SenpaiApp extends JFrame{
    private connectionPanel connect;
    public static textPanels texts;
    private JPanel actionPanel;

    public PipedOutputStream toSenpai;

    public Senpai senpai;

    public JTextArea output;
    public TextStream input;

    public static void main(String[]args){
        SenpaiApp app = new SenpaiApp(300,300);
    }

    public SenpaiApp(int width, int height){
        setTitle("SENPAI!!!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width,height);

        ButtonGroup group = new ButtonGroup();

        texts = new textPanels();
        output = texts.output;

        connect = new connectionPanel(output,toSenpai);
        buildActionPanel();

        add(connect,BorderLayout.NORTH);
        add(actionPanel,BorderLayout.SOUTH);
        add(texts,BorderLayout.CENTER);

        pack();
        setVisible(true);
    }
    private void buildActionPanel(){
        JButton exit = new JButton("Exit");
        exit.addActionListener( new exitButtonListener());

        actionPanel = new JPanel();
        actionPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        actionPanel.add(exit);
    }
    private class exitButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            System.exit(0);
        }
    }
    public class textPanels extends JPanel{
        private JPanel panel;

        private JLabel message;
        private JLabel message2;

        public JTextField input;
        public JTextArea output;

        TextStream stream;
        private JButton send;

        public textPanels(){
            message = new JLabel("output field/");
            message2 = new JLabel( "input field");

            input = new JTextField(20);
            output = new JTextArea(5,10);

            send = new JButton("send input");

            stream = new TextStream(input);

            toSenpai = new PipedOutputStream();
            sendButtonListener sender = new sendButtonListener(toSenpai);

            send.addActionListener(sender);
            input.addActionListener(sender);

            panel = new JPanel();
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            panel.add(message);panel.add(message2);
            panel.add(input);panel.add(output);panel.add(send);

            add(panel);
            setVisible(true);
        }
        private class sendButtonListener implements ActionListener{
            private PipedOutputStream out;
            public sendButtonListener(PipedOutputStream out){
                this.out = out;
            }
            public void actionPerformed(ActionEvent e){
               try{
                   String toSend = input.getText()+"\n";
                   out.write(toSend.getBytes());
                   input.setText("");
               }catch(Exception b){
                   b.printStackTrace();
               }
            }
        }
    }

    private class connectionPanel extends JPanel{
        private JPanel panel;
        private JLabel message;

        private JTextField inputText;
        private JButton connect;

        public connectionPanel(JTextArea output,PipedOutputStream input){
            populatePanel(output,input);
            add(panel);
            setVisible(true);
        }
        private void populatePanel(JTextArea output,PipedOutputStream input){
            message = new JLabel("Enter the port number to wait on ");
            inputText = new JTextField(10);
            connect = new JButton(" wait for connection");

            connect.addActionListener( new connectButtonListener(output,input));

            panel = new JPanel();
            panel.setBorder(BorderFactory.createLineBorder(Color.BLUE));

            panel.add(message);
            panel.add(inputText);
            panel.add(connect);
        }
        private class connectButtonListener implements ActionListener{
            JTextArea output;
            PipedOutputStream input;

            public connectButtonListener(JTextArea output,PipedOutputStream input){
                this.output = output;
                this.input = input;
            }
            public void actionPerformed(ActionEvent e){
                String toConnect = inputText.getText();
                int port = Integer.parseInt(toConnect);
                senpai = new Senpai(port,output,input);
            }
        }
    }
    public class TextStream extends InputStream implements ActionListener{
        public JTextField in;
        public String string;
        public int index = 0;

        public TextStream(JTextField in){
            this.in = in;
            this.string = this.in.getText();
        }
        @Override
        public void actionPerformed(ActionEvent a){
            this.string = this.in.getText()+"\n";
            index = 0;
            this.in.setText("");
            synchronized (this){
                this.notify();
            }
        }
        @Override
        public int read(){
            if(string != null && index == string.length()){
                string = null;
                return java.io.StreamTokenizer.TT_EOF;
            }
            while(string == null || index >= string.length()){
                try{
                    synchronized (this){
                        this.wait();
                    }
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            return this.string.charAt(index++);
        }
    }
}
