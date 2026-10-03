package com.expensetracker.view;

import com.expensetracker.dao.ExpenseDAO;
import com.expensetracker.model.Expense;
import com.expensetracker.service.AICategorizer;
import com.expensetracker.util.CSVExporter;
import com.expensetracker.util.ExcelExporter;
import javafx.scene.control.Control;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Main JavaFX dashboard for the Expense Tracker application.
 *
 * Features:
 * - Expense CRUD operations
 * - AI/NLP category prediction
 * - Search and filtering
 * - Sorting by date and amount
 * - Spending statistics
 * - Pie chart visualization
 * - CSV export
 * - Excel export
 */
public class MainDashboard extends BorderPane {

    // ---------------------------------------------------------
    // Services
    // ---------------------------------------------------------

    private final ExpenseDAO expenseDAO;
    private final AICategorizer aiCategorizer;

    // ---------------------------------------------------------
    // Data
    // ---------------------------------------------------------

    private final ObservableList<Expense> masterExpenseList;
    private final FilteredList<Expense> filteredExpenseList;

    // ---------------------------------------------------------
    // UI Components
    // ---------------------------------------------------------

    private TableView<Expense> tableView;
    private PieChart pieChart;
    private BarChart<String, Number> monthlyBarChart;
    private ComboBox<String> monthFilter;

    private TextField amountField;
    private TextField descriptionField;
    private DatePicker datePicker;
    private ComboBox<String> categoryComboBox;

    private Label aiStatusBadge;

    private Button addButton;
    private Button updateButton;

    private Label totalSpendingLabel;
    private Label topCategoryLabel;
    private Label totalCountLabel;
    private Label avgExpenseLabel;

    // ---------------------------------------------------------
    // Selected expense
    // ---------------------------------------------------------

    private Expense selectedExpense;

    // ---------------------------------------------------------
    // Sorting state
    // ---------------------------------------------------------

    private boolean dateSortAsc = false;
    private boolean amountSortAsc = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MainDashboard(Stage stage) {

        expenseDAO = new ExpenseDAO();
        aiCategorizer = new AICategorizer();

        masterExpenseList =
                FXCollections.observableArrayList();

        filteredExpenseList =
                new FilteredList<>(
                        masterExpenseList,
                        expense -> true
                );

        setStyle(
                "-fx-background-color: #F8FAFC;" +
                "-fx-font-family: 'Segoe UI', Helvetica, Arial, sans-serif;"
        );

        setPadding(new Insets(20));

        setTop(
                buildHeaderAndStatsSection()
        );

        setLeft(
                buildInputFormSection(stage)
        );

        setCenter(
                buildTableAndChartSection()
        );

        refreshDataFromDatabase();
    }

    // =========================================================
    // HEADER + STATISTICS
    // =========================================================

    private VBox buildHeaderAndStatsSection() {

        VBox topBox = new VBox(15);

        topBox.setPadding(
                new Insets(0, 0, 20, 0)
        );

        HBox titleBar = new HBox(15);

        titleBar.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox titleTextGroup =
                new VBox(4);

        Text mainTitle =
                new Text(
                        "EXPENSE TRACKER"
                );

        mainTitle.setFont(
                Font.font(
                        "Segoe UI",
                        FontWeight.BOLD,
                        24
                )
        );

        mainTitle.setFill(
                Color.web("#0F172A")
        );

        Text subTitle =
                new Text(
                        "JavaFX • MVC • JDBC • NLP AI Auto-Categorization"
                );

        subTitle.setFont(
                Font.font(
                        "Segoe UI",
                        FontWeight.NORMAL,
                        13
                )
        );

        subTitle.setFill(
                Color.web("#64748B")
        );

        titleTextGroup.getChildren().addAll(
                mainTitle,
                subTitle
        );

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        titleBar.getChildren().addAll(
                titleTextGroup,
                spacer
        );

        HBox statsGrid = new HBox(15);

        statsGrid.setAlignment(
                Pos.CENTER
        );

        VBox totalCard =
                createStatCard(
                        "TOTAL SPENDING",
                        "₹0.00",
                        "#2563EB",
                        "💳"
                );

        totalSpendingLabel =
                (Label) totalCard.getChildren().get(1);

        VBox topCatCard =
                createStatCard(
                        "TOP CATEGORY",
                        "None",
                        "#7C3AED",
                        "📊"
                );

        topCategoryLabel =
                (Label) topCatCard.getChildren().get(1);

        VBox countCard =
                createStatCard(
                        "TRANSACTIONS",
                        "0 Records",
                        "#059669",
                        "📝"
                );

        totalCountLabel =
                (Label) countCard.getChildren().get(1);

        VBox avgCard =
                createStatCard(
                        "AVERAGE EXPENSE",
                        "₹0.00",
                        "#D97706",
                        "📈"
                );

        avgExpenseLabel =
                (Label) avgCard.getChildren().get(1);

        HBox.setHgrow(
                totalCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                topCatCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                countCard,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                avgCard,
                Priority.ALWAYS
        );

        statsGrid.getChildren().addAll(
                totalCard,
                topCatCard,
                countCard,
                avgCard
        );

        topBox.getChildren().addAll(
                titleBar,
                statsGrid
        );

        return topBox;
    }

