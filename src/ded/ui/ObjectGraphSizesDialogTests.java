// ObjectGraphSizesDialogTests.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.ObjectGraph;
import ded.model.ObjectGraphNode;

import ded.ui.ObjectGraphSizesDialog;


/** Tests for 'ObjectGraphSizesDialog'. */
public class ObjectGraphSizesDialogTests {
    public static void main(String[] args)
    {
        ObjectGraphSizesDialogTests t = new ObjectGraphSizesDialogTests();
        t.testTrim();
    }

    private void testTrim()
    {
        Diagram diagram = new Diagram();
        Entity entity = new Entity();
        entity.objectGraphNodeID = "n2";
        diagram.entities.add(entity);

        ObjectGraphNode n1 = new ObjectGraphNode("n1");
        ObjectGraphNode n2 = new ObjectGraphNode("n2");

        ObjectGraph graph = new ObjectGraph();
        graph.m_nodes.put("n1", n1);
        graph.m_nodes.put("n2", n2);
        assert(graph.m_nodes.size() == 2);

        diagram.objectGraph = graph;
        diagram.selfCheck();

        int numRemoved = ObjectGraphSizesDialog.trimGraph(diagram, graph);
        assert(numRemoved == 1);

        // After trimming, we should only have 'n2'.
        assert(graph.m_nodes.size() == 1);
        assert(graph.m_nodes.entrySet().iterator().next().getValue() == n2);

        graph.selfCheck();

        diagram.objectGraph = graph;
        diagram.selfCheck();
    }
}


// EOF
