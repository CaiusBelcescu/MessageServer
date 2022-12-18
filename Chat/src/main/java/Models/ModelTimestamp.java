package Models;

public class ModelTimestamp {

    private Long time;

    //private Timestamp timestamp;
    public long getTime() {
        time = System.currentTimeMillis();
        return time;
    }

    @Override
    public String toString() {
        return "" + time;
    }
}

