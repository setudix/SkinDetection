import java.io.IOException;

public class TrainerRunner {
    public static void main(String[] args) throws IOException {
        Trainer trainer = new Trainer();
        try{
            trainer.run();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
