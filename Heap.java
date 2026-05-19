/**
 *
 * user1: id: ; username: ronishiri; name: Roni Shiri
 * user2: id: ; username: itaib1; name: Itai Ben-Shahar
 *
 * Heap
 *
 * An implementation of Fibonacci-like heap over positive integers
 * with optional lazy melds and optional lazy decrease-keys.
 *
 * 
 * Optimizations vs previous version:
 * 1) insert no longer allocates a temporary Heap; it adds root directly and, if !lazyMelds, runs successiveLinking().
 * 2) successiveLinking preallocates bucket array sized from log2(size)+3 to avoid repeated growth/copy.
 *    (Still no external data-structure libraries.)
 */


public class Heap {

    public final boolean lazyMelds;
    public final boolean lazyDecreaseKeys;


    public HeapItem min; // pointer to minimal item

    // internal state
    protected HeapNode roots; // circular doubly-linked list of roots (can be null)
    private int size;       // number of items
    private int numTrees;   // number of roots
    private int numMarked;  // number of marked nodes
    private int totalLinks;
    private int totalCuts;
    private int totalHeapifyCosts;

    /**
     * Constructor to initialize an empty heap.
     */
    public Heap(boolean lazyMelds, boolean lazyDecreaseKeys) {
        this.lazyMelds = lazyMelds;
        this.lazyDecreaseKeys = lazyDecreaseKeys;
        this.min = null;
        this.roots = null;
        this.size = 0;
        this.numTrees = 0;
        this.numMarked = 0;
        this.totalLinks = 0;
        this.totalCuts = 0;
        this.totalHeapifyCosts = 0;
    }

    /**
     * Insert (key,info) into the heap and return the newly generated HeapItem.
     * pre: key > 0
     */
    public HeapItem insert(int key, String info) {
        HeapNode node = new HeapNode();
        HeapItem item = new HeapItem();
        node.item = item;
        item.node = node;
        item.key = key;
        item.info = info;

        node.parent = null;
        node.child = null;
        node.rank = 0;
        node.marked = false;
        node.next = node.prev = node;

        // add to roots directly
        addRoot(node);
        this.size++;

        // if melds are eager, consolidate now
        if (!this.lazyMelds && this.numTrees > 1) {
            successiveLinking();
        }
        return item;
    }

    /**
     * Return the minimal HeapItem, null if empty.
     */
    public HeapItem findMin() {
        return this.min;
    }

    /**
     * Delete the minimal item.
     */
    public void deleteMin() {
        if (this.size == 0) {
            return;
        }
        HeapNode minNode = this.min.node;

        // detach children to roots
        if (minNode.child != null) {
            HeapNode child = minNode.child;
            HeapNode curr = child;
            do {
                curr.parent = null;
                if (curr.marked) {
                    curr.marked = false;
                    this.numMarked--;
                }
                curr = curr.next;
            } while (curr != child);

            roots = mergeRootLists(roots, child);
            this.numTrees += minNode.rank; // add all children as roots
        }

        // remove minNode from roots
        removeRoot(minNode);
        this.size--;

        if (this.size == 0) {
            this.min = null;
            this.roots = null;
            this.numTrees = 0;
            return;
        }

        // successive linking always happens in deleteMin
        successiveLinking();
    }

    /**
     * Decrease the key of x by diff and fix the heap.
     * pre: 0 <= diff <= x.key
     */
    public void decreaseKey(HeapItem x, int diff) {
        if (diff == 0) return;

        HeapNode node = x.node;
        int newKey = x.key - diff;
        x.key = newKey;

        HeapNode parent = node.parent;
        if (parent == null || parent.item.key <= newKey) {
            if (this.min == null || newKey < this.min.key) {
                this.min = x;
            }
            return;
        }

        if (this.lazyDecreaseKeys) {
            // Fibonacci-style cascading cuts
            cut(node);
            cascadingCut(parent);
        } else {
            // binomial-style heapify up (swap items only)
            int visited = heapifyUp(node);
            this.totalHeapifyCosts += visited;
        }

        if (this.min == null || x.key < this.min.key) {
            this.min = x;
        }
    }

    /**
     * Delete the x from the heap.
     */
    public void delete(HeapItem x) {
        decreaseKey(x, x.key);
        deleteMin();
    }

    /**
     * Meld the heap with heap2
     * pre: heap2.lazyMelds = this.lazyMelds AND heap2.lazyDecreaseKeys = this.lazyDecreaseKeys
     */
    public void meld(Heap heap2) {
        if (heap2 == null || heap2.size == 0) {
            return;
        }
        if (this.size == 0) {
            this.roots = heap2.roots;
            this.min = heap2.min;
            this.size = heap2.size;
            this.numTrees = heap2.numTrees;
            this.numMarked = heap2.numMarked;
            this.totalLinks = heap2.totalLinks;
            this.totalCuts = heap2.totalCuts;
            this.totalHeapifyCosts = heap2.totalHeapifyCosts;
        } else {
            this.roots = mergeRootLists(this.roots, heap2.roots);
            this.size += heap2.size;
            this.numTrees += heap2.numTrees;
            this.numMarked += heap2.numMarked;
            this.totalLinks += heap2.totalLinks;
            this.totalCuts += heap2.totalCuts;
            this.totalHeapifyCosts += heap2.totalHeapifyCosts;

            if (heap2.min != null && (this.min == null || heap2.min.key < this.min.key)) {
                this.min = heap2.min;
            }
        }

        // if melds are eager, consolidate now
        if (!this.lazyMelds) {
            successiveLinking();
        }
    }

