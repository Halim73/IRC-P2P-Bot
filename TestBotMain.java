import org.jibble.pircbot.*;

public class TestBotMain {
    public static void main(String[]args) throws Exception{
        TestBot bot = new TestBot();

        bot.setVerbose(true);
        bot.connect("irc.freenode.net");
        bot.joinChannel("##pr4ctic3");
    }
}
