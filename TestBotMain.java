import org.jibble.pircbot.*;

public class TestBotMain {
    public static void main(String[]args) throws Exception{
        //TestBot bot = new TestBot();
        ParaBot bot = new ParaBot();
        bot.setVerbose(true);
        bot.connect("irc.freenode.net");
        bot.joinChannel("##pr4ctic3");
    }
}
