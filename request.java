import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Request {
    AtomicInteger floor;
    AtomicBoolean direction;

    Request(AtomicInteger floor, AtomicBoolean direction) {
        this.floor = floor;
        this.direction = direction;
    }
}
