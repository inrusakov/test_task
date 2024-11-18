import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        TOBService tobService = new TOBService();

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.equals("exit"))
                break;
            String[] fields = line.split(";");
            String userId = fields[0];
            String clorderId = fields[1];
            char action = fields[2].charAt(0);
            int instrumentId = Integer.parseInt(fields[3]);
            char side = fields[4].charAt(0);
            long price = Long.parseLong(fields[5]);
            int amount = Integer.parseInt(fields[6]);
            int amountRest = Integer.parseInt(fields[7]);

            tobService.processOrder(userId, clorderId, action, instrumentId, side, price, amount, amountRest);
        }
        tobService.printUserStats();
    }
}