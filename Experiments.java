import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Experiments
 *
 * Runs three experiments on four types of heaps:
 * 1. Binomial Heap (lazyMelds=false, lazyDecreaseKeys=false)
 * 2. Lazy Binomial Heap (lazyMelds=true, lazyDecreaseKeys=false)
 * 3. Fibonacci Heap (lazyMelds=true, lazyDecreaseKeys=true)
 * 4. Binomial Heap with Cutoffs (lazyMelds=false, lazyDecreaseKeys=true)
 *
 * n = 464,646
 */

class OperationMetrics {
    long prevLinks;
    long prevCuts;
    long prevHeapifyCosts;
    long maxOperationCost = 0;

    public void startTracking(Heap heap) {
        prevLinks = heap.totalLinks();
        prevCuts = heap.totalCuts();
        prevHeapifyCosts = heap.totalHeapifyCosts();
    }

    public void recordOperation(Heap heap) {
        long currentLinks = heap.totalLinks();
        long currentCuts = heap.totalCuts();
        long currentHeapifyCosts = heap.totalHeapifyCosts();

        long operationCost = (currentLinks - prevLinks) +
                (currentCuts - prevCuts) +
                (currentHeapifyCosts - prevHeapifyCosts);

        maxOperationCost = Math.max(maxOperationCost, operationCost);

        prevLinks = currentLinks;
        prevCuts = currentCuts;
        prevHeapifyCosts = currentHeapifyCosts;
    }

    public long getMaxOperationCost() {
        return maxOperationCost;
    }
}

class ExperimentResults {
    long totalRuntime;
    long totalLinks;
    long totalCuts;
    long totalHeapifyCosts;
    long maxOperationCost;
    int numTrees;
    int heapSize;

    ExperimentResults(long runtime, long links, long cuts, long heapifyCosts, long maxOpCost, int numTrees, int heapSize) {
        this.totalRuntime = runtime;
        this.totalLinks = links;
        this.totalCuts = cuts;
        this.totalHeapifyCosts = heapifyCosts;
        this.maxOperationCost = maxOpCost;
        this.numTrees = numTrees;
        this.heapSize = heapSize;
    }
}

public class Experiments {

    static final int N = 464646;  // Full size for experiments
    static final int NUM_HEAP_TYPES = 4;
    static final int NUM_TRIALS = 20;  // Run each test 20 times

    // Heap types
    static final int BINOMIAL_HEAP = 0;           // lazyMelds=false, lazyDecreaseKeys=false
    static final int LAZY_BINOMIAL_HEAP = 1;      // lazyMelds=true, lazyDecreaseKeys=false
    static final int FIBONACCI_HEAP = 2;          // lazyMelds=true, lazyDecreaseKeys=true
    static final int BINOMIAL_WITH_CUTOFFS = 3;   // lazyMelds=false, lazyDecreaseKeys=true

    static String[] heapTypeNames = {
            "Binomial Heap (lazy=false, decrease=false)",
            "Lazy Binomial Heap (lazy=true, decrease=false)",
            "Fibonacci Heap (lazy=true, decrease=true)",
            "Binomial with Cutoffs (lazy=false, decrease=true)"
    };

    /**
     * Run a single trial of experiment 1
     */
    static ExperimentResults runExperiment1Trial(int[] perm, int heapType) {
        boolean lazyMelds;
        boolean lazyDecreaseKeys;

        if (heapType == BINOMIAL_HEAP) {
            lazyMelds = false;
            lazyDecreaseKeys = false;
        } else if (heapType == LAZY_BINOMIAL_HEAP) {
            lazyMelds = true;
            lazyDecreaseKeys = false;
        } else if (heapType == FIBONACCI_HEAP) {
            lazyMelds = true;
            lazyDecreaseKeys = true;
        } else { // BINOMIAL_WITH_CUTOFFS
            lazyMelds = false;
            lazyDecreaseKeys = true;
        }

        long startTime = System.currentTimeMillis();

        Heap heap = new Heap(lazyMelds, lazyDecreaseKeys);
        Heap.HeapNode[] pointers = new Heap.HeapNode[N + 1];
        OperationMetrics metrics = new OperationMetrics();

        // Insert N items in random order
        for (int i = 0; i < N; i++) {
            int key = perm[i];
            metrics.startTracking(heap);
            Heap.HeapItem nodeItem = heap.insert(key, "Item_" + key);
            // Heap.HeapNode node = heap.insert(key, "Item_" + key);
            metrics.recordOperation(heap);
            pointers[key] = nodeItem.node;
        }

        // Perform some decrease key operations
        for (int i = 1; i <= Math.min(1000, N); i++) {
            if (pointers[i] != null) {
                metrics.startTracking(heap);
                heap.decreaseKey(pointers[i].item, 1);
                metrics.recordOperation(heap);
            }
        }

        // Delete minimum
        metrics.startTracking(heap);
        heap.deleteMin();
        metrics.recordOperation(heap);

        long endTime = System.currentTimeMillis();
        long runtimeMs = endTime - startTime;

        return new ExperimentResults(
                runtimeMs,
                heap.totalLinks(),
                heap.totalCuts(),
                heap.totalHeapifyCosts(),
                metrics.getMaxOperationCost(),
                heap.numTrees(),
                heap.size()
        );
    }

