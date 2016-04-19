package request.congnos.query.model.tree;

/**
 * Created by Heng Song on 4/5/2016.
 */
public interface TreeOp {
    Object get(Comparable key);

    void remove(Comparable key);

    void insertOrUpdate(Comparable key, Object obj);

}
