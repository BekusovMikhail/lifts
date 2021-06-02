import javafx.animation.*;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.security.Key;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Application {
    private static Object Random;

    final int n = 10;
    final int m = 3;
    final int k = 6;
    final int max_people = k;
    final int people_freq = 2;

    ArrayList<Rectangle> rectangles = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        for (int a = 0; a < m; a++) {
            rectangles.add(new Rectangle(a * 100, n * 70, 70, 70));
            rectangles.get(a).setFill(Color.BLUE);
        }

        stage.setTitle("Lifts");

        Group rectanGroup = new Group();

        for (int a = 0; a < m; a++) {
            rectanGroup.getChildren().add(rectangles.get(a));
        }

        Scene scene1 = new Scene(rectanGroup, this.m * 100, this.n * 80);
        stage.setScene(scene1);

        stage.setWidth(m * 100);
        stage.setHeight(n * 80);

        stage.show();

        Timeline timeline = new Timeline();

        new AnimationTimer() {

            int counter = -1;

            ArrayList<Lift> lifts = new ArrayList<Lift>();
            ArrayList<Request> requests = new ArrayList<>();
            ArrayList<Exchanger<Boolean>> exc_req = new ArrayList<>();
            ArrayList<Exchanger<AtomicInteger>> exc_floor = new ArrayList<>();
            ArrayList<Exchanger<AtomicBoolean>> exc_dir = new ArrayList<>();

            public void settings() {
                for (int i = 0; i < m; i++) {
                    exc_req.add(new Exchanger<Boolean>());
                    exc_floor.add(new Exchanger<AtomicInteger>());
                    exc_dir.add(new Exchanger<AtomicBoolean>());
                }

                for (int i = 0; i < m; i++) {
                    lifts.add(new Lift(max_people, n, exc_req.get(i), exc_floor.get(i), exc_dir.get(i)));
                }

                for (Lift lift : lifts) {
                    lift.start();
                }
            }

            ArrayList<Integer> locations = new ArrayList<>();

            public void setLocations() {
                for (int h = 0; h < m; h++) {
                    locations.add(0);
                }
            }

            @Override
            public void handle(long l) {
                if (this.counter == -1) {
                    settings();
                    setLocations();
                }

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
                        try {
                            exc_floor.get(j).exchange(new AtomicInteger(-1));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            exc_dir.get(j).exchange(new AtomicBoolean(false));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            exc_req.get(j).exchange(Boolean.FALSE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
//                System.out.println(requests.size());
                    for (int request = 0; request < requests.size(); request++) {
                        for (int i = 0; i < lifts.size(); i++) {
                            try {
                                exc_floor.get(i).exchange(requests.get(request).floor);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            try {
                                exc_dir.get(i).exchange(requests.get(request).direction);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            boolean tmp = false;
                            try {
                                tmp = exc_req.get(i).exchange(Boolean.FALSE);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (tmp) {
                                requests.remove(request);
                                for (int j = i + 1; j < lifts.size(); j++) {
                                    try {
                                        exc_floor.get(j).exchange(new AtomicInteger(-1));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        exc_dir.get(j).exchange(new AtomicBoolean(false));
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        tmp = exc_req.get(j).exchange(Boolean.FALSE);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

//                ArrayList<TranslateTransition> translateTransitions = new ArrayList<>();
//                TranslateTransition tt = new TranslateTransition();
//                rectangles.get(0).setY(0);
//                tt.setFromY(n*70 - locations.get(0)*75);
//                tt.setNode(rectangles.get(0));
//                System.out.println(n*70 - locations.get(0)*75);
//                System.out.println((locations.get(0)-lifts.get(0).getLocation())*75);
//                tt.setByY(10);
//                tt.setToY((locations.get(0)-lifts.get(0).getLocation())*75);
//                tt.setDuration(Duration.millis(5000));
//                tt.play();

                for (int a = 0; a < m; a++) {
//                    final Node node = rectangles.get(a);
//                    node.setTranslateY(-lifts.get(a).getLocation() * 75);
                    rectangles.get(a).setTranslateY(n * 70 - locations.get(a) * 75);
                    KeyValue kv = new KeyValue(rectangles.get(a).yProperty(), (locations.get(a) - lifts.get(a).getLocation()) * 75);
                    KeyFrame kf = new KeyFrame(Duration.millis(1000), kv);

                    timeline.getKeyFrames().add(kf);
                }

                timeline.setCycleCount(1);
                timeline.setRate(1000);
                System.out.println(timeline.getStatus());
                timeline.play();
//                while (true) {
//                    System.out.println(timeline.getStatus());
//                    if (timeline.getStatus() == Animation.Status.STOPPED) {
//                        System.out.println(timeline.getStatus());
//                        break;
//                    }
//                }
                System.out.println(timeline.getStatus());

//                PauseTransition p = new PauseTransition(Duration.millis(1000));
//                p.setOnFinished(e->timeline.play());

                for (int u = 0; u < m; u++) {
                    locations.set(u, lifts.get(u).getLocation());
                }
                System.out.println(timeline.getStatus());
            }
        }.start();

    }
}




