package net.velor.rdc_utils.subclasses;

public class WorkingPerson {
    public String name;
    public String role;
    public String shift_type;
    public int role_color;
    public String scheduleColor;
    public int scheduleColorParsed;
    public int rowId;

    public WorkingPerson(String value, String role, int rowId) {
        name = value;
        this.role = role;
        this.rowId = rowId;
    }

    public WorkingPerson() {}
}
