import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Application {
    private static Object Random;

    public static void main(String[] args) throws InterruptedException {

        launch(args);

        int n = 10;
        int m = 3;
        int k = 6;
        int max_people = k;
        int people_freq = 2;

        ArrayList<Lift> lifts = new ArrayList<Lift>();
        ArrayList<Request> requests = new ArrayList<>();
        ArrayList<Exchanger<Boolean>> exc_req = new ArrayList<>();
        ArrayList<Exchanger<AtomicInteger>> exc_floor = new ArrayList<>();
        ArrayList<Exchanger<AtomicBoolean>> exc_dir = new ArrayList<>();

        for (int i = 0; i < m; i++) {
            exc_req.add(new Exchanger<Boolean>());
            exc_floor.add(new Exchanger<AtomicInteger>());
            exc_dir.add(new Exchanger<AtomicBoolean>());
        }

        for (int i = 0; i < m; i++) {
            lifts.add(new Lift(max_people, n, exc_req.get(i), exc_floor.get(i), exc_dir.get(i)));
        }


        int counter = -1;

        for (Lift lift : lifts) {
            lift.start();
        }

        while (true) {
            Random rand = new Random();
            counter += 1;
            if (counter == people_freq) {
                for (int f = 0; f < 3; f++) {
                    AtomicInteger new_floor = new AtomicInteger();
                    new_floor.set(rand.nextInt(n));
                    AtomicBoolean direction = new AtomicBoolean();
                    if (new_floor.get() == n - 1) {
                        direction.set(false);
                    } else if (new_floor.get() == 0) {
                        direction.set(true);
                    } else {
                        direction.set(rand.nextBoolean());
                    }
                    requests.add(new Request(new_floor, direction));

                }
                counter = 0;
            }
            if (requests.isEmpty()) {
                for (int j = 0; j < lifts.size(); j++) {
                    exc_floor.get(j).exchange(new AtomicInteger(-1));
                    exc_dir.get(j).exchange(new AtomicBoolean(false));
                    exc_req.get(j).exchange(Boolean.FALSE);
                }
            } else {
//                System.out.println(requests.size());
                for (int request = 0; request < requests.size(); request++) {
                    for (int i = 0; i < lifts.size(); i++) {
                        exc_floor.get(i).exchange(requests.get(request).floor);
                        exc_dir.get(i).exchange(requests.get(request).direction);
                        boolean tmp = exc_req.get(i).exchange(Boolean.FALSE);
                        if (tmp) {
                            requests.remove(request);
                            for (int j = i + 1; j < lifts.size(); j++) {
                                exc_floor.get(j).exchange(new AtomicInteger(-1));
                                exc_dir.get(j).exchange(new AtomicBoolean(false));
                                tmp = exc_req.get(j).exchange(Boolean.FALSE);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        Text text = new Text("Hello from JavaFX!");
        text.setLayoutY(80);    // установка положения надписи по оси Y
        text.setLayoutX(100);   // установка положения надписи по оси X

        Group group = new Group(text);

        Scene scene = new Scene(group);
        stage.setScene(scene);
        stage.setTitle("First Application");
        stage.setWidth(300);
        for (int hgt= 200; hgt<700;hgt+=25){
            stage.setHeight(hgt);
            stage.show();
        }

//        Platform.exit();

    }
}




