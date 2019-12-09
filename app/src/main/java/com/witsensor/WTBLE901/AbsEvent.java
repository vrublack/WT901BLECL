package com.witsensor.WTBLE901;

public abstract class AbsEvent implements IEvent {
    protected String type;
    protected Object data;

    public AbsEvent() {
    }

    public AbsEvent(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public String getEventType() {
        return type;
    }

    @Override
    public void setEventType(String eventType) {
        this.type = eventType;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }
}
