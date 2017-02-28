package ru.blogic.fn.utils.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.blogic.fn.runner.Runner;

public class FnCmUtilsUiApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/FnCmUtilsApplication.fxml"));
        Parent root = loader.load();
        Controller controller=loader.getController();
        primaryStage.setTitle("FnCmUtils v."+ Runner.getVersion());
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        controller.initAfterStage();
        Pane pane;

    }


    public static void main(String[] args) {
        launch(args);
    }
}
