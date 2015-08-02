// SerializationTests.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.awt.Dimension;
import java.awt.Point;

import org.json.JSONObject;
import org.json.JSONTokener;

/** Test serialization of Diagram. */
public class SerializationTests {
    /** Run the tests.  Throw exception on failure. */
    public static void main(String args[]) throws Exception
    {
        SerializationTests t = new SerializationTests();

        // Run unit tests when run w/o arguments.
        if (args.length == 0) {
            t.test1();
        }

        // Parse inputs specified on command line.
        for (String a : args) {
            testParseFile(a);
        }
    }

    public void test1() throws Exception
    {
        // Build a simple diagram.
        Diagram d = new Diagram();
        d.windowSize = new Dimension(1000,2000);

        Entity e1 = new Entity();
        e1.loc = new Point(5,10);
        e1.size = new Dimension(30,40);
        e1.shape = EntityShape.ES_ELLIPSE;
        e1.name = "e1";
        e1.attributes = "attr1\nattr2\nattr3";
        d.entities.add(e1);

        Entity e2 = new Entity();
        e2.loc = new Point(15,20);
        e2.size = new Dimension(130,140);
        e2.shape = EntityShape.ES_NO_SHAPE;
        e2.name = "e2";
        e2.attributes = "funny\"characters\\in\'this,string!";
        d.entities.add(e2);

        // Relation from e1 to e2 with two control points.
        Relation r1 = new Relation(new RelationEndpoint(e1),
                                   new RelationEndpoint(e2));
        r1.controlPts.add(new Point(71,72));
        r1.controlPts.add(new Point(73,74));
        r1.routingAlg = RoutingAlgorithm.RA_DIRECT;
        r1.label = "r1";
        r1.end.arrowStyle = ArrowStyle.AS_FILLED_TRIANGLE;
        r1.start.arrowStyle = ArrowStyle.AS_DOUBLE_ANGLE;
        d.relations.add(r1);

        // Relation between two points.
        Relation r2 = new Relation(new RelationEndpoint(new Point(81,82)),
                                   new RelationEndpoint(new Point(83,84)));
        d.relations.add(r2);

        // Make e2 inherit from e1.
        Inheritance i1 = new Inheritance(e1, true /*open*/, new Point(31,32));
        d.inheritances.add(i1);
        Relation r3 = new Relation(new RelationEndpoint(e2),
                                   new RelationEndpoint(i1));
        r3.routingAlg = RoutingAlgorithm.RA_MANHATTAN_VERT;
        d.relations.add(r3);

        // Make sure it is all consistent.
        d.selfCheck();

        // Serialize it.
        String serialized = d.toJSON().toString(2);
        //System.out.println(serialized);

        // Parse it.
        JSONObject o = new JSONObject(new JSONTokener(serialized));
        Diagram d2 = new Diagram(o);
        d2.selfCheck();

        // Check for structural equality.
        assert(d2.equals(d));

        // Serialize and check that for equality too.
        String ser2 = d2.toJSON().toString(2);
        assert(ser2.equals(serialized));

        // Make a deep copy.
        Diagram d3 = d.deepCopy();
        assert(d3.equals(d));
        assert(d2.equals(d3));

        // Serialize and check.
        String ser3 = d3.toJSON().toString(2);
        assert(ser3.equals(serialized));
    }

    private static void testParseFile(String fname) throws Exception
    {
        System.out.println("testing: "+fname);

        // Parse the file, checking that we can.
        Diagram d = Diagram.readFromFileAutodetect(fname);
        d.selfCheck();

        // Put it through a serialization cycle.
        String serialized = d.toJSON().toString(2);
        Diagram d2 = new Diagram(new JSONObject(new JSONTokener(serialized)));
        d2.selfCheck();

        // The deserialized objects should be equal.
        assert(d.equals(d2));

        // The serialized string form might be different from what was
        // in the file if we loaded an older version.  But if we serialize
        // again, *that* should match 'serialized'.
        String ser2 = d2.toJSON().toString(2);
        assert(serialized.equals(ser2));

        // Make a deep copy.
        Diagram d3 = d.deepCopy();
        assert(d3.equals(d));
        assert(d2.equals(d3));

        // Serialize and check.
        String ser3 = d3.toJSON().toString(2);
        assert(ser3.equals(serialized));
    }
}

// EOF