    private VBox createStatCard(
            String title,
            String initialValue,
            String accentColor,
            String icon) {

        VBox card =
                new VBox(6);

        card.setPadding(
                new Insets(14, 18, 14, 18)
        );

        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        HBox headerBox =
                new HBox(8);

        headerBox.setAlignment(
                Pos.CENTER_LEFT
        );

        Text iconText =
                new Text(icon);

        iconText.setFont(
                Font.font(14)
        );

        Label titleLabel =
                new Label(title);

        titleLabel.setStyle(
                "-fx-text-fill: #64748B;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;"
        );

        headerBox.getChildren().addAll(
                iconText,
                titleLabel
        );

        Label valueLabel =
                new Label(initialValue);

        valueLabel.setStyle(
                "-fx-text-fill: " + accentColor + ";" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );

        card.getChildren().addAll(
                headerBox,
                valueLabel
        );

        return card;
    }

    // =========================================================
    // INPUT FORM
    // =========================================================

    private VBox buildInputFormSection(Stage stage) {

        VBox formBox =
                new VBox(14);

        formBox.setPrefWidth(320);

        // Keep the left form at its natural content height instead of
        // stretching it to match the full height of the dashboard.
        formBox.setMaxHeight(Region.USE_PREF_SIZE);

        formBox.setPadding(
                new Insets(18)
        );

        formBox.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 14;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        Text formTitle =
                new Text("Expense Entry");

        formTitle.setFont(
                Font.font(
                        "Segoe UI",
                        FontWeight.BOLD,
                        16
                )
        );

        formTitle.setFill(
                Color.web("#0F172A")
        );

        Label amountLabel =
                createFormLabel("Amount (₹)");

        amountField =
                createStyledTextField("0.00");

        Label descriptionLabel =
                createFormLabel("Description");

        descriptionField =
                createStyledTextField(
                        "e.g. Uber ride, Starbucks, Rent"
                );

        aiStatusBadge =
                new Label(
                        "🤖 AI: Waiting for text input..."
                );

        aiStatusBadge.setWrapText(true);

        aiStatusBadge.setStyle(
                "-fx-text-fill: #475569;" +
                "-fx-font-size: 11px;" +
                "-fx-padding: 4 8;" +
                "-fx-background-color: #F1F5F9;" +
                "-fx-background-radius: 6;"
        );

        descriptionField.textProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (newValue != null
                            && !newValue.trim().isEmpty()) {

                        AICategorizer.PredictionResult result =
                                aiCategorizer.predictCategory(
                                        newValue
                                );

                        if (result.isConfident()) {

                            categoryComboBox.setValue(
                                    result.getCategory()
                            );

                            aiStatusBadge.setText(
                                    String.format(
                                            "🤖 AI Suggested: %s (%.0f%% confidence)",
                                            result.getCategory(),
                                            result.getConfidence() * 100
                                    )
                            );

                            aiStatusBadge.setStyle(
                                    "-fx-text-fill: #047857;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-padding: 4 8;" +
                                    "-fx-background-color: #D1FAE5;" +
                                    "-fx-background-radius: 6;" +
                                    "-fx-font-weight: bold;"
                            );

                        } else {

                            aiStatusBadge.setText(
                                    "🤖 AI: Low confidence. Select category manually."
                            );

                            aiStatusBadge.setStyle(
                                    "-fx-text-fill: #B45309;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-padding: 4 8;" +
                                    "-fx-background-color: #FEF3C7;" +
                                    "-fx-background-radius: 6;"
                            );
                        }

                    } else {

                        aiStatusBadge.setText(
                                "🤖 AI: Waiting for text input..."
                        );

                        aiStatusBadge.setStyle(
                                "-fx-text-fill: #475569;" +
                                "-fx-font-size: 11px;" +
                                "-fx-padding: 4 8;" +
                                "-fx-background-color: #F1F5F9;" +
                                "-fx-background-radius: 6;"
                        );
                    }
                }
        );

        Label dateLabel =
                createFormLabel("Date");

        datePicker =
                new DatePicker(
                        LocalDate.now()
                );

        datePicker.setMaxWidth(
                Double.MAX_VALUE
        );

        datePicker.setStyle(
                "-fx-background-color: #F8FAFC;" +
                "-fx-text-fill: #0F172A;" +
                "-fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 6;"
        );

        Label categoryLabel =
                createFormLabel("Category");

        categoryComboBox =
                new ComboBox<>();

        categoryComboBox.setItems(
                FXCollections.observableArrayList(
                        aiCategorizer.getAvailableCategories()
                )
        );

        categoryComboBox.setValue(
                "Other"
        );

        categoryComboBox.setMaxWidth(
                Double.MAX_VALUE
        );

        categoryComboBox.setStyle(
                "-fx-background-color: #F8FAFC;" +
                "-fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 6;"
        );

        addButton =
                createStyledLightButton(
                        "➕ Add Expense",
                        "#EFF6FF",
                        "#1E40AF",
                        "#BFDBFE",
                        "#DBEAFE"
                );

        addButton.setOnAction(
                e -> handleAddExpense()
        );

        updateButton =
                createStyledLightButton(
                        "✏️ Update Selected",
                        "#FFFBEB",
                        "#92400E",
                        "#FDE68A",
                        "#FEF3C7"
                );

        updateButton.setDisable(true);

        updateButton.setOnAction(
                e -> handleUpdateExpense()
        );

        Button clearButton =
                createStyledLightButton(
                        "🔄 Clear Inputs",
                        "#F8FAFC",
                        "#475569",
                        "#E2E8F0",
                        "#F1F5F9"
                );

        clearButton.setOnAction(
                e -> clearFormFields()
        );

        Separator separator =
                new Separator();

        Button csvExportButton =
                createStyledLightButton(
                        "📊 Export Data to CSV",
                        "#ECFDF5",
                        "#065F46",
                        "#A7F3D0",
                        "#D1FAE5"
                );

        csvExportButton.setOnAction(
                e -> handleCSVExport(stage)
        );

        Button excelExportButton =
                createStyledLightButton(
                        "📗 Export Data to Excel",
                        "#EFF6FF",
                        "#1E40AF",
                        "#BFDBFE",
                        "#DBEAFE"
                );

        excelExportButton.setOnAction(
                e -> handleExcelExport(stage)
        );

        formBox.getChildren().addAll(
                formTitle,

                amountLabel,
                amountField,

                descriptionLabel,
                descriptionField,
                aiStatusBadge,

                dateLabel,
                datePicker,

                categoryLabel,
                categoryComboBox,

                addButton,
                updateButton,
                clearButton,

                separator,

                csvExportButton,
                excelExportButton
        );

        BorderPane.setMargin(
                formBox,
                new Insets(0, 20, 0, 0)
        );

        return formBox;
    }

    // =========================================================
    // TABLE + CHART
    // =========================================================

    private VBox buildTableAndChartSection() {

        VBox rightBox =
                new VBox(15);

        HBox filterSortBar =
                new HBox(10);

        filterSortBar.setAlignment(
                Pos.CENTER_LEFT
        );

        TextField searchField =
                createStyledTextField(
                        "🔍 Search description or category..."
                );

        searchField.setPrefWidth(220);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        applyFilters(searchField.getText())
        );

        // =====================================================
        // MONTH FILTER
        // =====================================================

        monthFilter =
                new ComboBox<>();

        monthFilter.setPrefWidth(140);

        monthFilter.setPromptText(
                "Select Month"
        );

        monthFilter.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );

        monthFilter.getItems().add(
                "All Months"
        );

        monthFilter.setValue(
                "All Months"
        );

        monthFilter.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        applyFilters(searchField.getText())
        );

        Button sortByDateButton =
                createStyledLightButton(
                        "📅 Sort Date",
                        "#F8FAFC",
                        "#334155",
                        "#E2E8F0",
                        "#F1F5F9"
                );

        sortByDateButton.setMaxWidth(
                Control.USE_COMPUTED_SIZE
        );

        sortByDateButton.setOnAction(
                e -> {

                    dateSortAsc =
                            !dateSortAsc;

                    Comparator<Expense> comparator =
                            Comparator.comparing(
                                    Expense::getDate
                            );

                    if (!dateSortAsc) {
                        comparator =
                                comparator.reversed();
                    }

                    FXCollections.sort(
                            masterExpenseList,
                            comparator
                    );

                    sortByDateButton.setText(
                            dateSortAsc
                                    ? "📅 Date ↑"
                                    : "📅 Date ↓"
                    );
                }
        );

        Button sortByAmountButton =
                createStyledLightButton(
                        "💲 Sort Amount",
                        "#F8FAFC",
                        "#334155",
                        "#E2E8F0",
                        "#F1F5F9"
                );

        sortByAmountButton.setMaxWidth(
                Control.USE_COMPUTED_SIZE
        );

        sortByAmountButton.setOnAction(
                e -> {

                    amountSortAsc =
                            !amountSortAsc;

                    Comparator<Expense> comparator =
                            Comparator.comparing(
                                    Expense::getAmount
                            );

                    if (!amountSortAsc) {
                        comparator =
                                comparator.reversed();
                    }

                    FXCollections.sort(
                            masterExpenseList,
                            comparator
                    );

                    sortByAmountButton.setText(
                            amountSortAsc
                                    ? "💲 Amount ↑"
                                    : "💲 Amount ↓"
                    );
                }
        );

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        filterSortBar.getChildren().addAll(
                searchField,
                monthFilter,
                sortByDateButton,
                sortByAmountButton,
                spacer
        );

        tableView =
                new TableView<>();

        tableView.setItems(
                filteredExpenseList
        );

        tableView.setPrefHeight(260);

        tableView.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-base: #FFFFFF;" +
                "-fx-control-inner-background: #FFFFFF;" +
                "-fx-table-cell-border-color: #E2E8F0;" +
                "-fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 8;"
        );

        /*
         * Resize policy intentionally left at JavaFX default.
         * This avoids raw/generic ResizeFeatures warnings
         * across different JavaFX versions.
         */

        TableColumn<Expense, Integer> idColumn =
                new TableColumn<>("ID");

        idColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>(
                        "id"
                )
        );

        idColumn.setPrefWidth(50);

        TableColumn<Expense, String> dateColumn =
                new TableColumn<>("Date");

        dateColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>(
                        "formattedDate"
                )
        );

        dateColumn.setPrefWidth(90);

        TableColumn<Expense, String> categoryColumn =
                new TableColumn<>("Category");

        categoryColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>(
                        "category"
                )
        );

        categoryColumn.setPrefWidth(120);

        TableColumn<Expense, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>(
                        "description"
                )
        );

        descriptionColumn.setPrefWidth(200);

        TableColumn<Expense, String> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>(
                        "formattedAmount"
                )
        );

        amountColumn.setStyle(
                "-fx-alignment: CENTER-RIGHT;" +
                "-fx-font-weight: bold;"
        );

        amountColumn.setPrefWidth(90);

        TableColumn<Expense, Void> actionColumn =
                new TableColumn<>("Action");

        actionColumn.setPrefWidth(70);

        actionColumn.setCellFactory(
                param -> new TableCell<Expense, Void>() {

                    private final Button deleteButton =
                            new Button("🗑️");

                    {
                        deleteButton.setStyle(
                                "-fx-background-color: #FEE2E2;" +
                                "-fx-text-fill: #991B1B;" +
                                "-fx-font-size: 10px;" +
                                "-fx-padding: 3 8;" +
                                "-fx-background-radius: 4;" +
                                "-fx-border-color: #FCA5A5;" +
                                "-fx-border-radius: 4;" +
                                "-fx-cursor: hand;"
                        );

                        deleteButton.setOnAction(
                                e -> {

                                    Expense expense =
                                            getTableView()
                                                    .getItems()
                                                    .get(getIndex());

                                    handleDeleteExpense(
                                            expense
                                    );
                                }
                        );
                    }

                    @Override
                    protected void updateItem(
                            Void item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(deleteButton);
                        }
                    }
                }
        );

        // Add columns individually to avoid varargs type-safety warning.
        tableView.getColumns().add(idColumn);
        tableView.getColumns().add(dateColumn);
        tableView.getColumns().add(categoryColumn);
        tableView.getColumns().add(descriptionColumn);
        tableView.getColumns().add(amountColumn);
        tableView.getColumns().add(actionColumn);

        tableView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {

                            if (newValue != null) {

                                selectedExpense =
                                        newValue;

                                amountField.setText(
                                        String.valueOf(
                                                newValue.getAmount()
                                        )
                                );

                                descriptionField.setText(
                                        newValue.getDescription()
                                );

                                datePicker.setValue(
                                        newValue.getDate()
                                );

                                categoryComboBox.setValue(
                                        newValue.getCategory()
                                );

                                updateButton.setDisable(
                                        false
                                );

                                addButton.setDisable(
                                        true
                                );
                            }
                        }
                );

        // =====================================================
        // PIE CHART
        // =====================================================

        pieChart =
                new PieChart();

        pieChart.setTitle(
                "Spending Breakdown by Category"
        );

        pieChart.setLegendVisible(
                true
        );

        pieChart.setPrefHeight(230);
        pieChart.setPrefWidth(360);
        pieChart.setMaxWidth(Double.MAX_VALUE);

        pieChart.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        // =====================================================
        // MONTHLY BAR CHART
        // =====================================================

        CategoryAxis monthAxis =
                new CategoryAxis();

        monthAxis.setLabel(
                "Month"
        );

        NumberAxis amountAxis =
                new NumberAxis();

        amountAxis.setLabel(
                "Amount (₹)"
        );

        monthlyBarChart =
                new BarChart<>(
                        monthAxis,
                        amountAxis
                );

        monthlyBarChart.setTitle(
                "Monthly Spending"
        );

        monthlyBarChart.setLegendVisible(
                false
        );

        monthlyBarChart.setPrefHeight(230);
        monthlyBarChart.setPrefWidth(360);
        monthlyBarChart.setMaxWidth(Double.MAX_VALUE);
        monthlyBarChart.setAnimated(false);

        monthlyBarChart.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 2);"
        );

        HBox chartsBox =
                new HBox(15);

        chartsBox.setAlignment(
                Pos.CENTER
        );

        HBox.setHgrow(
                pieChart,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                monthlyBarChart,
                Priority.ALWAYS
        );

        chartsBox.getChildren().addAll(
                pieChart,
                monthlyBarChart
        );

        rightBox.getChildren().addAll(
                filterSortBar,
                tableView,
                chartsBox
        );

        VBox.setVgrow(
                tableView,
                Priority.ALWAYS
        );

        return rightBox;
    }

    // =========================================================
    // DATABASE
    // =========================================================

    private void refreshDataFromDatabase() {

        List<Expense> listFromDatabase =
                expenseDAO.getAllExpenses();

        masterExpenseList.setAll(
                listFromDatabase
        );

        updateMonthFilterOptions();

        updateAnalyticsAndChart();
    }

    // =========================================================
    // ADD EXPENSE
    // =========================================================

    private void handleAddExpense() {

        try {

            double amount =
                    Double.parseDouble(
                            amountField
                                    .getText()
                                    .trim()
                    );

            String description =
                    descriptionField
                            .getText()
                            .trim();

            String category =
                    categoryComboBox.getValue();

            LocalDate date =
                    datePicker.getValue();

            if (description.isEmpty()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Input Error",
                        "Description cannot be empty."
                );

                return;
            }

            Expense newExpense =
                    new Expense(
                            amount,
                            category,
                            description,
                            date
                    );

            int generatedId =
                    expenseDAO.insert(
                            newExpense
                    );

            if (generatedId != -1) {

                masterExpenseList.add(
                        0,
                        newExpense
                );

                updateMonthFilterOptions();

                updateAnalyticsAndChart();

                clearFormFields();

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Expense recorded successfully!"
                );

            } else {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Database Error",
                        "Failed to save expense record."
                );
            }

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Input Error",
                    "Please enter a valid numeric amount."
            );

        } catch (IllegalArgumentException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Input Error",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // UPDATE EXPENSE
    // =========================================================

    private void handleUpdateExpense() {

        if (selectedExpense == null) {
            return;
        }

        try {

            double amount =
                    Double.parseDouble(
                            amountField
                                    .getText()
                                    .trim()
                    );

            String description =
                    descriptionField
                            .getText()
                            .trim();

            String category =
                    categoryComboBox.getValue();

            LocalDate date =
                    datePicker.getValue();

            if (description.isEmpty()) {

                showAlert(
                        Alert.AlertType.WARNING,
                        "Input Error",
                        "Description cannot be empty."
                );

                return;
            }

            selectedExpense.setAmount(
                    amount
            );

            selectedExpense.setDescription(
                    description
            );

            selectedExpense.setCategory(
                    category
            );

            selectedExpense.setDate(
                    date
            );

            boolean success =
                    expenseDAO.update(
                            selectedExpense
                    );

            if (success) {

                tableView.refresh();

                updateMonthFilterOptions();

                updateAnalyticsAndChart();

                clearFormFields();

                showAlert(
                        Alert.AlertType.INFORMATION,
                        "Success",
                        "Expense updated successfully!"
                );

            } else {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Update Error",
                        "Failed to update record in database."
                );
            }

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Input Error",
                    "Please enter a valid numeric amount."
            );

        } catch (IllegalArgumentException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Input Error",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // DELETE EXPENSE
    // =========================================================

    private void handleDeleteExpense(
            Expense expense) {

        if (expense == null
                || expense.getId() == null) {

            return;
        }

        Alert confirm =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Delete expense '" +
                                expense.getDescription() +
                                "'?",
                        ButtonType.YES,
                        ButtonType.NO
                );

        confirm.setHeaderText(null);

        confirm.showAndWait()
                .ifPresent(response -> {

                    if (response == ButtonType.YES) {

                        boolean deleted =
                                expenseDAO.delete(
                                        expense.getId()
                                );

                        if (deleted) {

                            masterExpenseList.remove(
                                    expense
                            );

                            updateMonthFilterOptions();

                            updateAnalyticsAndChart();

                            clearFormFields();

                        } else {

                            showAlert(
                                    Alert.AlertType.ERROR,
                                    "Error",
                                    "Failed to delete record."
                            );
                        }
                    }
                });
    }

    // =========================================================
    // CSV EXPORT
    // =========================================================

    private void handleCSVExport(
            Stage stage) {

        if (masterExpenseList.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Export Warning",
                    "No expense records available to export."
            );

            return;
        }

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Save Expenses CSV File"
        );

        fileChooser.setInitialFileName(
                "Expense_Report_" +
                        LocalDate.now() +
                        ".csv"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "CSV Files (*.csv)",
                        "*.csv"
                )
        );

        File file =
                fileChooser.showSaveDialog(
                        stage
                );

        if (file != null) {

            try {

                boolean success =
                        CSVExporter.exportToCSV(
                                file,
                                masterExpenseList
                        );

                if (success) {

                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "CSV Export Successful",
                            "Report successfully exported to:\n" +
                                    file.getAbsolutePath()
                    );
                }

            } catch (IOException e) {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Export Failed",
                        "Error writing CSV file: " +
                                e.getMessage()
                );
            }
        }
    }

    // =========================================================
    // EXCEL EXPORT
    // =========================================================

    private void handleExcelExport(
            Stage stage) {

        if (masterExpenseList.isEmpty()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Export Warning",
                    "No expense records available to export."
            );

            return;
        }

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Save Expenses Excel File"
        );

        fileChooser.setInitialFileName(
                "Expense_Report_" +
                        LocalDate.now() +
                        ".xlsx"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Excel Files (*.xlsx)",
                        "*.xlsx"
                )
        );

        File file =
                fileChooser.showSaveDialog(
                        stage
                );

        if (file != null) {

            try {

                boolean success =
                        ExcelExporter.exportToExcel(
                                file,
                                masterExpenseList
                        );

                if (success) {

                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "Excel Export Successful",
                            "Report successfully exported to:\n" +
                                    file.getAbsolutePath()
                    );
                }

            } catch (IOException e) {

                showAlert(
                        Alert.AlertType.ERROR,
                        "Export Failed",
                        "Error writing Excel file: " +
                                e.getMessage()
                );
            }
        }
    }

    // =========================================================
    // ANALYTICS
    // =========================================================

    private void updateAnalyticsAndChart() {

        double totalSpending = 0.0;

        int count =
                filteredExpenseList.size();

        Map<String, Double> categoryMap =
                new HashMap<>();

        // Monthly spending data
        Map<YearMonth, Double> monthlyMap =
                new TreeMap<>();

        for (Expense expense :
                filteredExpenseList) {

            totalSpending +=
                    expense.getAmount();

            categoryMap.put(
                    expense.getCategory(),
                    categoryMap.getOrDefault(
                            expense.getCategory(),
                            0.0
                    ) + expense.getAmount()
            );

            YearMonth month =
                    YearMonth.from(
                            expense.getDate()
                    );

            monthlyMap.put(
                    month,
                    monthlyMap.getOrDefault(
                            month,
                            0.0
                    ) + expense.getAmount()
            );
        }

        totalSpendingLabel.setText(
                String.format(
                        "₹%.2f",
                        totalSpending
                )
        );

        totalCountLabel.setText(
                count + " Records"
        );

        avgExpenseLabel.setText(
                count > 0
                        ? String.format(
                                "₹%.2f",
                                totalSpending / count
                        )
                        : "₹0.00"
        );

        String topCategory = "None";

        double maxCategoryTotal = 0.0;

        for (Map.Entry<String, Double> entry :
                categoryMap.entrySet()) {

            if (entry.getValue()
                    > maxCategoryTotal) {

                maxCategoryTotal =
                        entry.getValue();

                topCategory =
                        entry.getKey();
            }
        }

        topCategoryLabel.setText(
                topCategory
        );

        ObservableList<PieChart.Data> pieData =
                FXCollections.observableArrayList();

        for (Map.Entry<String, Double> entry :
                categoryMap.entrySet()) {

            pieData.add(
                    new PieChart.Data(
                            entry.getKey() +
                                    " (₹" +
                                    String.format(
                                            "%.0f",
                                            entry.getValue()
                                    ) +
                                    ")",
                            entry.getValue()
                    )
            );
        }

        pieChart.setData(
                pieData
        );

        // =====================================================
        // MONTHLY BAR CHART
        // =====================================================

        if (monthlyBarChart != null) {

            XYChart.Series<String, Number> series =
                    new XYChart.Series<>();

            series.setName(
                    "Monthly Spending"
            );

            DateTimeFormatter monthFormatter =
                    DateTimeFormatter.ofPattern(
                            "MMM yyyy"
                    );

            // Show only the latest 6 months with data.
            int startIndex =
                    Math.max(
                            0,
                            monthlyMap.size() - 6
                    );

            int currentIndex = 0;

            for (Map.Entry<YearMonth, Double> entry :
                    monthlyMap.entrySet()) {

                if (currentIndex >= startIndex) {

                    series.getData().add(
                            new XYChart.Data<>(
                                    entry.getKey()
                                            .format(monthFormatter),
                                    entry.getValue()
                            )
                    );
                }

                currentIndex++;
            }

            monthlyBarChart.getData().clear();

            monthlyBarChart.getData().add(
                    series
            );
        }
    }

    // =========================================================
    // SEARCH + MONTH FILTER
    // =========================================================

    private void applyFilters(String searchText) {

        String search =
                searchText == null
                        ? ""
                        : searchText.trim().toLowerCase();

        String selectedMonth =
                monthFilter == null
                        ? "All Months"
                        : monthFilter.getValue();

        filteredExpenseList.setPredicate(
                expense -> {

                    boolean matchesSearch = true;

                    if (!search.isEmpty()) {

                        matchesSearch =
                                expense.getDescription()
                                        .toLowerCase()
                                        .contains(search)
                                ||
                                expense.getCategory()
                                        .toLowerCase()
                                        .contains(search);
                    }

                    boolean matchesMonth = true;

                    if (selectedMonth != null
                            && !selectedMonth.equals("All Months")) {

                        String expenseMonth =
                                expense.getDate()
                                        .format(
                                                DateTimeFormatter.ofPattern(
                                                        "MMM yyyy"
                                                )
                                        );

                        matchesMonth =
                                expenseMonth.equals(
                                        selectedMonth
                                );
                    }

                    return matchesSearch
                            && matchesMonth;
                }
        );

        updateAnalyticsAndChart();
    }

    // =========================================================
    // UPDATE MONTH FILTER OPTIONS
    // =========================================================

    private void updateMonthFilterOptions() {

        if (monthFilter == null) {
            return;
        }

        String previousSelection =
                monthFilter.getValue();

        monthFilter.getItems().clear();

        monthFilter.getItems().add(
                "All Months"
        );

        TreeMap<YearMonth, Boolean> months =
                new TreeMap<>();

        for (Expense expense :
                masterExpenseList) {

            YearMonth month =
                    YearMonth.from(
                            expense.getDate()
                    );

            months.put(
                    month,
                    true
            );
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "MMM yyyy"
                );

        for (YearMonth month :
                months.keySet()) {

            monthFilter.getItems().add(
                    month.format(formatter)
            );
        }

        if (previousSelection != null
                && monthFilter.getItems()
                        .contains(previousSelection)) {

            monthFilter.setValue(
                    previousSelection
            );

        } else {

            monthFilter.setValue(
                    "All Months"
            );
        }
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearFormFields() {

        selectedExpense = null;

        amountField.setText(
                "0.00"
        );

        descriptionField.clear();

        datePicker.setValue(
                LocalDate.now()
        );

        categoryComboBox.setValue(
                "Other"
        );

        addButton.setDisable(
                false
        );

        updateButton.setDisable(
                true
        );

        tableView
                .getSelectionModel()
                .clearSelection();
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private Label createFormLabel(
            String text) {

        Label label =
                new Label(text);

        label.setStyle(
                "-fx-text-fill: #475569;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;"
        );

        return label;
    }

    private TextField createStyledTextField(
            String prompt) {

        TextField textField =
                new TextField();

        textField.setPromptText(
                prompt
        );

        textField.setStyle(
                "-fx-background-color: #F8FAFC;" +
                "-fx-text-fill: #0F172A;" +
                "-fx-prompt-text-fill: #94A3B8;" +
                "-fx-border-color: #CBD5E1;" +
                "-fx-border-radius: 6;" +
                "-fx-padding: 8 10;"
        );

        return textField;
    }

    private Button createStyledLightButton(
            String text,
            String backgroundColor,
            String textColor,
            String borderColor,
            String hoverBackground) {

        Button button =
                new Button(text);

        button.setMaxWidth(
                Double.MAX_VALUE
        );

        String normalStyle =
                "-fx-background-color: " +
                        backgroundColor +
                        ";" +
                        "-fx-text-fill: " +
                        textColor +
                        ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 9 14;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " +
                        borderColor +
                        ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;";

        String hoverStyle =
                "-fx-background-color: " +
                        hoverBackground +
                        ";" +
                        "-fx-text-fill: " +
                        textColor +
                        ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 9 14;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " +
                        borderColor +
                        ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-cursor: hand;";

        button.setStyle(
                normalStyle
        );

        button.setOnMouseEntered(
                event ->
                        button.setStyle(
                                hoverStyle
                        )
        );

        button.setOnMouseExited(
                event ->
                        button.setStyle(
                                normalStyle
                        )
        );

        return button;
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String content) {

        Alert alert =
                new Alert(type);

        alert.setTitle(
                title
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                content
        );

        alert.showAndWait();
    }
}