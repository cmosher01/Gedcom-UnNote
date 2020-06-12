# Gedcom-UnNote

Copyright © 2017, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CVSSQ2BWDCKQ2)
[![License](https://img.shields.io/github/license/cmosher01/Gedcom-UnNote.svg)](https://www.gnu.org/licenses/gpl.html)

In GEDCOM files, converts between inline `NOTE`s and `NOTE` records, and
removes empty `NOTE` records.

### delete empty notes
    0 @I1@ INDI
         1 NAME Testing /Tester/
         1 NOTE @N1@
    0 @I2@ INDI
         1 NAME QA /Tester/
         1 NOTE @N1@
    0 @N1@ NOTE
         1 CONT
         1 CONT

gets changed to this:

    0 @I1@ INDI
         1 NAME Testing /Tester/
    0 @I2@ INDI
         1 NAME QA /Tester/

### make note inline/record

Converts between `NOTE` record:

    0 @I1@ INDI
         1 NAME Testing /Tester/
         1 NOTE @N1@
    0 @N1@ NOTE string

and inline `NOTE`:

    0 @I1@ INDI
         1 NAME Testing /Tester/
         1 NOTE string

However, if there are multiple references to a given `NOTE`
record, then it will *not* be inlined.