    /**
     * Return the number of elements in the heap
     */
    public int size() {
        return this.size;
    }

    /**
     * Return the number of trees in the heap.
     */
    public int numTrees() {
        return this.numTrees;
    }

    /**
     * Return the number of marked nodes in the heap.
     */
    public int numMarkedNodes() {
        return this.numMarked;
    }

    /**
     * Return the total number of links.
     */
    public int totalLinks() {
        return this.totalLinks;
    }

    /**
     * Return the total number of cuts.
     */
    public int totalCuts() {
        return this.totalCuts;
    }

    /**
     * Return the total heapify costs.
     */
    public int totalHeapifyCosts() {
        return this.totalHeapifyCosts;
    }

    // ----------------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------------

    private void addRoot(HeapNode node) {
        if (this.roots == null) {
            this.roots = node;
            node.next = node.prev = node;
        } else {
            this.roots = mergeRootLists(this.roots, node);
        }
        this.numTrees++;
        if (this.min == null || node.item.key < this.min.key) {
            this.min = node.item;
        }
    }

    private void removeRoot(HeapNode node) {
        if (node.next == node) { // single root
            this.roots = null;
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            if (this.roots == node) {
                this.roots = node.next;
            }
        }
        this.numTrees--;
        node.next = node.prev = node;
    }

    private HeapNode mergeRootLists(HeapNode a, HeapNode b) {
        if (a == null) return b;
        if (b == null) return a;
        HeapNode aNext = a.next;
        HeapNode bPrev = b.prev;

        a.next = b;
        b.prev = a;

        bPrev.next = aNext;
        aNext.prev = bPrev;

        return a; // any head is fine
    }

    private HeapNode linkTrees(HeapNode x, HeapNode y) {
        // assumes x.item.key <= y.item.key
        y.parent = x;
        y.marked = false;

        if (x.child == null) {
            x.child = y;
            y.next = y.prev = y;
        } else {
            HeapNode c = x.child;
            y.next = c;
            y.prev = c.prev;
            c.prev.next = y;
            c.prev = y;
        }
        x.rank++;
        this.totalLinks++;
        return x;
    }

    private void successiveLinking() {
        if (this.roots == null) return;

        // preallocate buckets by estimated max rank ~ log2(size) + 3
        int maxRank = (int) Math.ceil(Math.log(this.size) / Math.log(2)) + 3;
        HeapNode[] buckets = new HeapNode[maxRank + 1];

        HeapNode start = this.roots;
        HeapNode curr = start;

        // reset before rebuild
        this.roots = null;
        this.min = null;
        this.numTrees = 0;

        boolean first = true;
        while (first || curr != start) {
            first = false;
            HeapNode next = curr.next;

            curr.parent = null;
            curr.next = curr.prev = curr;

            while (true) {
                int r = curr.rank;
                if (r >= buckets.length) {
                    // grow if underestimated
                    int newLen = buckets.length * 2;
                    while (r >= newLen) newLen *= 2;
                    HeapNode[] nb = new HeapNode[newLen];
                    System.arraycopy(buckets, 0, nb, 0, buckets.length);
                    buckets = nb;
                }

                if (buckets[r] == null) {
                    buckets[r] = curr;
                    break;
                } else {
                    HeapNode conflict = buckets[r];
                    buckets[r] = null;
                    if (conflict.item.key < curr.item.key) {
                        HeapNode tmp = curr;
                        curr = conflict;
                        conflict = tmp;
                    }
                    curr = linkTrees(curr, conflict);
                }
            }

            curr = next;
        }

        // rebuild roots from buckets
        for (int i = 0; i < buckets.length; i++) {
            HeapNode node = buckets[i];
            if (node != null) {
                this.roots = mergeRootLists(this.roots, node);
                this.numTrees++;
                if (this.min == null || node.item.key < this.min.key) {
                    this.min = node.item;
                }
            }
        }
    }

    private void cut(HeapNode node) {
        HeapNode parent = node.parent;
        if (parent == null) return;

        if (node.next == node) {
            parent.child = null;
        } else {
            node.next.prev = node.prev;
            node.prev.next = node.next;
            if (parent.child == node) {
                parent.child = node.next;
            }
        }
        parent.rank--;

        node.parent = null;
        node.next = node.prev = node;
        if (node.marked) {
            node.marked = false;
            this.numMarked--;
        }

        addRoot(node);
        if (!this.lazyMelds && this.numTrees > 1) {
            successiveLinking();
        }
        this.totalCuts++;
    }

    private void cascadingCut(HeapNode node) {
        HeapNode current = node;
        while (current != null && current.parent != null) {
            if (!current.marked) {
                current.marked = true;
                this.numMarked++;
                break;
            } else {
                HeapNode parent = current.parent;
                cut(current);
                current = parent;
            }
        }
    }

    private int heapifyUp(HeapNode node) {
        int visited = 1;
        HeapNode curr = node;
        while (curr.parent != null && curr.parent.item.key > curr.item.key) {
            HeapNode parent = curr.parent;
            HeapItem tmp = curr.item;
            curr.item = parent.item;
            parent.item = tmp;
            curr.item.node = curr;
            parent.item.node = parent;

            curr = parent;
            visited++;
        }
        return visited;
    }

    // ----------------------------------------------------------------------
    // Nested classes
    // ----------------------------------------------------------------------
    public static class HeapNode {
        public HeapItem item;
        public HeapNode child;
        public HeapNode next;
        public HeapNode prev;
        public HeapNode parent;
        public int rank;
        public boolean marked;
    }

    public static class HeapItem {
        public HeapNode node;
        public int key;
        public String info;
    }
}