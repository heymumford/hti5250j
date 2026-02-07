package org.hti5250j.gui;
/*
=====================================================================

  ColumnComparator.java

  Created by Claude Duguay
  Copyright (c) 2002
   This was taken from a Java Pro magazine article
   http://www.fawcette.com/javapro/codepage.asp?loccode=jp0208

   I have NOT asked for permission to use this.

=====================================================================
*/

import java.util.*;

public class ColumnComparator implements Comparator {
    protected int index;
    protected boolean ascending;

    public ColumnComparator(int index, boolean ascending) {
        this.index = index;
        this.ascending = ascending;
    }

    public int compare(Object one, Object two) {
        if (one instanceof Vector vOne && two instanceof Vector vTwo) {
            Object oOne = vOne.elementAt(index);
            Object oTwo = vTwo.elementAt(index);
            if (oOne instanceof Comparable cOne && oTwo instanceof Comparable cTwo) {
                if (ascending) {
                    return cOne.compareTo(cTwo);
                } else {
                    return cTwo.compareTo(cOne);
                }
            }
        }

        return 1;
    }
}
