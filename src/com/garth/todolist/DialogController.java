package com.garth.todolist;

import com.garth.todolist.datamodel.TodoData;
import com.garth.todolist.datamodel.TodoItem;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class DialogController {

    @FXML
    private TextField shortDescriptionField;
    @FXML
    private TextArea detailsArea;
    @FXML
    private DatePicker deadlinePicker;

    // Gather user's input, create a new to-do item, add to list in to-do data instance
    public TodoItem processResults() {
        String shortDescription = shortDescriptionField.getText().trim();
        String details = detailsArea.getText().trim();
        LocalDate deadlineValue = deadlinePicker.getValue();

        // get the instance and add to-do item
        TodoItem newItem = new TodoItem(shortDescription, details, deadlineValue);
        TodoData.getInstance().addTodoItem(newItem);
        return newItem;
    }
}
