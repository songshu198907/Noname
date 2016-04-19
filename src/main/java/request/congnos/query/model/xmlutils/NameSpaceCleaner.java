package request.congnos.query.model.xmlutils;

import org.dom4j.*;
import org.dom4j.tree.DefaultElement;

/**
 * Created by Heng Song on 3/31/2016.
 * This class is used to clean the namespace of
 * the xml file.
 */
public class NameSpaceCleaner extends VisitorSupport {
    @Override
    public void visit(Document document) {
        DefaultElement element = (DefaultElement) document.getRootElement();
        element.setNamespace(Namespace.NO_NAMESPACE);
        document.getRootElement().additionalNamespaces().clear();

    }

    @Override
    public void visit(Namespace namespace) {
        namespace.detach();
    }

    @Override
    public void visit(Attribute node) {
        if (node.toString().contains("xmlns")
                || node.toString().contains("xsi:")) {
            node.detach();
        }
    }

    @Override
    public void visit(Element node) {
        if (node instanceof DefaultElement) {
            ((DefaultElement) node).setNamespace(Namespace.NO_NAMESPACE);
        }
    }
}
