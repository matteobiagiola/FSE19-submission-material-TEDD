package org.mb.tedd.utils;

import java.util.List;

public class ExecutionTime {

    private double time;
    private String timeUnit;

    public ExecutionTime computeExecutionTime(List<Long> executionTimes){
        long executionTime = executionTimes.stream()
                .mapToLong(Long::longValue).sum();

        this.compute(executionTime);

        return this;
    }

    public ExecutionTime computeExecutionTime(Long executionTime){
        this.compute(executionTime);

        return this;
    }

    private void compute(long executionTime){
        double total = (double) executionTime / (double) 1000;
        double scale = 1.0;
        this.timeUnit = "s";

        if(total > 60.0){
            if(total > 3600){
                scale = 3600.0;
                this.timeUnit = "h";
            }
            else {
                scale = 60.0;
                this.timeUnit = "min";
            }
        }

        this.time = total/scale;
    }

    @Override
    public String toString(){
        return this.time + " " + this.timeUnit;
    }
}
