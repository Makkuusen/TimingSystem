package me.makkuusen.timing.system.logger;

import org.json.simple.JSONObject;

import java.util.UUID;

public class LogEntryBuilder {

    private final long date;
    private final String context;

    private String action;
    private String valueChanged;
    private String oldValue;
    private String newValue;
    private Long objectId;
    private UUID uuid;
    private Long count;

    public LogEntryBuilder(long date, String context) {
        this.date = date;
        this.context = context;
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

    public LogEntryBuilder setUUID(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public LogEntry build() {
        JSONObject body = new JSONObject();
        body.put("context", context);
        putIfPresent(body, "objectId", objectId);
        putIfPresent(body, "action", action);
        putIfPresent(body, "valueChanged", valueChanged);
        putIfPresent(body, "oldValue", oldValue);
        putIfPresent(body, "newValue", newValue);
        putIfPresent(body, "count", count);
        putIfPresent(body, "uuid", uuid == null ? null : uuid.toString());

        return new LogEntry(date, body);
    }

    private void putIfPresent(JSONObject body, String key, Object value) {
        if(value != null)
            body.put(key ,value);
    }

    public static LogEntryBuilder start(long date, String context) {
        return new LogEntryBuilder(date, context);
    }
}
