import java.io.*;
import java.net.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private String currentLine = "";

    private BufferedReader input;
    private PrintStream output;

    private ServerSocket server = null;
    private Socket openSocket = null;
    private InetAddress host;

    public Server(int id,int port){
        initiateServer(id,port);
    }
    public static void main(String[]args){
        Server server = new Server(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
    }
    public StringBuffer readFromClient(){
        try{
            StringBuffer buff = new StringBuffer();
            String temp = "";

            while((temp = input.readLine()) != null){
                buff.append(temp);
            }
            System.out.println("received message "+buff.toString());
            return buff;
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public void initiateServer(int id,int port){
        final ExecutorService pool = Executors.newFixedThreadPool(10);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try{
                    ServerSocket server = new ServerSocket(port);
                    System.out.println("waiting for clients at "+server.getLocalPort()+"...");
                    while(true){
                        Socket sock = server.accept();
                        System.out.println(id+" successfully connected to socket "+sock.getPort());
                        new echo(sock,id);
                    }
                }catch(IOException e){
                    e.printStackTrace();
                    System.out.println("error with IO");
                }
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
                BufferedReader input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                PrintStream output = new PrintStream(sock.getOutputStream());

                StringBuffer buff = new StringBuffer();
                String temp;

                while((temp = input.readLine()) != null){
                    buff.append(temp);

                    String message = buff.toString();
                    System.out.println("received message "+message);

                    output.println("current message "+message);

                    if(message.split(" ")[1].equalsIgnoreCase("close")){
                        input.close();
                        output.close();
                        break;
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
    }
}
