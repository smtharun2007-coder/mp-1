package com.disasterrelief.model;

/**
 * Section D relief needs breakdown within a single disaster report.
 */
public class ReportNeeds {
    private RescueNeed rescue;
    private ShelterNeed shelter;
    private MedicineNeed medicine;
    private FoodNeed food;

    public ReportNeeds() {}

    public ReportNeeds(RescueNeed rescue) {
        this.rescue = rescue;
    }

    public RescueNeed getRescue() {
        return rescue;
    }

    public void setRescue(RescueNeed rescue) {
        this.rescue = rescue;
    }

    public ShelterNeed getShelter() {
        return shelter;
    }

    public void setShelter(ShelterNeed shelter) {
        this.shelter = shelter;
    }

    public MedicineNeed getMedicine() {
        return medicine;
    }

    public void setMedicine(MedicineNeed medicine) {
        this.medicine = medicine;
    }

    public FoodNeed getFood() {
        return food;
    }

    public void setFood(FoodNeed food) {
        this.food = food;
    }

    // --- Sub-need structures ---

    public static class RescueNeed {
        private int people;

        public RescueNeed() {}
        public RescueNeed(int people) { this.people = people; }
        public int getPeople() { return people; }
        public void setPeople(int people) { this.people = people; }
    }

    public static class ShelterNeed {
        private int people;

        public ShelterNeed() {}
        public ShelterNeed(int people) { this.people = people; }
        public int getPeople() { return people; }
        public void setPeople(int people) { this.people = people; }
    }

    public static class MedicineNeed {
        private int people;
        private int kitsSuggested;
        private int kitsRequested;
        private String editReason;

        public MedicineNeed() {}
        public MedicineNeed(int people, int kitsSuggested, int kitsRequested, String editReason) {
            this.people = people;
            this.kitsSuggested = kitsSuggested;
            this.kitsRequested = kitsRequested;
            this.editReason = editReason;
        }

        public int getPeople() { return people; }
        public void setPeople(int people) { this.people = people; }
        public int getKitsSuggested() { return kitsSuggested; }
        public void setKitsSuggested(int kitsSuggested) { this.kitsSuggested = kitsSuggested; }
        public int getKitsRequested() { return kitsRequested; }
        public void setKitsRequested(int kitsRequested) { this.kitsRequested = kitsRequested; }
        public String getEditReason() { return editReason; }
        public void setEditReason(String editReason) { this.editReason = editReason; }
    }

    public static class FoodNeed {
        private int people;
        private int days;
        private int kitsSuggested;
        private int kitsRequested;
        private String editReason;

        public FoodNeed() {}
        public FoodNeed(int people, int days, int kitsSuggested, int kitsRequested, String editReason) {
            this.people = people;
            this.days = days;
            this.kitsSuggested = kitsSuggested;
            this.kitsRequested = kitsRequested;
            this.editReason = editReason;
        }

        public int getPeople() { return people; }
        public void setPeople(int people) { this.people = people; }
        public int getDays() { return days; }
        public void setDays(int days) { this.days = days; }
        public int getKitsSuggested() { return kitsSuggested; }
        public void setKitsSuggested(int kitsSuggested) { this.kitsSuggested = kitsSuggested; }
        public int getKitsRequested() { return kitsRequested; }
        public void setKitsRequested(int kitsRequested) { this.kitsRequested = kitsRequested; }
        public String getEditReason() { return editReason; }
        public void setEditReason(String editReason) { this.editReason = editReason; }
    }
}
