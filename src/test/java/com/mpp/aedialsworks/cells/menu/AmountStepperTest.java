package com.mpp.aedialsworks.cells.menu;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AmountStepperTest {
    private static final List<String> OFFSETS=List.of("1","10","100","1000");
    @Test void incrementsAndDecrementsLargeQuantities(){
        assertEquals(4294967306L,AmountStepper.step(4294967296L,true,false,OFFSETS,1,1));
        assertEquals(4294967196L,AmountStepper.step(4294967296L,false,false,OFFSETS,2,1));
    }
    @Test void modifiersClampToConfiguredOffsets(){
        assertEquals(1001,AmountStepper.step(1,true,false,OFFSETS,99,1));
        assertEquals(2,AmountStepper.step(1,true,false,OFFSETS,-1,1));
    }
    @Test void saturatesAtLongBoundary(){
        assertEquals(Long.MAX_VALUE,AmountStepper.step(Long.MAX_VALUE-5,true,false,OFFSETS,3,1));
        assertEquals(1,AmountStepper.step(4,false,false,List.of(Long.toString(Long.MAX_VALUE)),0,1));
    }
    @Test void fixedPresetsAreOrderedAndSkipEqualValues(){
        var presets=List.of("1000","1","100","100");
        assertEquals(1000,AmountStepper.step(100,true,true,presets,0,1));
        assertEquals(100,AmountStepper.step(500,false,true,presets,0,1));
        assertEquals(1000,AmountStepper.step(1000,true,true,presets,0,1));
    }
    @Test void emptyConfigurationStillAllowsSingleSteps(){
        assertEquals(5,AmountStepper.step(4,true,false,List.of(),3,1));
    }
    @Test void individualLimitCanReturnToAutomaticZero(){
        assertEquals(0,AmountStepper.step(1,false,false,OFFSETS,0,0));
        assertEquals(1,AmountStepper.step(1,false,false,OFFSETS,0,1));
    }
}
