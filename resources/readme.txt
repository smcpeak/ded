helvR12sm.ttf is derived from helvR12.pcf.gz in
/usr/share/fonts/X11/75dpi in my Debian linux distro.  I believe the
X11 fonts are all free.  Using 'fontforge', I fixed one problem, which
is that the brackets would run together in "[]"; I fixed this by
increasing the width of ']' from 3 to 4 and moving it one pixel to the
right in its box.  It is saved as TTF because that is what Swing wants
to load, but it is a bitmap font and only looks right at 12pt.

