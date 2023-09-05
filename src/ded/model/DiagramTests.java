// DiagramTests.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.List;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.ObjectGraph;
import ded.model.ObjectGraphNode;
import ded.model.Relation;


/** Tests for 'Diagram' class. */
public class DiagramTests {
    public static void main(String[] args)
    {
        DiagramTests t = new DiagramTests();
        t.testFixObjectGraphEntityNeighbors();
    }

    private void testFixObjectGraphEntityNeighbors()
    {
        Diagram diagram = new Diagram();

        ObjectGraph graph = new ObjectGraph();
        diagram.objectGraph = graph;

        ObjectGraphNode n1 = new ObjectGraphNode("n1");
        graph.addNode(n1);
        n1.addPointerTarget("rel", "n2");

        ObjectGraphNode n2 = new ObjectGraphNode("n2");
        graph.addNode(n2);

        Entity e1 = new Entity();
        diagram.entities.add(e1);
        e1.objectGraphNodeID = n1.m_id;

        Entity e2 = new Entity();
        diagram.entities.add(e2);
        e2.objectGraphNodeID = "wrong ID";

        Relation r12 = new Relation(
            new RelationEndpoint(e1),
            new RelationEndpoint(e2));
        diagram.relations.add(r12);
        r12.label = "rel";

        // First, verify that 'checkObjectGraphLinks' sees the problem.
        {
            List<String> issues = diagram.checkObjectGraphLinks();
            assert(issues.size() == 2);

            assert(issues.get(0).equals(
                "Entity { ID=\"wrong ID\", x=0, y=0 } "+
                "has an invalid object graph ID."));
            assert(issues.get(1).equals(
                "Relation \"rel\" from entity { ID=\"n1\", x=0, y=0 } "+
                "is connected to entity { ID=\"wrong ID\", x=0, y=0 }, "+
                "but is expected to be connected to an entity with "+
                "ID \"n2\"."));
        }

        // Fix the problem.
        assert(diagram.canFixObjectGraphEntityNeighbors(e1));
        int ct = diagram.fixObjectGraphEntityNeighbors(e1);
        assert(ct == 1);

        // Now the check routine should be happy.
        assert(diagram.checkObjectGraphLinks().isEmpty());

        // And the ID should be correct.
        assert(e2.objectGraphNodeID.equals("n2"));
    }
}


// EOF
