// RoutingAlgorithm.java

package ded.model;

/** Routing algorithms for relations (arrows). */
public enum RoutingAlgorithm {
    RA_DIRECT                                    // direct, straight line            
        ("Direct"),
    RA_MANHATTAN_HORIZ                           // horiz/vert only, start horiz
        ("Manhattan (Horiz. First)"),
    RA_MANHATTAN_VERT                            // horiz/vert only, start vert
        ("Manhattan (Vert. First)");
    
    /** How the algorithm is described in the UI. */
    public String description;
    
    RoutingAlgorithm(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return this.description;
    }
    
    /** True if this is one of the Manhattan algorithms. */
    public boolean isManhattan()
    {
        return this == RA_MANHATTAN_HORIZ || this == RA_MANHATTAN_VERT;
    }
}

// EOF
