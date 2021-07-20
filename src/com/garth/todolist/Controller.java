package com.garth.todolist;

import com.garth.todolist.datamodel.TodoData;
import com.garth.todolist.datamodel.TodoItem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public class Controller {

//    private List<TodoItem> todoItems;

    @FXML
    private ListView<TodoItem> todoListView;
    @FXML
    private TextArea itemDetailsTextArea;
    @FXML
    private Label deadlineLabel;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ContextMenu listContextMenu;
    @FXML
    private ToggleButton filToggleButton;

    private FilteredList<TodoItem> filteredList;

    private Predicate<TodoItem> wantAllItems;
    private Predicate<TodoItem> wantTodayItems;

    @FXML
    public void initialize() {
        listContextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                deleteItem(item);
            }
        });
        // add deleteMenuItem to listContextMenu
        listContextMenu.getItems().addAll(deleteMenuItem);

        todoListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TodoItem>() {
            @Override
            public void changed(ObservableValue<? extends TodoItem> observable, TodoItem oldValue, TodoItem newValue) {
                if (newValue != null) {
                    TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                    itemDetailsTextArea.setText(item.getDetails());
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                    deadlineLabel.setText(df.format(item.getDeadline()));

                }
            }
        });

        wantAllItems = new Predicate<TodoItem>() {
            @Override
            public boolean test(TodoItem item) {
                return true;
            }
        };

        wantTodayItems = new Predicate<TodoItem>() {
            @Override
            public boolean test(TodoItem item) {
                return (item.getDeadline().equals(LocalDate.now()));
            }
        };

        filteredList = new FilteredList<>(TodoData.getInstance().getTodoItems(), wantAllItems);

        // order the to-do tasks according to their deadline (most due - least due)
        SortedList<TodoItem> sortedList = new SortedList<TodoItem>(filteredList,
                new Comparator<TodoItem>() {
                    @Override
                    public int compare(TodoItem item, TodoItem t1) {
                        return item.getDeadline().compareTo(t1.getDeadline());
                    }
                });

        // populate ListView from FXML
        // bind list view to the observable list
        // uncommenting below will not set the todoListView in order.
//        todoListView.setItems(TodoData.getInstance().getTodoItems());

        // setItems to sortedList for sorted due dates
        todoListView.setItems(sortedList);

        // set to single selection
        todoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        // this alone will not display details and due date.
        todoListView.getSelectionModel().selectFirst();

        // start of cell factory stuff in video
        // set cell factory. Pass an anonymous class that implements the callback interface
        //
        todoListView.setCellFactory(new Callback<ListView<TodoItem>, ListCell<TodoItem>>() {
            @Override
            public ListCell<TodoItem> call(ListView<TodoItem> param) {
                ListCell<TodoItem> cell = new ListCell<>() {
                    // this method gonna run whenever the list view wants to paint a single cell
                    @Override
                    protected void updateItem(TodoItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        }
                        else {
                            setText(item.getShortDescription());
                            // any date that is equal to today's date or prior to this date should be flagged in red.
                            if (item.getDeadline().isBefore(LocalDate.now().plusDays(1))) {
                                setTextFill(Color.RED);
                            }
                            else if (item.getDeadline().equals(LocalDate.now().plusDays(1))) {
                                setTextFill(Color.ORANGE);
                            }
                        }
                    }
                };
                // associate menu in the cell factory
                // only want non empty cells to have a context menu
                cell.emptyProperty().addListener(
                        (obs, wasEmpty, isNowEmpty) -> {
                            if (isNowEmpty) {
                                cell.setContextMenu(null); // if cell hasn't got anything in it
                            }
                            else {
                                cell.setContextMenu(listContextMenu);
                            }
                        }
                );

                return cell;
            }
        });
    }

    // when user presses file then selects new, show the dialog
    @FXML
    public void showNewItemDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        // dialog should be modal, meaning user cannot use the main window when dialog is open (already in default)

        // Specifies the owner Window for this dialog, or null for a top-level,
        // unowned dialog. This must be done prior to making the dialog visible.
        dialog.initOwner(mainBorderPane.getScene().getWindow());

        dialog.setTitle("Add New Todo Item"); // TITLE TEXT (at window bar)
        // The dialog's header text is separated from the dialog content and is in the larger font
        dialog.setHeaderText("Use this dialog to create a new todo item");

        // get Controller from FXMLLoader
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("todoItemDialog.fxml"));
        // try block because it may throw null
        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());
        }
        catch (IOException e) {
            System.out.println("Couldn't load the dialog");
            e.printStackTrace();
            return;
        }

        // okay and cancel button for dialog, javafx specifies such, such as OK and CANCEL
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        // Show - non-blocking
        // Show and wait - bring up a blocking dialog

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DialogController controller = fxmlLoader.getController();
            TodoItem newItem = controller.processResults();
            // replace the todoListView with updated TodoItems from Dialog when creating new todoItem
//            todoListView.getItems().setAll(TodoData.getInstance().getTodoItems());
            todoListView.getSelectionModel().select(newItem);
        }
    }

    @FXML
    public void handleKeyPressed(KeyEvent keyEvent) {
         TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
         if (selectedItem != null) {
             if (keyEvent.getCode().equals(KeyCode.DELETE)) {
                 deleteItem(selectedItem);
             }
         }
    }

    @FXML
    public void handleClickListView() {
        // get the selected item
        // SelectionModel - class that tracks which item/s is/are selected in the control
        TodoItem item = todoListView.getSelectionModel().getSelectedItem();
        itemDetailsTextArea.setText(item.getDetails());
        deadlineLabel.setText(item.getDeadline().toString());
    }

    /**
     * Deletes an item. A confirmation dialog will appear to confirm user to delete the
     * to-do item.
     * @param item
     */
    public void deleteItem(TodoItem item) {
        // confirm deletion first
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        // title, head text, content text
        alert.setTitle("Delete Todo Item");
        alert.setHeaderText("Delete item: " + item.getShortDescription());
        Label label = new Label("Are you sure? Press OK to confirm, or cancel to back out.");
        label.setWrapText(true);
        alert.getDialogPane().setContent(label);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            TodoData.getInstance().deleteTodoItem(item);
        }
    }

    @FXML
    public void handleFilterButton() {
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if (filToggleButton.isSelected()) {
            // return items with a deadline of today only
            filteredList.setPredicate(wantTodayItems);
            // if there are no records having deadline of today
            if (filteredList.isEmpty()) {
                itemDetailsTextArea.clear();
                deadlineLabel.setText("");
            }
            // if the currently-selected item is in the filteredList, select it
            else if (filteredList.contains(selectedItem)) {
                todoListView.getSelectionModel().select(selectedItem);
            }
            // selects the first
            else {
                todoListView.getSelectionModel().selectFirst();
            }
        }
        // if button is not selected, show everything
        else {
            filteredList.setPredicate(wantAllItems );
            todoListView.getSelectionModel().select(selectedItem);
        }
    }

    @FXML
    public void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Todo App");
        Label label = new Label("Are you sure? Press OK to confirm, or cancel to back out.");
        label.setWrapText(true);
        alert.getDialogPane().setContent(label);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Platform.exit();
        }
        else {
            alert.close();
        }
    }
}











