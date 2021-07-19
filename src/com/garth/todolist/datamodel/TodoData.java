package com.garth.todolist.datamodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;

public class TodoData {

    private static TodoData instance = new TodoData();
    private static String fileName = "TodoListItems.txt";

    private ObservableList<TodoItem> todoItems;
    private DateTimeFormatter formatter;

    public static TodoData getInstance() {
        return instance;
    }

    // prevent instantiation
    private TodoData() {
        formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    }

    // temporary, only need it when we run the application and then close it to save the hard coded items
//    public void setTodoItems(List<TodoItem> todoItems) {
//        this.todoItems = todoItems;
//    }

    // method to load to do items from the file
    public void loadTodoItems() throws IOException {

        // observableArrayList because setAll function used in the Controller class needs an
        // array in a format that uses the ObservableList
        // we do this also for performance reasons
        // we always use FXCollections
        todoItems = FXCollections.observableArrayList();

        Path path = Paths.get(fileName);
        BufferedReader br = Files.newBufferedReader(path);

        String input; // contains data for each line

        try {
            while ((input = br.readLine()) != null) {
                // see storeTodoItems method why regex is "\t"
                String[] itemPieces = input.split("\t");

                String shortDescription = itemPieces[0];
                String details = itemPieces[1];
                String dateString = itemPieces[2];

                // format date
                LocalDate date = LocalDate.parse(dateString, formatter);

                TodoItem todoItem = new TodoItem(shortDescription, details, date);
                todoItems.add(todoItem);
            }
        }
        finally {
            if (br != null) {
                br.close();
            }
        }

    }

    public void addTodoItem(TodoItem item) {
        todoItems.add(item);
    }

    // method that saves the data
    public void storeTodoItems() throws IOException {
        Path path = Paths.get(fileName);
        BufferedWriter bw = Files.newBufferedWriter(path);

        try {
            // build iterator
            Iterator<TodoItem> iter = todoItems.iterator();

            while (iter.hasNext()) {
                TodoItem item = iter.next();
                bw.write(String.format("%s\t%s\t%s", item.getShortDescription(),
                        item.getDetails(), item.getDeadline().format(formatter)));
                // adds new line to the text file
                bw.newLine();
            }
        }
        finally {
            if (bw != null)
                bw.close();
        }
    }

    // return to-do list items
    public ObservableList<TodoItem> getTodoItems() {
        return todoItems;
    }

    public void deleteTodoItem(TodoItem item) {
        todoItems.remove(item);
    }
}






