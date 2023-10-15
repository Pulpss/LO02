package karmaka.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import karmaka.view.Router;

public class MainController implements Initializable {
    @FXML
    void handlePlayButton() throws IOException {
        Router.getInstance().setScene("createGame");
    }

    @FXML
    public void initialize(URL arg0, ResourceBundle arg1) {
    }
}