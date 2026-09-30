package com.mpp.aedialsworks.powertools.crafter;
import java.util.*;
import java.util.function.BiConsumer;
/** Extraction has a durable compensation sink: a rejected refund can never delete resources. */
public final class ResourceTransaction {
    private ResourceTransaction(){}
    public interface Port<K> { long available(K key,long amount); long extract(K key,long amount); }
    public static <K> boolean extract(Map<K,Long> requested,Port<K> port,BiConsumer<K,Long> refund){
        for(var e:requested.entrySet())if(e.getValue()<=0 || port.available(e.getKey(),e.getValue())<e.getValue())return false;
        Map<K,Long> taken=new LinkedHashMap<>();
        for(var e:requested.entrySet()){
            long n=port.extract(e.getKey(),e.getValue());if(n>0)taken.put(e.getKey(),n);
            if(n!=e.getValue()){taken.forEach(refund);return false;}
        }
        return true;
    }
    public static long saturatedMultiply(long a,long b){if(a<=0||b<=0)return 0;return a>Long.MAX_VALUE/b?Long.MAX_VALUE:a*b;}
}
