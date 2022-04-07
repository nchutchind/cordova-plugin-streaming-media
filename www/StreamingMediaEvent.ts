enum StreamingEventType {
    PLAY,
    PAUSE,
    SEEK
}

interface StreamingMediaEvent {
    eventType: StreamingEventType
}

interface SeekEvent extends StreamingMediaEvent {
    msec: number
}