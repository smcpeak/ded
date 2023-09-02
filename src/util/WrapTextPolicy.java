// WrapTextPolicy.java
// See toplevel license.txt for copyright and license terms.

package util;


/** Policy controlling the 'WrapText' class behavior. */
enum WrapTextPolicy {
    // Do not wrap the text at all.
    WTP_NoWrap,

    // Wrap only after punctuation that ends a sentence or has a
    // similar effect.
    WTP_Sentence,

    // Wrap on any whitespace.
    WTP_Whitespace,
};


// EOF
