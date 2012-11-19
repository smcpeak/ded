// SerializationTests.java

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
        t.test1();
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
        
        // Serialize it.
        String serialized = d.toJSON().toString(2);
        System.out.println(serialized);
        
        // Parse it.
        JSONObject o = new JSONObject(new JSONTokener(serialized));
        Diagram d2 = new Diagram();
        d2.fromJSON(o);
        
        // Check for structural equality.
        assert(d2.equals(d));
        
        // Serialize and check that for equality too.
        String ser2 = d2.toJSON().toString(2);
        assert(ser2.equals(serialized));
    }
}

// EOF
