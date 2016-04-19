package request.congnos.query.model.tree;

import java.io.Serializable;

/**
 * Created by Heng Song on 4/5/2016.
 */
public class BPTree implements TreeOp, Serializable {
    private static final long serialVersionUID = 1L;
    protected int totalNodes;
    protected TreeNode root;
    protected int order;
    protected TreeNode head;

    public BPTree(int order) {
        if (order < 3) {
            throw new RuntimeException("Order of the tree must be greater than 2");
        }

        this.order = order;
        root = new TreeNode(true, true);
        head = root;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public TreeNode getHead() {
        return head;
    }

    public void setHead(TreeNode head) {
        this.head = head;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    @Override
    public Object get(Comparable key) {
        return root.get(key);

    }

    @Override
    public void remove(Comparable key) {
        root.remove(key, this);
    }

    @Override
    public synchronized void insertOrUpdate(Comparable key, Object obj) {
        totalNodes++;
        root.insertOrUpdate(key, obj, this);
    }
}
