import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TOBService {

    private final Map<Integer, TreeMap<Long, Integer>> buyOrders = new HashMap<>();
    private final Map<Integer, TreeMap<Long, Integer>> sellOrders = new HashMap<>();
    private final Map<String, UserStats> userStats = new HashMap<>();

    public void processOrder(String userId, String clorderId, char action, int instrumentId, char side, long price, int amount, int amountRest) {
        if (price < 0 || amount < 0 || amountRest < 0) {
            throw new IllegalArgumentException(TOBConstants.WRONG_ARGUMENT_EXCEPTION);
        }

        // Логика сохранения сделок пользователей по их id.
        userStats.computeIfAbsent(userId, k -> new UserStats()).update(action, amount);

        // Получение мапы по инструменту и мапы с ценами в разном порядке(для покупки и продажи).
        Map<Integer, TreeMap<Long, Integer>> orders = getOrdersBySide(side);
        TreeMap<Long, Integer> instrumentOrders = orders.computeIfAbsent(
                instrumentId,
                k -> new TreeMap<>(side == TOBConstants.BUY_REQUEST_CHAR ? Comparator.<Long>reverseOrder() : Comparator.<Long>naturalOrder())
        );

        long bestPriceBefore = getBestPrice(instrumentOrders);
        int bestVolumeBefore = getBestVolume(instrumentOrders, bestPriceBefore);

        // Процессинг действия.
        switch (action) {
            case TOBConstants.SET_ACTION_CHAR:
                instrumentOrders.put(price, Math.addExact(instrumentOrders.getOrDefault(price, 0), amountRest));
                break;
            case TOBConstants.DROP_ACTION_CHAR:
                int newDropAmount = Math.subtractExact(instrumentOrders.getOrDefault(price, 0), amount);
                if (newDropAmount <= 0) {
                    instrumentOrders.remove(price);
                } else {
                    instrumentOrders.put(price, newDropAmount);
                }
                break;
            case TOBConstants.COMMIT_ACTION_CHAR:
                int newCommitAmount = Math.subtractExact(instrumentOrders.getOrDefault(price, 0), amount);
                if (amountRest == 0 || newCommitAmount <= 0) {
                    instrumentOrders.remove(price);
                } else {
                    instrumentOrders.put(price, newCommitAmount);
                }
                break;
            default:
                throw new IllegalArgumentException(TOBConstants.WRONG_ACTION_EXCEPTION + action);
        }

        // Проверка цен и вывод если есть новая лучшая.
        long bestPriceAfter = getBestPrice(instrumentOrders);
        int bestVolumeAfter = getBestVolume(instrumentOrders, bestPriceAfter);

        if (bestPriceBefore != bestPriceAfter || bestVolumeBefore != bestVolumeAfter) {
            if (bestPriceAfter == 0 || bestVolumeAfter == 0) {
                bestPriceAfter = (side == TOBConstants.BUY_REQUEST_CHAR) ? 0 : 999999999999999999L;
            }
            printNewBestPrice(instrumentId, side, bestPriceAfter, bestVolumeAfter);
        }
    }

    private long getBestPrice(TreeMap<Long, Integer> orders) {
        return orders.isEmpty() ? 0 : orders.firstKey();
    }

    private int getBestVolume(TreeMap<Long, Integer> orders, long bestPrice) {
        return bestPrice == 0 ? 0 : orders.getOrDefault(bestPrice, 0);
    }

    private void printNewBestPrice(int instrumentId, char side, long bestPriceAfter, int bestVolumeAfter) {
        System.out.printf(TOBConstants.OUTPUT_PATTERN, instrumentId, side, bestPriceAfter, bestVolumeAfter);
    }

    private Map<Integer, TreeMap<Long, Integer>> getOrdersBySide(Character side) {
        if (side.equals(TOBConstants.BUY_REQUEST_CHAR)) {
            return buyOrders;
        } else if (side.equals(TOBConstants.SELL_REQUEST_CHAR)) {
            return sellOrders;
        } else {
            throw new IllegalArgumentException(TOBConstants.WRONG_SIDE_EXCEPTION);
        }
    }

    // Константы
    static class TOBConstants {
        public static final char BUY_REQUEST_CHAR = 'B';
        public static final char SELL_REQUEST_CHAR = 'S';
        public static final char SET_ACTION_CHAR = '0';
        public static final char DROP_ACTION_CHAR = '1';
        public static final char COMMIT_ACTION_CHAR = '2';
        public static final String WRONG_ARGUMENT_EXCEPTION = "Price, amount, and amountRest must be non-negative.";
        public static final String WRONG_ACTION_EXCEPTION = "Invalid action type: ";
        public static final String WRONG_SIDE_EXCEPTION = "Invalid side. Must be 'B' for buy or 'S' for sell.";
        public static final String OUTPUT_PATTERN = "%d;%c;%d;%d\n";
        public static final String USER_STAT_INFO_PATTERN = "User: %s, Stats: %s\n";
        public static final String SINGLE_USER_STAT_INFO_PATTERN = "TotalOrders: %d, TotalDeals: %d, TotalVolume: %d";
    }

    // Метод для получения статистики пользователей.
    public void printUserStats() {
        for (Map.Entry<String, UserStats> entry : userStats.entrySet()) {
            System.out.printf(TOBConstants.USER_STAT_INFO_PATTERN, entry.getKey(), entry.getValue());
        }
    }

    // Статистика пользователя.
    static class UserStats {
        // Общее количество заявок.
        private int totalOrders = 0;
        // Количество сделок.
        private int totalDeals = 0;
        // Общий объем сделок.
        private int totalVolume = 0;

        public void update(char action, int amount) {
            switch (action) {
                case TOBConstants.SET_ACTION_CHAR:
                    totalOrders++;
                    break;
                case TOBConstants.COMMIT_ACTION_CHAR:
                    totalDeals++;
                    totalVolume += amount;
                    break;
            }
        }

        @Override
        public String toString() {
            return String.format(TOBConstants.SINGLE_USER_STAT_INFO_PATTERN,
                    totalOrders, totalDeals, totalVolume);
        }
    }
}
