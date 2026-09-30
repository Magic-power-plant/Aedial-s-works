package com.mpp.aedialsworks.cells.menu;

import java.util.List;
import com.mpp.aedialsworks.cells.cell.CellMath;

/** Shared long-safe amount controls; the server still enforces each port's limits. */
public final class AmountStepper {
    private AmountStepper() {}

    public static long step(long current, boolean increase, boolean fixedValues,
            List<? extends String> configuredValues, int magnitude, long minimum) {
        long value=Math.max(minimum,current);
        long[] values=configuredValues.stream().mapToLong(Long::parseLong).filter(v->v>0).distinct().sorted().toArray();
        if(values.length==0)values=new long[]{1};
        if(fixedValues){
            if(increase){for(long preset:values)if(preset>value)return Math.max(minimum,preset);}
            else {for(int i=values.length-1;i>=0;i--)if(values[i]<value)return Math.max(minimum,values[i]);}
            return value;
        }
        long step=values[Math.max(0,Math.min(magnitude,values.length-1))];
        return increase?CellMath.add(value,step):Math.max(minimum,value-step);
    }
}
