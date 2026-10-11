package com.disasterrelief.model;

/**
 * START mass-casualty and affected population counts for Section B of the single report form.
 */
public class ReportCounts {
    private int total;
    private int red;
    private int yellow;
    private int green;
    private int black;
    private int trapped;
    private int vulnerable;

    public ReportCounts() {}

    public ReportCounts(int total, int red, int yellow, int green, int black, int trapped, int vulnerable) {
        this.total = total;
        this.red = red;
        this.yellow = yellow;
        this.green = green;
        this.black = black;
        this.trapped = trapped;
        this.vulnerable = vulnerable;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getYellow() {
        return yellow;
    }

    public void setYellow(int yellow) {
        this.yellow = yellow;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlack() {
        return black;
    }

    public void setBlack(int black) {
        this.black = black;
    }

    public int getTrapped() {
        return trapped;
    }

    public void setTrapped(int trapped) {
        this.trapped = trapped;
    }

    public int getVulnerable() {
        return vulnerable;
    }

    public void setVulnerable(int vulnerable) {
        this.vulnerable = vulnerable;
    }
}
