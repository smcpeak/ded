// RelationEndpointController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Point;

import ded.model.RelationEndpoint;

/** Control the position of an endpoint of a Relation. */
public class RelationEndpointController extends ResizeController {
    // ------------------------ instance data --------------------------
    /** Relation controller we are a part of. */
    public RelationController rcontroller;

    /** The endpoint we are controlling. */
    public RelationEndpoint endpoint;

    // --------------------------- methods -----------------------------
    public RelationEndpointController(
        DiagramController diagramController,
        RelationController relationController,
        RelationEndpoint relationEndpoint)
    {
        super(diagramController);
        this.rcontroller = relationController;
        this.endpoint = relationEndpoint;

        this.selfCheck();
    }

    @Override
    public void edit()
    {
        // Similar to entity resize handles, delegate an edit
        // request to the underlying relation.
        this.rcontroller.edit();
    }

    @Override
    public Point getLoc()
    {
        return this.endpoint.getCenter();
    }

    @Override
    public void dragTo(Point pt)
    {
        this.endpoint.seLocationTo(this.diagramController.getRelationEndpoint(pt));

        // Do not set dirty bit.  Wait for mouse button release.
    }

    @Override
    public void selfCheck()
    {
        assert(this.diagramController.contains(this.rcontroller));
    }
}

// EOF
