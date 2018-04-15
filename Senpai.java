import javax.imageio.ImageIO;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Senpai {
    public InputStream test;
    public static void main(String[]args){
        int name = 'm'+'a'+'s'+'t';
        initiateServer(name,Integer.parseInt(args[1]));
    }
    public Senpai(int port,JTextArea area,PipedOutputStream input){
        int name = 'm'+'a'+'s'+'t';
        initiateServer(name,port,area,input,true);
    }
    public static void initiateServer(int id,int port){
        //initiateServer(id,port,null,null,false);
    }
    public SSLServerSocket buildSecurity(Scanner console,int port){
        try{
            SSLContext context = SSLContext.getInstance("SSL");
            KeyManagerFactory factory = KeyManagerFactory.getInstance("SUNX509");
            KeyStore store = KeyStore.getInstance("JKS");

            System.out.println("login password: ");
            char[]password = console.next().toCharArray();
            store.load(new FileInputStream("jts4e.keys"),password);
            factory.init(store,password);
            context.init(factory.getKeyManagers(),null,null);
            Arrays.fill(password,'0');
            SSLServerSocketFactory secure = context.getServerSocketFactory();
            SSLServerSocket server = (SSLServerSocket)secure.createServerSocket(port);
            String[]cipherSuites = server.getSupportedCipherSuites();
            server.setEnabledCipherSuites(cipherSuites);
            return server;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public void initiateServer(int id, int port, JTextArea area, PipedOutputStream input,boolean redirect){
        final ExecutorService pool = Executors.newFixedThreadPool(10);
        Runnable task = ()-> {
            try{
                //ServerSocket server = new ServerSocket(port);

                PrintStream printStream = new PrintStream(new CustomStream(area));
                PipedInputStream connect = new PipedInputStream(input);

                if(redirect){
                    System.setOut(printStream);
                    System.setErr(printStream);
                    System.setIn(connect);
                }
                Scanner console = new Scanner(System.in);
                SSLServerSocket server = buildSecurity(console,port);

                System.out.println(server.getLocalPort());
                System.out.println("waiting for clients at "+server.getLocalPort()+"...");
                while(true){
                    Socket sock = server.accept();

                    System.out.println(id+" successfully connected to socket "+sock.getPort());

                    DataInputStream in = new DataInputStream(sock.getInputStream());
                    DataOutputStream out = new DataOutputStream(sock.getOutputStream());

                    //Scanner console = new Scanner(System.in);
                    while(true) {
                        System.out.println("what would you like to do:\n" +
                                "1 = command\n"+
                                " 2 = snapshot\n"+
                                " 3 = get output\n"+
                                "4 = move mouse\n"+
                                "5 = press mouse\n"+
                                "6 = send file\n"+
                                "7 = receive file\n"+
                                "-1 = exit]\n");
                        int flag = 0;
                        flag = console.nextInt();
                        /*try{
                            flag = console.nextInt();
                        }catch(IllegalArgumentException e){
                            System.out.println("Error wrong usage please try again");
                            continue;
                        }*/

                        if (flag == -1) {
                            out.writeByte(flag);
                            System.out.println("Goodbye!!");
                            in.close();
                            out.close();
                            sock.close();
                            System.exit(0);
                        }

                        if (flag == 1) {
                            System.out.println("enter command");
                            String command = console.next();
                            String toDo = "";
                            for (int i = 0; i < command.split("-").length; i++) {
                                toDo += command.split("-")[i] + " ";
                            }
                            out.writeByte(flag);
                            out.writeUTF(toDo);
                        }else if(flag == 4){
                            System.out.println("x-coord");
                            int x = console.nextInt();
                            System.out.println("y-coord");
                            int y = console.nextInt();

                            out.writeByte(flag);
                            out.writeInt(x);
                            out.writeInt(y);
                        }else if(flag == 5){
                            System.out.println("keycode 1 left click ");
                            int code = console.nextInt();
                            out.writeByte(flag);
                            out.writeInt(code);
                        }else{
                            out.writeByte(flag);
                        }

                        switch (in.readByte()){
                            case 1:
                                System.out.println("getting output");
                                String temp;
                                while(!(temp = in.readUTF()).equalsIgnoreCase("\0")){
                                    System.out.println(temp);
                                }
                                System.out.println("getting output done");
                                break;
                            case 2:
                                System.out.println("getting picture");
                                File file = new File("Screen.jpg");

                                int length = in.readInt();
                                byte[] pic = new byte[length];
                                if(length > 0){
                                    in.readFully(pic);
                                }
                                if(pic  != null){
                                    InputStream image = new ByteArrayInputStream(pic);
                                    BufferedImage buff = ImageIO.read(image);
                                    ImageIO.write(buff,"jpg",file);
                                }
                                if(file.exists()){
                                    System.out.println("image saved to "+file.getAbsolutePath());
                                }
                                System.out.println("getting picture done");
                                break;
                            case 3:
                                System.out.println("preparing to receive text");
                                File text = new File("received.txt");
                                FileOutputStream writer = new FileOutputStream(text);
                                length = in.readInt();
                                if(length > 0){
                                    byte[]inFile = new byte[length];
                                    in.readFully(inFile);
                                    writer.write(inFile);
                                    System.out.println("getting text done");
                                }
                                break;
                            case 4:
                                System.out.println("moving mouse");
                                if(in.readByte() == 999){
                                    System.out.println("moved mouse");
                                }else{
                                    System.out.println("moved mouse failed");
                                }
                                break;
                            case 5:
                                if (in.readByte() == 999) {
                                    System.out.println("mouse clicked");
                                }else{
                                    System.out.println("mouse not clicked");
                                }
                                break;
                            case 6:
                                System.out.println("enter file to send with extension ");
                                String fileName = console.next();

                                File read = new File(fileName);
                                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(read));

                                byte[]buff = new byte[1024];
                                fileStream.read(buff,0,buff.length);

                                //out.writeByte(6);//flag
                                out.writeUTF(fileName);

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
                            case 7:
                                System.out.println("what file do you want");
                                String toGet = console.next();
                                out.writeUTF(toGet);

                                text = new File(toGet);
                                writer = new FileOutputStream(text);
                                length = in.readInt();
                                if(length > 0){
                                    byte[]inFile = new byte[length];
                                    in.readFully(inFile);
                                    writer.write(inFile);
                                    System.out.println("getting file done");
                                }
                                break;
                            default:
                                System.out.println("nothing");
                                break;
                        }
                        out.flush();
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println("error with IO");
            }catch(Exception e){
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(task);
        pool.submit(thread);
    }
    public class CustomStream extends OutputStream{
        public JTextArea field;
        public int size = 0;
        public CustomStream(JTextArea area){
            this.field = area;
            size = field.getDocument().getLength();
        }
        @Override
        public void write(int i){
            char b = (char)i;
            String c = ""+b;
            field.append(c);
            field.setCaretPosition(field.getDocument().getLength());
            field.update(field.getGraphics());

            if(field.getText().length() > 2000){
                int length = field.getText().length();
                String lastBit = field.getText().substring(length/2,length);
                field.setText(lastBit);
            }
        }
    }
}

