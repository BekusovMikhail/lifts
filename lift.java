import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Lift extends Thread {
    boolean direction = true;
    int location = 0;
    int max_people;
    int num_of_floors;
    ArrayList<Integer> floors_for_loading = new ArrayList<Integer>();
    ArrayList<Integer> floors_for_unloading = new ArrayList<Integer>();
    AtomicInteger tmp_floor = new AtomicInteger();
    AtomicBoolean tmp_direction = new AtomicBoolean();
    Exchanger<Boolean> exc_req;
    Exchanger<AtomicInteger> exc_floor;
    Exchanger<AtomicBoolean> exc_dir;

    Random rand = new Random();

    Lift(int max_people, int num_of_floors, Exchanger<Boolean> exc_req,
         Exchanger<AtomicInteger> exc_floor, Exchanger<AtomicBoolean> exc_dir) {
        this.max_people = max_people;
        this.num_of_floors = num_of_floors;
        this.exc_req = exc_req;
        this.exc_floor = exc_floor;
        this.exc_dir = exc_dir;
    }

    public boolean request(AtomicInteger floor, AtomicBoolean direction) {
        if (floor.get() == -1) {
            return false;
        }
        if (this.floors_for_unloading.isEmpty()
                && this.floors_for_loading.isEmpty()) {
            this.direction = (this.location < floor.get());
            this.floors_for_loading.add(floor.get());
            return true;
        }
        if (this.floors_for_unloading.size() + this.floors_for_loading.size() < this.max_people
                && this.direction == direction.get()
                && this.direction == (this.location < floor.get())) {
            this.floors_for_loading.add(floor.get());
            return true;
        }
        if (this.floors_for_unloading.size() + this.floors_for_loading.size() < this.max_people
                && this.direction != direction.get()
                && this.direction == (this.location < floor.get())
                && (floor.get() == num_of_floors || floor.get() == 0)) {
            this.floors_for_loading.add(floor.get());
            return true;
        }
        return false;
    }

    public void move() {
        if (this.direction && this.location == this.num_of_floors - 1) {
            this.direction = false;
        }
        if (!this.direction && this.location == 0) {
            this.direction = true;
        }
        if (this.floors_for_loading.isEmpty() && this.floors_for_unloading.isEmpty()) {
        } else if (this.direction) {
            this.location += 1;
        } else {
            this.location -= 1;
        }
        System.out.println(
                this.getName() + " Floor:" +
                        this.location + " Request_Num:" +
                        this.floors_for_loading.size() + " People_Inside:" +
                        this.floors_for_unloading.size()
        );
    }

    public void loading() {
        if (this.floors_for_loading.contains(this.location)) {
            if (this.direction && location != this.num_of_floors - 1 ||
                    this.location == 0) {
                int tmp = this.location + 1 + this.rand.nextInt(
                        this.num_of_floors * this.num_of_floors) % (this.num_of_floors - this.location - 1);
                this.floors_for_unloading.add(tmp);
            } else {
                int tmp = this.location - 1 - this.rand.nextInt(
                        this.num_of_floors * this.num_of_floors) % this.location;
                this.floors_for_unloading.add(tmp);
            }

            this.floors_for_loading.remove((Integer) this.location);
        }
    }

    public void unloading() {
        if (this.floors_for_unloading.contains(this.location)) {
            this.floors_for_unloading.remove((Integer) this.location);
        }
    }

    public int getLocation() {
        return this.location;
    }

    public int getNumOfPeople() {
        return floors_for_unloading.size();
    }


    public void run() {
        while (true) {

            try {
                this.tmp_floor = this.exc_floor.exchange(new AtomicInteger());
            } catch (InterruptedException e) {
                System.out.println("Exchange error");
            }
            try {
                this.tmp_direction = this.exc_dir.exchange(new AtomicBoolean());
            } catch (InterruptedException e) {
                System.out.println("Exchange error");
            }

            boolean tmp_req = this.request(this.tmp_floor, this.tmp_direction);
            try {
                this.exc_req.exchange(tmp_req);
            } catch (InterruptedException e) {
                System.out.println("Exchange error");
            }

            this.move();
            this.unloading();
            this.loading();
        }
    }
}