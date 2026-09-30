package com.mpp.aedialsworks.powertools;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.mpp.aedialsworks.powertools.crafter.ResourceTransaction;
import com.mpp.aedialsworks.powertools.monitor.Comparison;
class ResourceTransactionTest {
    @Test void unavailableInputsNeverExtract(){int[] calls={0};assertFalse(ResourceTransaction.extract(Map.of("iron",9L),new ResourceTransaction.Port<String>(){public long available(String k,long n){return 8;}public long extract(String k,long n){calls[0]++;return n;}},(k,n)->fail()));assertEquals(0,calls[0]);}
    @Test void partialExtractionIsFullyRefunded(){var request=new LinkedHashMap<String,Long>();request.put("iron",9L);request.put("gold",4L);var refund=new HashMap<String,Long>();assertFalse(ResourceTransaction.extract(request,new ResourceTransaction.Port<String>(){public long available(String k,long n){return n;}public long extract(String k,long n){return k.equals("gold")?2:n;}},refund::put));assertEquals(Map.of("iron",9L,"gold",2L),refund);}
    @Test void successDoesNotRefund(){assertTrue(ResourceTransaction.extract(Map.of("iron",Long.MAX_VALUE),new ResourceTransaction.Port<String>(){public long available(String k,long n){return n;}public long extract(String k,long n){return n;}},(k,n)->fail()));}
    @Test void multiplicationSaturatesAndKeepsExactSmallValues(){assertEquals(Long.MAX_VALUE,ResourceTransaction.saturatedMultiply(Long.MAX_VALUE,4096));assertEquals(4096000000L,ResourceTransaction.saturatedMultiply(1000000,4096));assertEquals(0,ResourceTransaction.saturatedMultiply(0,8));}
    @Test void allComparisonsWorkBeyondIntegerRange(){long n=1L<<40;assertTrue(Comparison.LESS.test(n,n+1));assertTrue(Comparison.LESS_EQUAL.test(n,n));assertTrue(Comparison.EQUAL.test(n,n));assertTrue(Comparison.GREATER_EQUAL.test(n,n));assertTrue(Comparison.GREATER.test(n+1,n));assertTrue(Comparison.NOT_EQUAL.test(n,n+1));assertFalse(Comparison.LESS.test(Long.MAX_VALUE,Long.MAX_VALUE));}
}
