package ca.gc.agr.ias.serde;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class Count {
    
    private int count;
    private int of;

    @JsonGetter
    public int getCount() {
        return count;
    }

    @JsonSetter
    public Count setCount(int count) {
        this.count = count;
        return this;
    }

    @JsonGetter
    public int getOf() {
        return of;
    }

    @JsonSetter
    public Count setOf(int of) {
        this.of = of;
        return this;
    }


    
}
