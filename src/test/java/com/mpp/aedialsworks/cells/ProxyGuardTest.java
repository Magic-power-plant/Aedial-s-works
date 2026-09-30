package com.mpp.aedialsworks.cells;
import com.mpp.aedialsworks.cells.subnetproxy.ProxyGuard;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProxyGuardTest {
 @Test void directAndLongCyclesAreDetected(){var g=Map.of("A",List.of("B"),"B",List.of("C"),"C",List.of("A"));assertTrue(ProxyGuard.reaches("A","C",n->g.getOrDefault(n,List.of())));assertFalse(ProxyGuard.reaches("A","D",n->g.getOrDefault(n,List.of())));}
 @Test void diamondsDoNotLoop(){var g=Map.of(1,List.of(2,3),2,List.of(4),3,List.of(4));assertTrue(ProxyGuard.reaches(1,4,n->g.getOrDefault(n,List.of())));assertFalse(ProxyGuard.reaches(4,1,n->g.getOrDefault(n,List.of())));}
 @Test void hopGuardIsReleasedOnException(){assertFalse(ProxyGuard.Hop.nested());try(var hop=ProxyGuard.Hop.enter()){assertTrue(ProxyGuard.Hop.nested());throw new IllegalStateException();}catch(IllegalStateException ignored){}assertFalse(ProxyGuard.Hop.nested());}
}
