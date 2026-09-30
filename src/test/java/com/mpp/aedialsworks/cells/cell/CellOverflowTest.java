package com.mpp.aedialsworks.cells.cell;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CellOverflowTest {
    @Test void overflowOnlyVoidsStoredTypes(){assertFalse(AbstractCellState.canVoidOverflow(true,0));assertTrue(AbstractCellState.canVoidOverflow(true,1));assertFalse(AbstractCellState.canVoidOverflow(false,1));}
    @Test void fullCellRejectsNewTypeInsteadOfVoiding(){var l=new LongLedger<String>();assertEquals(50,l.insert("a",50,50,1,Long.MAX_VALUE,false));assertEquals(0,l.insert("b",1,50,1,Long.MAX_VALUE,false));assertEquals(0,l.get("b"));assertEquals(50,l.total());}
}
