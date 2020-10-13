package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class utils {

    /**
     * Convert a value object (something implementing 
     * org.sosy_lab.cpachecker.cpa.value.type.Value) into it's concrete
     * implmenentation.
     */
    public static Value toValue(Object value){
        if (value instanceof Boolean) {
            return BooleanValue.valueOf((Boolean) value);
        } else if (value instanceof Integer) {
            return new NumericValue((Integer) value);
        } else if (value instanceof Character) {
            return new NumericValue((Integer) value);
        } else if (value instanceof Float) {
            return new NumericValue((Float) value);
        } else if (value instanceof Double) {
            return new NumericValue((Double) value);
        } else if (value instanceof BigInteger) {
            BigInteger v = (BigInteger) value;
            return new NumericValue(v);
        } else {
            throw new IllegalArgumentException(
                    String.format(
                            "Did not recognize value for loadedValues Map: %s.",
                            value.getClass()));
        }
    }
}