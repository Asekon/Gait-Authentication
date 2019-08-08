package oya.omarbach;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by Omar on 5/14/2018.
 */
@Entity
public class Gait {
    @NonNull
    @PrimaryKey private String name;
    private String Template;
    private double distance;
    public Gait() {
    }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTemplate() { return Template; }
    public void setTemplate(String Template) { this.Template = Template; }




}
