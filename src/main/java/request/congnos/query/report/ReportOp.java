package request.congnos.query.report;

import java.io.File;

/**
 * Created by Heng Song on 4/8/2016.
 */
public interface ReportOp {
    void load(String fileName);

    void parse();

    Object search(String name);

    void export(File file);
}
