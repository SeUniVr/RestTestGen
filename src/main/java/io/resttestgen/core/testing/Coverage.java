package io.resttestgen.core.testing;

import com.google.gson.JsonObject;

public abstract class Coverage {

    public abstract void updateCoverage(TestInteraction testInteraction);

    public double getCoverage(){
        return (double)this.getTested()*100/this.getToTest();
    }

    public abstract int getTested();

    public abstract int getToTest();

    public abstract JsonObject getReportAsJsonObject();

}
