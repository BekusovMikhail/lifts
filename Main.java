import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Application {
    private static Object Random;

    public static void main(String[] args) throws InterruptedException {
        launch(args);
    }

    public void draw(Stage stage) {
        stage.show();
    }

    @Override
    public void start(Stage stage) throws Exception {

        Random rand = new Random();
        stage.setHeight(rand.nextInt(700));
        Text text = new Text("Hello from JavaFX!");
        text.setLayoutY(80);
        text.setLayoutX(100);

        Group group = new Group(text);

        Scene scene = new Scene(group);
        stage.setScene(scene);
        stage.setTitle("Lifts");


        Timeline timeline = new Timeline(500);

        new AnimationTimer() {

            final int n = 10;
            final int m = 3;
            final int k = 6;
            final int max_people = k;
            final int people_freq = 2;
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

            @Override
            public void handle(long l) {

                if (this.counter == -1) {
                    settings();
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
                stage.setWidth(this.m * 100);
                stage.setHeight(this.n * 100);

                ArrayList<Rectangle> rectangles = new ArrayList<>();
                Group rectanGroup = new Group();

                for (int k = 0; k < m; k++) {
                    rectangles.add(new Rectangle(k * 100, lifts.get(k).getLocation()*100, 70, 80));
                    rectangles.get(k).setFill(Color.BLUE);
                    rectanGroup.getChildren().add(rectangles.get(k));
                }



                Scene scene1 = new Scene(rectanGroup);
                stage.setScene(scene1);

                stage.show();
                timeline.play();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }.start();


//        Platform.exit();

    }
}




