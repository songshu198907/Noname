package request.congnos.query.model.tree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Heng Song on 4/5/2016.
 */
public class TreeNode implements Serializable {
    private static final long serialVersionUID = 1L;
    protected boolean isLeaf;
    protected boolean isRoot;
    protected TreeNode parent;
    protected TreeNode previous;
    protected TreeNode next;
    protected List<Map.Entry<Comparable, Object>> entries;
    protected List<TreeNode> children;

    public TreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        entries = new ArrayList<>();
        if (!isLeaf) {
            children = new ArrayList<>();
        }

    }

    public TreeNode(boolean isLeaf, boolean isRoot) {
        this(isLeaf);
        this.isRoot = isRoot;
    }

    protected static void validate(TreeNode node, BPTree tree) {

        if (node.getEntries().size() == node.getChildren().size()) {
            for (int i = 0; i < node.getEntries().size(); i++) {
                Comparable key = node.getChildren().get(i).getEntries().get(0).getKey();
                if (node.getEntries().get(i).getKey().compareTo(key) != 0) {
                    node.getEntries().remove(i);
                    node.getEntries().add(i, new AbstractMap.SimpleEntry<>(key, null));
                    if (!node.isRoot()) {
                        validate(node.getParent(), tree);
                    }
                }
            }
        } else if (node.isRoot() && node.getChildren().size() >= 2
                || node.getChildren().size() >= tree.getOrder() / 2
                && node.getChildren().size() <= tree.getOrder()
                && node.getChildren().size() >= 2) {
            node.getEntries().clear();
            for (int i = 0; i < node.getChildren().size(); i++) {
                Comparable key = node.getChildren().get(i).getEntries().get(0).getKey();
                node.getEntries().add(new AbstractMap.SimpleEntry<>(key, null));
                if (!node.isRoot()) {

                    validate(node.getParent(), tree);
                }
            }
        }
    }

    public Object get(Comparable key) {
        if (isLeaf) {
            for (Map.Entry<Comparable, Object> entry : entries) {
                if (entry.getKey().compareTo(key) == 0) {
                    return entry.getValue();
                }
            }
            return null;
        } else {
            if (key.compareTo(entries.get(0).getKey()) <= 0) {
                return children.get(0).get(key);
            } else if (key.compareTo(entries.get(entries.size() - 1).getKey()) >= 0) {
                return children.get(children.size() - 1).get(key);
            } else {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getKey().compareTo(key) <= 0
                            &&
                            entries.get(i + 1).getKey().compareTo(key) > 0) {
                        return children.get(i).get(key);
                    }
                }
            }

            return null;
        }

    }

    protected void insertOrUpdate(Comparable key, Object obj, BPTree tree) {
        if (isLeaf) {
            if (contains(key) || entries.size() < tree.getOrder()) {
                insertOrUpdate(key, obj);
                if (parent != null) {
                    parent.updateInsert(tree);
                }
            } else {
                TreeNode left = new TreeNode(true);
                TreeNode right = new TreeNode(true);
                if (previous != null) {
                    previous.setNext(left);
                    left.setPrevious(previous);
                }
                if (next != null) {
                    next.setPrevious(right);
                    right.setNext(next);
                }
                if (previous == null) {
                    tree.setHead(left);
                }
                left.setNext(right);
                right.setPrevious(left);
                previous = null;
                next = null;
                int leftSize = (tree.getOrder() + 1) / 2 + (tree.getOrder() + 1) % 2;
                int rightSize = (tree.getOrder() + 1) / 2;
                insertOrUpdate(key, obj);
                for (int i = 0; i < leftSize; i++) {
                    left.getEntries().add(entries.get(i));
                }
                for (int i = 0; i < rightSize; i++) {
                    right.getEntries().add(entries.get(leftSize + i));
                }
                if (parent != null) {
                    int index = parent.getChildren().indexOf(this);
                    parent.getChildren().remove(this);
                    left.setParent(parent);
                    right.setParent(parent);
                    parent.getChildren().add(index, left);
                    parent.getChildren().add(index + 1, right);
                    setEntries(null);
                    setChildren(null);
                    parent.updateInsert(tree);
                    setParent(null);

                } else {
                    isRoot = false;
                    TreeNode parent = new TreeNode(false, true);
                    tree.setRoot(parent);
                    left.setParent(parent);
                    right.setParent(parent);
                    parent.getChildren().add(left);
                    parent.getChildren().add(right);
                    setEntries(null);
                    setChildren(null);
                    parent.updateInsert(tree);
                }


            }
        } else {
            if (key.compareTo(entries.get(0).getKey()) <= 0) {
                children.get(0).insertOrUpdate(key, obj, tree);
            } else if (key.compareTo(entries.get(entries.size() - 1).getKey()) >= 0) {
                children.get(children.size() - 1).insertOrUpdate(key, obj, tree);
            } else {
                for (int node = 0; node < entries.size(); node++) {
                    if (entries.get(node).getKey().compareTo(key) <= 0 && entries.get(node + 1).getKey().compareTo(key) > 0) {
                        children.get(node).insertOrUpdate(key, obj, tree);
                        break;
                    }
                }
            }
        }

    }

    protected void updateInsert(BPTree tree) {
        validate(this, tree);
        if (children.size() > tree.getOrder()) {
            TreeNode left = new TreeNode(false);
            TreeNode right = new TreeNode(false);
            int rightSize = (tree.getOrder() + 1) / 2;
//            int leftSize = tree.getOrder() - rightSize;
            int leftSize = (tree.getOrder() + 1) / 2 + (tree.getOrder() + 1) % 2;

            for (int i = 0; i < leftSize; i++) {
                left.getChildren().add(children.get(i));
                left.entries.add(new AbstractMap.SimpleEntry<>(children.get(i).getEntries().get(0).getKey(), null));
                children.get(i).setParent(left);
            }
            for (int i = 0; i < rightSize; i++) {
                right.getChildren().add(children.get(leftSize + i));
                right.entries.add(new AbstractMap.SimpleEntry<>(children.get(leftSize + i).getEntries().get(0).getKey(), null));
                children.get(leftSize + i).setParent(right);

            }
            if (parent != null) {
                int index = parent.getChildren().indexOf(this);
                parent.children.remove(this);
                left.setParent(parent);
                right.setParent(parent);
                parent.children.add(index, left);
                parent.children.add(index + 1, right);
                setEntries(null);
                setChildren(null);
                parent.updateInsert(tree);
                setParent(null);

            } else {
                TreeNode parent = new TreeNode(false, true);
                isRoot = false;
                tree.setRoot(parent);
                left.setParent(parent);
                right.setParent(parent);
                parent.children.add(left);
                parent.children.add(right);
                setEntries(null);
                setChildren(null);
                parent.updateInsert(tree);

            }
        }

    }

    protected void updateRemove(BPTree tree) {
        validate(this, tree);
        if (children.size() < tree.getOrder() / 2 || children.size() < 2) {
            if (isRoot) {
                if (children.size() >= 2) {
                    return;
                } else {
                    TreeNode root = children.get(0);
                    tree.setRoot(root);
                    root.setParent(null);
                    root.setRoot(true);
                    setEntries(null);
                    setChildren(null);
                }
            } else {
                int currIdx = parent.getChildren().indexOf(this);
                int prevIdx = currIdx - 1;
                int nextIdx = currIdx + 1;
                TreeNode previous = null, next = null;
                if (prevIdx >= 0) {
                    previous = parent.getChildren().get(prevIdx);
                }
                if (nextIdx < parent.getChildren().size()) {
                    next = parent.getChildren().get(nextIdx);
                }
                if (previous != null
                        &&
                        previous.getChildren().size() > 2
                        &&
                        previous.getChildren().size() > tree.getOrder() / 2
                        ) {
                    int index = previous.getChildren().size() - 1;
                    TreeNode borrow = previous.getChildren().get(index);
                    previous.getChildren().remove(index);
                    borrow.setParent(this);
                    children.add(0, borrow);
                    validate(previous, tree);
                    validate(this, tree);
                    parent.updateRemove(tree);
                } else if (next != null &&
                        next.getChildren().size() > 2 &&
                        next.getChildren().size() > tree.getOrder() / 2) {
                    TreeNode borrow = next.getChildren().get(0);
                    next.getChildren().remove(0);
                    borrow.setParent(this);
                    children.add(borrow);
                    validate(this, tree);
                    validate(next, tree);
                    parent.updateRemove(tree);
                } else {
                    if (previous != null &&
                            (previous.getChildren().size() <= tree.getOrder() / 2 ||
                                    previous.getChildren().size() <= 2
                            )
                            ) {
                        for (int i = previous.getChildren().size() - 1; i >= 0; i--) {
                            TreeNode node = previous.getChildren().get(i);
                            children.add(0, node);
                            node.setParent(this);
                        }
                        previous.setChildren(null);
                        previous.setEntries(null);
                        previous.setParent(null);
                        parent.getChildren().remove(previous);
                        validate(this, tree);
                        parent.updateRemove(tree);
                    } else if (next != null &&
                            (next.getChildren().size() <= 2 || next.getChildren().size() <= tree.getOrder() / 2)
                            ) {
                        for (int i = 0; i < next.getChildren().size(); i++) {
                            TreeNode node = next.getChildren().get(i);
                            node.setParent(this);
                            children.add(node);
                        }
                        next.setParent(null);
                        next.setChildren(null);
                        next.setEntries(null);
                        parent.getChildren().remove(next);
                        validate(this, tree);
                        parent.updateRemove(tree);
                    }
                }
            }
        }
    }

    protected void remove(Comparable key, BPTree tree) {
        if (isLeaf) {
            if (!contains(key)) {
                return;
            }
            if (isRoot) {
                remove(key);
            } else {
                if (entries.size() > 2 && entries.size() > tree.getOrder() / 2) {
                    remove(key);
                } else {
                    if (previous != null &&
                            previous.getEntries().size() > 2 &&
                            previous.getEntries().size() > tree.getOrder() / 2 &&
                            previous.getParent() == parent) {
                        int index = previous.getEntries().size() - 1;
                        Map.Entry<Comparable, Object> entry = previous.getEntries().get(index);
                        previous.getEntries().remove(entry);
                        entries.add(0, entry);
                        remove(key);

                    } else if (next != null &&
                            next.getEntries().size() > 2 &&
                            next.getEntries().size() > tree.getOrder() / 2 &&
                            next.getParent() == parent) {
                        Map.Entry entry = next.getEntries().get(0);
                        next.entries.remove(entry);
                        entries.add(entry);
                        remove(key);
                    } else {
                        if (previous != null && (
                                previous.getEntries().size() <= 2 ||
                                        previous.getEntries().size() <= tree.getOrder() / 2) &&
                                previous.getParent() == parent) {
                            for (int i = previous.getEntries().size() - 1; i >= 0; i--) {
                                Map.Entry entry = previous.getEntries().get(i);
                                entries.add(0, entry);
                            }
                            remove(key);
                            previous.setEntries(null);
                            previous.setParent(null);
                            parent.getChildren().remove(previous);
                            if (previous.getPrevious() != null) {
                                TreeNode tmp = previous.getPrevious();
                                tmp.setNext(this);
                                this.setPrevious(tmp);


                            } else {
                                tree.setHead(this);
                                previous.setNext(null);
                                previous = null;
                            }
                        } else if (next != null && (
                                next.getEntries().size() <= 2 ||
                                        next.getEntries().size() <= tree.getOrder() / 2
                        ) && next.getParent() == parent) {
                            for (int i = 0; i < next.entries.size(); i++) {
                                Map.Entry entry = next.entries.get(i);
                                entries.add(entry);
                            }
                            remove(key);
                            next.setParent(null);
                            next.setEntries(null);
                            parent.getChildren().remove(next);
                            if (next.getNext() != null) {
                                TreeNode tmp = next.getNext();
                                tmp.setPrevious(this);
                                this.setNext(tmp);
                            } else {

                                next = null;
                            }

                        }

                    }
                }
            }
            parent.updateRemove(tree);
        } else {
            if (key.compareTo(entries.get(0).getKey()) <= 0) {
                children.get(0).remove(key, tree);
            } else if (key.compareTo(entries.get(entries.size() - 1).getKey()) >= 0) {
                children.get(children.size() - 1).remove(key, tree);
            } else {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getKey().compareTo(key) <= 0 && entries.get(i + 1).getKey().compareTo(key) > 0) {
                        TreeNode child = children.get(i);

                        child.remove(key, tree);
                        break;
                    }
                }
            }
        }

    }

    protected boolean contains(Comparable key) {
        for (Map.Entry entry : entries) {
            if (key.compareTo(entry.getKey()) == 0) {
                return true;
            }
        }
        return false;
    }

    protected void insertOrUpdate(Comparable key, Object obj) {
        Map.Entry<Comparable, Object> entry = new AbstractMap.SimpleEntry<>(key, obj);
        if (entries.size() == 0) {
            entries.add(entry);
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().compareTo(key) == 0) {
                entries.get(i).setValue(obj);
                return;
            } else if (entries.get(i).getKey().compareTo(key) > 0) {
                entries.add(i, entry);
                return;
            }
        }
        entries.add(entry);

    }

    public void remove(Comparable key) {
        int index = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().compareTo(key) == 0) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            entries.remove(index);
        }

    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public TreeNode getPrevious() {
        return previous;
    }

    public void setPrevious(TreeNode previous) {
        this.previous = previous;
    }

    public TreeNode getNext() {
        return next;
    }

    public void setNext(TreeNode next) {
        this.next = next;
    }

    public List<Map.Entry<Comparable, Object>> getEntries() {
        return entries;
    }

    public void setEntries(List<Map.Entry<Comparable, Object>> entries) {
        this.entries = entries;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("isRoot: ");
        sb.append(isRoot);
        sb.append(", ");
        sb.append("isLeaf: ");
        sb.append(isLeaf);
        sb.append(", ");
        sb.append("keys: ");
        for (Map.Entry entry : entries) {
            sb.append(entry.getKey());
            sb.append(", ");
        }
        sb.append(", ");
        return sb.toString();
    }
}
