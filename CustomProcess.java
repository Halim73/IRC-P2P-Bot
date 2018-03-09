import java.io.Serializable;

public class CustomProcess implements Serializable {
    public final ProcessBuilder process;

    public CustomProcess(ProcessBuilder process){
        this.process = process;
    }
    public ProcessBuilder getProcess(){
        return this.process;
    }
}
