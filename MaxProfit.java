public class MaxProfit {

    static int maxProfit;
    static int bestT, bestP, bestC;

    public static void main(String[] args) {

        int[] testCases = {7, 8, 13};

        for (int n : testCases) {

            maxProfit = 0;
            bestT = bestP = bestC = 0;

            dfs(n, 0, 0, 0, 0, 0);

            System.out.println("Input Time Unit: " + n);
            System.out.println("Output Earnings: $" + maxProfit);
            System.out.println("Solution:");
            System.out.println("T: " + bestT + " P: " + bestP + " C: " + bestC);
            System.out.println("--------------------------------");
        }
    }

    static void dfs(int n, int currentTime, int profit, int t, int p, int c) {

        if (profit > maxProfit) {
            maxProfit = profit;
            bestT = t;
            bestP = p;
            bestC = c;
        }

        if (currentTime + 5 <= n) {
            int completionTime = currentTime + 5;
            int earn = (n - completionTime) * 1500;
            dfs(n, completionTime, profit + earn, t + 1, p, c);
        }

        if (currentTime + 4 <= n) {
            int completionTime = currentTime + 4;
            int earn = (n - completionTime) * 1000;
            dfs(n, completionTime, profit + earn, t, p + 1, c);
        }

        if (currentTime + 10 <= n) {
            int completionTime = currentTime + 10;
            int earn = (n - completionTime) * 2000;
            dfs(n, completionTime, profit + earn, t, p, c + 1);
        }
    }
}
