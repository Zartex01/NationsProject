package fr.nations.epoque;

import java.util.ArrayList;
import java.util.List;

public class EpoqueLevel {

    private int number;
    private String name;
    private int durationMinutes;
    private final List<EpoqueCondition> conditions = new ArrayList<>();
    private final List<EpoqueReward>    rewards    = new ArrayList<>();

    public EpoqueLevel(int number, String name, int durationMinutes) {
        this.number          = number;
        this.name            = name;
        this.durationMinutes = durationMinutes;
    }

    public long getDurationMillis() {
        return (long) durationMinutes * 60_000L;
    }

    public int getNumber()          { return number; }
    public String getName()         { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public List<EpoqueCondition> getConditions() { return conditions; }
    public List<EpoqueReward>    getRewards()    { return rewards; }

    public void setNumber(int n)            { this.number = n; }
    public void setName(String name)        { this.name = name; }
    public void setDurationMinutes(int min) { this.durationMinutes = min; }

    public void addCondition(EpoqueCondition c)  { conditions.add(c); }
    public void removeCondition(int index)       { if (index >= 0 && index < conditions.size()) conditions.remove(index); }
    public void addReward(EpoqueReward r)        { rewards.add(r); }
    public void removeReward(int index)          { if (index >= 0 && index < rewards.size()) rewards.remove(index); }
}
