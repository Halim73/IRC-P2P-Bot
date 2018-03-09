import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Senpai {
    public static void main(String[]args){
        int name = 'm'+'a'+'s'+'t';
        initiateServer(name,Integer.parseInt(args[1]));
    }
    public static void initiateServer(int id,int port){
        final ExecutorService pool = Executors.newFixedThreadPool(10);

        Runnable task = ()-> {
            try{
                ServerSocket server = new ServerSocket(port);
                System.out.println(server.getLocalPort());
                System.out.println("waiting for clients at "+server.getLocalPort()+"...");
                while(true){
                    Socket sock = server.accept();
                    System.out.println(id+" successfully connected to socket "+sock.getPort());

                    Scanner console = new Scanner(System.in);

                    DataInputStream in = new DataInputStream(sock.getInputStream());
                    DataOutputStream out = new DataOutputStream(sock.getOutputStream());

                    while(true) {
                        System.out.println("what would you like to do [1 = command 2 = snapshot 3 = get output 4 = move mouse 5 = press mouse -1 = exit]");
                        int flag = console.nextInt();

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
            }
        };
        Thread thread = new Thread(task);
        pool.submit(thread);
    }
}
