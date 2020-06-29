package structured_java;

public class KeyboardFocusInfo {

    enum FocusLevel {
        AREA,
        ROW,
        COLUMN
    }

    private int focusedRow = 0;
    private int focusedColumn = 0;
    private FocusLevel focusLevel;
    private int focusedAreaIndex = 0;

    public KeyboardFocusInfo() {
        focusLevel = FocusLevel.AREA;
    }

    public int getFocusedRow() {
        return focusedRow;
    }

    public void setFocusedRow(int focusedRow) {
        this.focusedRow = focusedRow;
    }

    public int getFocusedColumn() {
        return focusedColumn;
    }

    public void setFocusedColumn(int focusedColumn) {
        this.focusedColumn = focusedColumn;
    }

    public FocusLevel getFocusLevel() {
        return focusLevel;
    }

    public void setFocusLevel(FocusLevel focusLevel) {
        this.focusLevel = focusLevel;
    }

    public int getFocusedAreaIndex() {
        return focusedAreaIndex;
    }

    public void setFocusedAreaIndex(int focusedAreaIndex) {
        this.focusedAreaIndex = focusedAreaIndex;
    }

    public void incrementFocusedAreaIndex() {
        focusedAreaIndex++;
    }

    public void decrementFocusedAreaIndex() {
        focusedAreaIndex--;
    }

    public void incrementRow() {
        focusedRow++;
    }

    public void decrementRow() {
        focusedRow--;
    }

    public void incrementColumn() {
        focusedColumn++;
    }

    public void decrementColumn() {
        focusedColumn--;
    }
}
