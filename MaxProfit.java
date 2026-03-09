public class MaxProfit {

    public static void main(String[] args) {

        int[] testCases = {7, 8, 13};

        for (int n : testCases) {

            int maxProfit = 0;
            int bestT = 0;
            int bestP = 0;
            int bestC = 0;

            for (int t = 0; t <= n / 5; t++) {
                for (int p = 0; p <= n / 4; p++) {
                    for (int c = 0; c <= n / 10; c++) {

                        int buildTime = t * 5 + p * 4 + c * 10;

                        if (buildTime <= n) {

                            int profit = 0;
                            int time = 0;

                            for (int i = 0; i < t; i++) {
                                time += 5;
                                profit += (n - time) * 1500;
                            }

                            for (int i = 0; i < p; i++) {
                                time += 4;
                                profit += (n - time) * 1000;
                            }

                            for (int i = 0; i < c; i++) {
                                time += 10;
                                profit += (n - time) * 2000;
                            }

                            if (profit > maxProfit) {
                                maxProfit = profit;
                                bestT = t;
                                bestP = p;
                                bestC = c;
                            }
                        }
                    }
                }
            }

            System.out.println("Input Time Unit: " + n);
            System.out.println("Output Earnings: $" + maxProfit);
            System.out.println("Solution:");
            System.out.println("T: " + bestT + " P: " + bestP + " C: " + bestC);
            System.out.println("-----------------------------");
        }
    }
}