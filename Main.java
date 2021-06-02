import javafx.animation.*;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Application {
    private static Object Random;

    final int n = 10;
    final int m = 3;
    final int k = 6;
    final int max_people = k;
    final int people_freq = 1;
    final int animationSpeedMs = 2000;

    ArrayList<Rectangle> rectangles = new ArrayList<>();
    ArrayList<Text> people_counters = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        for (int a = 0; a < m; a++) {
            rectangles.add(new Rectangle(a * 90 + 60, (n - 1) * 80, 70, 80));
            rectangles.get(a).setStyle("-fx-stroke: black; -fx-stroke-width: 3;");
            rectangles.get(a).setFill(Color.BLUE);
            Text tmptext = new Text(a * 90 + 29 + 60, (n - 1) * 80 + 45 + 60, "0");
            tmptext.setFill(Color.RED);
            tmptext.setStyle("-fx-font-size: 24; -fx-text-alignment: center");
            people_counters.add(tmptext);
        }

        Group rectanGroup = new Group();

        for (int a = 0; a < n; a++) {
            Text tmptext = new Text(20, (a) * 80 + 45, Integer.toString(n - a));
            tmptext.setFill(Color.GREEN);
            tmptext.setStyle("-fx-font-size: 24; -fx-text-alignment: center");
            rectanGroup.getChildren().add(tmptext);
        }


        for (int a = 0; a < m; a++) {
            Line tmpline = new Line(a * 90 + 35 + 60, 0, a * 90 + 35 + 60, n * 80);
            rectanGroup.getChildren().add(tmpline);
        }

        for (int a = 0; a < m; a++) {
            rectanGroup.getChildren().add(rectangles.get(a));
            rectanGroup.getChildren().add(people_counters.get(a));

        }

        stage.setTitle("Lifts");

        Scene scene1 = new Scene(rectanGroup, this.m * 90 + 60, this.n * 80);
        stage.setScene(scene1);

        stage.setWidth(m * 90 + 60);
        stage.setHeight(n * 80 + 40);

        stage.show();

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

            long prev_time = 0;

            @Override
            public void handle(long l) {
                if (this.counter == -1) {
                    settings();
                    setLocations();
                }
                if ((l - prev_time) < animationSpeedMs * 1000000) {
                    return;
                }
                prev_time = l;

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

                for (int a = 0; a < m; a++) {
                    TranslateTransition tt = new TranslateTransition();
                    rectangles.get(a).setY(0);
                    tt.setFromY((n - 1 - locations.get(a)) * 80);
                    tt.setNode(rectangles.get(a));
                    tt.setToY((n - 1 - lifts.get(a).getLocation()) * 80);
                    tt.setDuration(Duration.millis(animationSpeedMs));
                    tt.play();
                }

                for (int a = 0; a < m; a++) {
//                    Text tmptext = new Text(a * 90 + 29, (n - 1) * 80 + 45, "0");
                    people_counters.get(a).setText(Integer.toString(lifts.get(a).getNumOfPeople()));
                    TranslateTransition tt = new TranslateTransition();
                    people_counters.get(a).setY(0);
                    tt.setFromY((n - 1 - locations.get(a)) * 80 + 45);
                    tt.setNode(people_counters.get(a));
                    tt.setToY((n - 1 - lifts.get(a).getLocation()) * 80 + 45);
                    tt.setDuration(Duration.millis(animationSpeedMs));
                    tt.play();
                }

                for (int u = 0; u < m; u++) {
                    locations.set(u, lifts.get(u).getLocation());
                }

            }
        }.start();

    }
}