    /**
     * Experiment 1: Insert n items in random order, then delete min (repeated 20 times)
     */
    static void experiment1() {
        System.out.println("\n========================================");
        System.out.println("EXPERIMENT 1: Insert Random + Delete Min");
        System.out.println("n = " + N);
        System.out.println("Number of trials: " + NUM_TRIALS);
        System.out.println("========================================\n");

        // Generate random permutation once
        int[] perm = generateRandomPermutation(N);

        for (int heapType = 0; heapType < NUM_HEAP_TYPES; heapType++) {
            long totalRuntime = 0;
            long totalLinks = 0;
            long totalCuts = 0;
            long totalHeapifyCosts = 0;
            long totalMaxOpCost = 0;

            System.out.println("Testing " + heapTypeNames[heapType] + "...");

            try {
                for (int trial = 0; trial < NUM_TRIALS; trial++) {
                    ExperimentResults results = runExperiment1Trial(perm, heapType);
                    totalRuntime += results.totalRuntime;
                    totalLinks += results.totalLinks;
                    totalCuts += results.totalCuts;
                    totalHeapifyCosts += results.totalHeapifyCosts;
                    totalMaxOpCost += results.maxOperationCost;
                    System.out.print(".");
                }
                System.out.println();

                // Get last results for numTrees and heapSize
                ExperimentResults lastResults = runExperiment1Trial(perm, heapType);

                // Report average metrics
                System.out.println("  Average Runtime: " + String.format("%.2f", totalRuntime / (double) NUM_TRIALS) + " ms");
                System.out.println("  Average Total Links: " + String.format("%.0f", totalLinks / (double) NUM_TRIALS));
                System.out.println("  Average Total Cuts: " + String.format("%.0f", totalCuts / (double) NUM_TRIALS));
                System.out.println("  Average Heapify Costs: " + String.format("%.0f", totalHeapifyCosts / (double) NUM_TRIALS));
                System.out.println("  Average Max Operation Cost: " + String.format("%.0f", totalMaxOpCost / (double) NUM_TRIALS));
                System.out.println("  Number of Trees: " + lastResults.numTrees);
                System.out.println("  Heap Size: " + lastResults.heapSize);
                System.out.println();
            } catch (Exception e) {
                System.err.println("  ERROR: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }
    }

    /**
     * Run a single trial of experiment 2
     */
    static ExperimentResults runExperiment2Trial(int[] perm, int heapType) {
        boolean lazyMelds;
        boolean lazyDecreaseKeys;

        if (heapType == BINOMIAL_HEAP) {
            lazyMelds = false;
            lazyDecreaseKeys = false;
        } else if (heapType == LAZY_BINOMIAL_HEAP) {
            lazyMelds = true;
            lazyDecreaseKeys = false;
        } else if (heapType == FIBONACCI_HEAP) {
            lazyMelds = true;
            lazyDecreaseKeys = true;
        } else { // BINOMIAL_WITH_CUTOFFS
            lazyMelds = false;
            lazyDecreaseKeys = true;
        }

        long startTime = System.currentTimeMillis();

        Heap heap = new Heap(lazyMelds, lazyDecreaseKeys);
        Heap.HeapItem[] pointers = new Heap.HeapItem[N + 1];
        boolean[] deleted = new boolean[N + 1];
        OperationMetrics metrics = new OperationMetrics();

        // Insert N items in random order
        for (int i = 0; i < N; i++) {
            int key = perm[i];
            metrics.startTracking(heap);
            Heap.HeapItem nodeItem = heap.insert(key, "Item_" + key);
            metrics.recordOperation(heap);
            pointers[key] = nodeItem;
        }

        // Delete minimum
        metrics.startTracking(heap);
        heap.deleteMin();
        metrics.recordOperation(heap);
        deleted[1] = true;

        // Decrease key of largest items to 0 until 46 items remain
        int itemsToDecrease = N - 1 - 46;
        int maxKey = N;

        while (itemsToDecrease > 0 && maxKey > 0) {
            // Find next non-deleted maximum
            while (maxKey > 0 && deleted[maxKey]) {
                maxKey--;
            }

            if (maxKey > 0 && pointers[maxKey] != null) {
                metrics.startTracking(heap);
                heap.delete(pointers[maxKey]);
                // heap.decreaseKey(pointers[maxKey].item, pointers[maxKey].item.key + 1);
                metrics.recordOperation(heap);
                deleted[maxKey] = true;
                itemsToDecrease--;
                maxKey--;
            } else {
                break;
            }
        }

        // Delete minimum again to remove all decreased items
/*        while (heap.size() > 46) {
            metrics.startTracking(heap);
            heap.deleteMin();
            metrics.recordOperation(heap);
        }*/

        long endTime = System.currentTimeMillis();
        long runtimeMs = endTime - startTime;

        return new ExperimentResults(
                runtimeMs,
                heap.totalLinks(),
                heap.totalCuts(),
                heap.totalHeapifyCosts(),
                metrics.getMaxOperationCost(),
                heap.numTrees(),
                heap.size()
        );
    }

    /**
     * Experiment 2: Insert n items, delete min, delete max until 46 items remain
     */
    static void experiment2() {
        System.out.println("\n========================================");
        System.out.println("EXPERIMENT 2: Insert + Delete Min + Delete Max");
        System.out.println("n = " + N + ", final size = 46");
        System.out.println("Number of trials: " + NUM_TRIALS);
        System.out.println("========================================\n");

        // Generate random permutation once
        int[] perm = generateRandomPermutation(N);

        for (int heapType = 0; heapType < NUM_HEAP_TYPES; heapType++) {
            long totalRuntime = 0;
            long totalLinks = 0;
            long totalCuts = 0;
            long totalHeapifyCosts = 0;
            long totalMaxOpCost = 0;

            System.out.println("Testing " + heapTypeNames[heapType] + "...");

            try {
                for (int trial = 0; trial < NUM_TRIALS; trial++) {
                    System.out.print(".");
                    System.out.flush();
                    ExperimentResults results = runExperiment2Trial(perm, heapType);
                    totalRuntime += results.totalRuntime;
                    totalLinks += results.totalLinks;
                    totalCuts += results.totalCuts;
                    totalHeapifyCosts += results.totalHeapifyCosts;
                    totalMaxOpCost += results.maxOperationCost;
                }
                System.out.println();

                // Get last results for numTrees and heapSize
                ExperimentResults lastResults = runExperiment2Trial(perm, heapType);

                // Report average metrics
                System.out.println("  Average Runtime: " + String.format("%.2f", totalRuntime / (double) NUM_TRIALS) + " ms");
                System.out.println("  Average Total Links: " + String.format("%.0f", totalLinks / (double) NUM_TRIALS));
                System.out.println("  Average Total Cuts: " + String.format("%.0f", totalCuts / (double) NUM_TRIALS));
                System.out.println("  Average Heapify Costs: " + String.format("%.0f", totalHeapifyCosts / (double) NUM_TRIALS));
                System.out.println("  Average Max Operation Cost: " + String.format("%.0f", totalMaxOpCost / (double) NUM_TRIALS));
                System.out.println("  Number of Trees: " + lastResults.numTrees);
                System.out.println("  Heap Size: " + lastResults.heapSize);
                System.out.println();
            } catch (Exception e) {
                System.err.println("  ERROR: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }
    }

    /**
     * Run a single trial of experiment 3
     */
    static ExperimentResults runExperiment3Trial(int[] perm, int heapType, int numDecreases) {
        boolean lazyMelds;
        boolean lazyDecreaseKeys;

        if (heapType == BINOMIAL_HEAP) {
            lazyMelds = false;
            lazyDecreaseKeys = false;
        } else if (heapType == LAZY_BINOMIAL_HEAP) {
            lazyMelds = true;
            lazyDecreaseKeys = false;
        } else if (heapType == FIBONACCI_HEAP) {
            lazyMelds = true;
            lazyDecreaseKeys = true;
        } else { // BINOMIAL_WITH_CUTOFFS
            lazyMelds = false;
            lazyDecreaseKeys = true;
        }

        long startTime = System.currentTimeMillis();

        Heap heap = new Heap(lazyMelds, lazyDecreaseKeys);
        Heap.HeapNode[] pointers = new Heap.HeapNode[N + 1];
        boolean[] deleted = new boolean[N + 1];
        OperationMetrics metrics = new OperationMetrics();

        // Insert N items in random order
        for (int i = 0; i < N; i++) {
            int key = perm[i];
            metrics.startTracking(heap);
            Heap.HeapItem nodeItem = heap.insert(key, "Item_" + key);
            metrics.recordOperation(heap);
            pointers[key] = nodeItem.node;
        }

        // Delete minimum
        metrics.startTracking(heap);
        heap.deleteMin();
        metrics.recordOperation(heap);
        deleted[1] = true;

        // Decrease key of ceil(n*0.1) largest items to 0
        int decreasedCount = 0;
        for (int i = N; i > 0 && decreasedCount < numDecreases; i--) {
            if (!deleted[i] && pointers[i] != null) {
                metrics.startTracking(heap);
                heap.decreaseKey(pointers[i].item, pointers[i].item.key);
                metrics.recordOperation(heap);
                decreasedCount++;
            }
        }

        // Delete minimum again
        metrics.startTracking(heap);
        heap.deleteMin();
        metrics.recordOperation(heap);

        long endTime = System.currentTimeMillis();
        long runtimeMs = endTime - startTime;

        return new ExperimentResults(
                runtimeMs,
                heap.totalLinks(),
                heap.totalCuts(),
                heap.totalHeapifyCosts(),
                metrics.getMaxOperationCost(),
                heap.numTrees(),
                heap.size()
        );
    }

    /**
     * Experiment 3: Insert n items, delete min, decrease max items to 0, then delete min again
     */
    static void experiment3() {
        System.out.println("\n========================================");
        System.out.println("EXPERIMENT 3: Insert + Delete Min + Decrease Max + Delete Min");
        System.out.println("n = " + N);
        System.out.println("Number of trials: " + NUM_TRIALS);
        System.out.println("========================================\n");

        int numDecreases = (int) Math.ceil(N * 0.1);
        System.out.println("Number of items to decrease: " + numDecreases + "\n");

        // Generate random permutation once
        int[] perm = generateRandomPermutation(N);

        for (int heapType = 0; heapType < NUM_HEAP_TYPES; heapType++) {
            long totalRuntime = 0;
            long totalLinks = 0;
            long totalCuts = 0;
            long totalHeapifyCosts = 0;
            long totalMaxOpCost = 0;

            System.out.println("Testing " + heapTypeNames[heapType] + "...");

            try {
                for (int trial = 0; trial < NUM_TRIALS; trial++) {
                    ExperimentResults results = runExperiment3Trial(perm, heapType, numDecreases);
                    totalRuntime += results.totalRuntime;
                    totalLinks += results.totalLinks;
                    totalCuts += results.totalCuts;
                    totalHeapifyCosts += results.totalHeapifyCosts;
                    totalMaxOpCost += results.maxOperationCost;
                    System.out.print(".");
                }
                System.out.println();

                // Get last results for numTrees and heapSize
                ExperimentResults lastResults = runExperiment3Trial(perm, heapType, numDecreases);

                // Report average metrics
                System.out.println("  Average Runtime: " + String.format("%.2f", totalRuntime / (double) NUM_TRIALS) + " ms");
                System.out.println("  Average Total Links: " + String.format("%.0f", totalLinks / (double) NUM_TRIALS));
                System.out.println("  Average Total Cuts: " + String.format("%.0f", totalCuts / (double) NUM_TRIALS));
                System.out.println("  Average Heapify Costs: " + String.format("%.0f", totalHeapifyCosts / (double) NUM_TRIALS));
                System.out.println("  Average Max Operation Cost: " + String.format("%.0f", totalMaxOpCost / (double) NUM_TRIALS));
                System.out.println("  Number of Trees: " + lastResults.numTrees);
                System.out.println("  Heap Size: " + lastResults.heapSize);
                System.out.println();
            } catch (Exception e) {
                System.err.println("  ERROR: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }
    }

    /**
     * Generate a random permutation of numbers 1 to n
     */
    static int[] generateRandomPermutation(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(i);
        }
        Collections.shuffle(list);
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = list.get(i);
        }
        return perm;
    }

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("==============================================");
        System.out.println("HEAP EXPERIMENTS");
        System.out.println("==============================================");
        System.out.println("\nChoose which experiment to run:");
        System.out.println("1 - Experiment 1: Insert Random + Delete Min");
        System.out.println("2 - Experiment 2: Insert + Delete Min + Delete Max");
        System.out.println("3 - Experiment 3: Insert + Delete Min + Decrease Max + Delete Min");
        System.out.println("4 - Run all experiments");
        System.out.print("\nEnter your choice (1-4): ");

        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                experiment1();
                break;
            case "2":
                experiment2();
                break;
            case "3":
                experiment3();
                break;
            case "4":
                experiment1();
                experiment2();
                experiment3();
                break;
            default:
                System.out.println("Invalid choice. Running all experiments...");
                experiment1();
                experiment2();
                experiment3();
        }

        System.out.println("==============================================");
        System.out.println("EXPERIMENTS COMPLETED");
        System.out.println("==============================================");
        scanner.close();
    }
}
