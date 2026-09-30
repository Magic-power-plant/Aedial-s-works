package com.mpp.aedialsworks.cells;
import com.mpp.aedialsworks.cells.cell.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CellAccountingTest {
 @Test void multiplyBeforeDivideDoesNotLosePrecision(){assertEquals(Long.MAX_VALUE,CellMath.multiplyDivide(Long.MAX_VALUE,63,63));}
 @Test void saturatedArithmeticNeverWraps(){assertEquals(Long.MAX_VALUE,CellMath.multiply(Long.MAX_VALUE,9));assertEquals(Long.MAX_VALUE,CellMath.add(Long.MAX_VALUE,1));assertEquals(1152921504606846976L,CellMath.ceilDivide(Long.MAX_VALUE,8));}
 @Test void legacyTiersAndTypeOverhead(){assertEquals(8128,CellMath.capacity(1024,8,1,1,8));assertEquals(2147483648L,CellTier.G2.bytes);assertEquals(0,CellMath.capacity(10,8,2,1,8));}
 @Test void highDensityExceedsInt(){assertEquals(17454747082816L,CellMath.capacity(1024,8,1,Integer.MAX_VALUE,8));}
 @Test void simulationsAndPersistenceKeepCounts(){var l=new LongLedger<String>();assertEquals(5000000000L,l.insert("iron",5000000000L,Long.MAX_VALUE,63,Long.MAX_VALUE,true));assertEquals(0,l.total());l.restore("iron",5000000000L);assertEquals(4000000000L,l.extract("iron",4000000000L,false));assertEquals(1000000000L,l.get("iron"));}
 @Test void totalAndPerTypeLimitsAreIndependent(){var l=new LongLedger<String>();assertEquals(50,l.insert("a",90,100,2,50,false));assertEquals(50,l.insert("b",90,100,2,50,false));assertEquals(0,l.insert("c",1,100,2,50,false));assertEquals(100,l.total());}
 @Test void reducedCapacityPreservesRecoverableInventory(){var l=new LongLedger<String>();l.restore("iron",Long.MAX_VALUE);assertEquals(0,l.insert("iron",1,10,1,10,false));assertEquals(Long.MAX_VALUE,l.extract("iron",Long.MAX_VALUE,false));assertEquals(0,l.types());}
 @Test void rejectsCorruptOverflowAndDuplicateRows(){var l=new LongLedger<String>();l.restore("a",Long.MAX_VALUE);assertThrows(IllegalArgumentException.class,()->l.restore("b",1));assertThrows(IllegalArgumentException.class,()->l.restore("a",1));}
}
