import java.util.Scanner;

/**
 * @author Gavin
 * 2019/8/28 17:19
 */
public class MetricClient {

    public static void main(String[] args) throws Exception {
        ClientNetProtocol clientNetProtocol = new ClientNetProtocol(10011);
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            clientNetProtocol.addMessage(line);
        }
    }
}
