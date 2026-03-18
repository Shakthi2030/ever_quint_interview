import java.util.*;

public class MaxProfit {

    static int maxProfit;
    static List<int[]> bestSolutions;

    public static void main(String[] args) {
        int[] testCases = {7, 8, 13, 49};
        for (int n : testCases) {
            maxProfit = 0;
            bestSolutions = new ArrayList<>();
            dfs(n, 0, 0, 0, 0, 0);

            System.out.println("Input Time Unit: " + n);
            System.out.println("Output Earnings: $" + maxProfit);
            System.out.println("Solutions");
            for (int i = 0; i < bestSolutions.size(); i++) {
                int[] sol = bestSolutions.get(i);
                System.out.println((i+1) + ". T: " + sol[0] + " P: " + sol[1] + " C: " + sol[2]);
            }
            System.out.println("--------------------------------");
        }
    }

    static void dfs(int n, int currentTime, int profit, int t, int p, int c) {
        if (profit > maxProfit) {
            maxProfit = profit;
            bestSolutions = new ArrayList<>();
            bestSolutions.add(new int[]{t, p, c});
        } else if (profit == maxProfit && profit > 0) {
            int[] candidate = {t, p, c};
            boolean duplicate = false;
            for (int[] sol : bestSolutions) {
                if (Arrays.equals(sol, candidate)) { duplicate = true; break; }
            }
            if (!duplicate) bestSolutions.add(candidate);
        }

        if (currentTime + 5 < n) {
            int ct = currentTime + 5;
            dfs(n, ct, profit + (n - ct) * 1500, t + 1, p, c);
        }
        if (currentTime + 4 < n) {
            int ct = currentTime + 4;
            dfs(n, ct, profit + (n - ct) * 1000, t, p + 1, c);
        }
        if (currentTime + 10 < n) {
            int ct = currentTime + 10;
            dfs(n, ct, profit + (n - ct) * 2000, t, p, c + 1);
        }
    }
}
