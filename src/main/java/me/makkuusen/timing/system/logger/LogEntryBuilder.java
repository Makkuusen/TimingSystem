package me.makkuusen.timing.system.logger;

import org.json.simple.JSONObject;

public class LogEntryBuilder {

    private final long date;
    private final LogType logType;

    private String action;
    private String valueChanged;
    private String oldValue;
    private String newValue;
    private Long objectId;
    private Long count;

    public LogEntryBuilder(long date, LogType logType) {
        this.date = date;
        this.logType = logType;
    }

    public LogEntryBuilder setAction(String action) {
        this.action = action;
        return this;
    }

    public LogEntryBuilder setValueChanged(String valueChanged) {
        this.valueChanged = valueChanged;
        return this;
    }

    public LogEntryBuilder setOldValue(String oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    public LogEntryBuilder setNewValue(String newValue) {
        this.newValue = newValue;
        return this;
    }

    public LogEntryBuilder setObjectId(long objectId) {
        this.objectId = objectId;
        return this;
    }

    public LogEntryBuilder setCount(long count) {
        this.count = count;
        return this;
    }

    public LogEntry build() {
        JSONObject body = new JSONObject();
        body.put("type", logType.getId());
        putIfPresent(body, "objectId", objectId);
        putIfPresent(body, "action", action);
        putIfPresent(body, "valueChanged", valueChanged);
        putIfPresent(body, "oldValue", oldValue);
        putIfPresent(body, "newValue", newValue);
        putIfPresent(body, "count", count);

        return new LogEntry(date, body);
    }

    private void putIfPresent(JSONObject body, String key, Object value) {
        if(value != null)
            body.put(key ,value);
    }

    public static LogEntryBuilder start(long date, LogType logType) {
        return new LogEntryBuilder(date, logType);
    }
}
