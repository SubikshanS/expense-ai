package com.expensetracker.view;

import com.expensetracker.config.DBConnection;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseSummary;
import com.expensetracker.service.AICategorizer;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.util.CSVExporter;
import com.expensetracker.util.ExcelExporter;
import com.expensetracker.util.MoneyUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.expensetracker.model.CategoryMoMPoint;
import com.expensetracker.model.CategoryReportRow;
import com.expensetracker.model.CategorySpendingPoint;
import com.expensetracker.model.CategoryTrend;
import com.expensetracker.model.FinancialIntelligenceResult;
import com.expensetracker.model.FinancialReport;
import com.expensetracker.model.MoMComparisonResult;
import com.expensetracker.model.MonthComparison;
import com.expensetracker.model.ReportFilter;
import com.expensetracker.model.SpendingAnomaly;
import com.expensetracker.model.TimeRange;
import com.expensetracker.model.TimeSeriesPoint;
import com.expensetracker.util.CurrencyAxisFormatter;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Modern, responsive JavaFX Dashboard with sidebar navigation across:
 * - Dashboard (Executive Overview & KPI Cards)
 * - Transactions (Ledger, Filtering, and Entry Form)
 * - Analytics (Visual Distribution & Monthly Spending Trends)
 * - Reports (Export Center & Aggregation Metrics)
 */
public class MainDashboard extends BorderPane {

    public enum ViewType {
        DASHBOARD,
        TRANSACTIONS,
        ANALYTICS,
        INSIGHTS,
        REPORTS
    }

    private final Stage stage;
    private final ExpenseService expenseService;
    private final ExecutorService executorService;

    // Data collections
    private final ObservableList<Expense> masterExpenseList;
    private final FilteredList<Expense> filteredExpenseList;

    // Navigation UI
    private Button navDashboardBtn;
    private Button navTransactionsBtn;
    private Button navAnalyticsBtn;
    private Button navInsightsBtn;
    private Button navReportsBtn;
    private StackPane contentHost;

    // View containers
    private VBox dashboardView;
    private VBox transactionsView;
    private VBox analyticsView;
    private VBox insightsView;
    private VBox reportsView;

    // Persistent ScrollPane wrappers to prevent node reparenting issues
    private ScrollPane dashboardScroll;
    private ScrollPane analyticsScroll;
    private ScrollPane insightsScroll;
    private ScrollPane reportsScroll;

    // Financial Reports UI components
    private ComboBox<ReportFilter.PeriodPreset> reportPeriodPresetCombo;
    private ComboBox<String> reportCategoryFilterCombo;
    private DatePicker reportStartDatePicker;
    private DatePicker reportEndDatePicker;
    private HBox reportCustomDatesBox;
    private Label reportMetaStatusLbl;
    private Label reportKpiTotalSpendLbl;
    private Label reportKpiTxCountLbl;
    private Label reportKpiAvgSpendLbl;
    private Label reportKpiTopCatLbl;
    private Label reportKpiPeakDayLbl;
    private TableView<CategoryReportRow> reportCategoryTable;
    private TableView<Expense> reportLedgerTable;
    private FinancialReport currentGeneratedReport;

    // Financial Intelligence UI components
    private Label insightsCurrentMonthVal;
    private Label insightsCurrentMonthSub;
    private Label insightsPrevMonthVal;
    private Label insightsPrevMonthSub;
    private Label insightsChangeVal;
    private Label insightsChangeSub;
    private Label insightsVelocityVal;
    private Label insightsVelocitySub;
    private VBox growthDriversBox;
    private VBox anomaliesBox;
    private VBox summaryInsightsBox;
    private Label dashboardIntelSummaryLbl;
    private Label dashboardIntelBadge;

    // KPI Card Labels
    private Label kpiTotalSpending;
    private Label kpiTopCategory;
    private Label kpiTransactionCount;
    private Label kpiAverageExpense;

    // Form Components
    private TextField amountField;
    private TextField descriptionField;
    private DatePicker datePicker;
    private ComboBox<String> categoryComboBox;
    private Label aiBadge;
    private Button saveExpenseButton;
    private Button updateExpenseButton;
    private Button clearFormButton;

    // Table & Filters
    private TableView<Expense> mainTableView;
    private TableView<Expense> recentTableView;
    private ComboBox<String> monthFilterComboBox;
    private TextField searchField;

    // Dashboard Visual Charts
    private PieChart dashboardPieChart;
    private VBox dashboardPieEmptyState;
    private BarChart<String, Number> dashboardMonthlyBarChart;
    private VBox dashboardMonthlyEmptyState;

    // Analytics Visual Controls & Charts
    private ComboBox<TimeRange> lineChartRangeCombo;
    private Label lineChartSubtitle;
    private LineChart<String, Number> spendingLineChart;
    private VBox lineChartEmptyState;
    private Label analyticsPeriodTotalLbl;
    private Label analyticsPeriodAvgLbl;
    private Label analyticsPeriodPeakLbl;
    private Label analyticsPeriodCountLbl;

    private BarChart<String, Number> categoryBarChart;
    private Label categoryBarSubtitle;
    private VBox categoryBarEmptyState;

    private BarChart<String, Number> momBarChart;
    private Label momSubtitle;
    private CheckBox momNormalizeToggle;
    private VBox momEmptyState;
    private Label momSummaryFootnote;

    // Loading & Status
    private StackPane loadingOverlay;
    private HBox statusBanner;
    private Label statusBannerText;

    // State
    private Expense selectedExpense = null;
    private boolean sortDateAsc = false;
    private boolean sortAmountAsc = false;

    public MainDashboard(Stage stage) {
        this.stage = stage;
        this.expenseService = new ExpenseService();
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ExpenseTracker-BackgroundWorker");
            t.setDaemon(true);
            return t;
        });

        this.masterExpenseList = FXCollections.observableArrayList();
        this.filteredExpenseList = new FilteredList<>(masterExpenseList, p -> true);

        loadStylesheet();
        initializeUI();
        refreshExpensesFromDatabase();
    }

    private void loadStylesheet() {
        try {
            var css = getClass().getResource("/styles/app.css");
            if (css != null) {
                getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }

    private void initializeUI() {
        setLeft(buildSidebar());

        contentHost = new StackPane();
        contentHost.getStyleClass().add("content-area");

        // Build all views
        dashboardView = buildDashboardView();
        transactionsView = buildTransactionsView();
        analyticsView = buildAnalyticsView();
        insightsView = buildInsightsView();
        reportsView = buildReportsView();

        // Permanent ScrollPanes to prevent node reparenting issues
        dashboardScroll = createScrollPane(dashboardView);
        analyticsScroll = createScrollPane(analyticsView);
        insightsScroll = createScrollPane(insightsView);
        reportsScroll = createScrollPane(reportsView);

        // Loading overlay
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(248, 250, 252, 0.7);");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        loadingOverlay.getChildren().add(spinner);
        loadingOverlay.setVisible(false);

        // Status banner
        statusBanner = new HBox(8);
        statusBanner.setAlignment(Pos.CENTER_LEFT);
        statusBanner.getStyleClass().addAll("status-banner", "status-banner-success");
        statusBannerText = new Label();
        statusBanner.getChildren().add(statusBannerText);
        statusBanner.setVisible(false);
        statusBanner.setManaged(false);

        VBox centerContainer = new VBox(12);
        centerContainer.getChildren().addAll(statusBanner, contentHost);
        VBox.setVgrow(contentHost, Priority.ALWAYS);

        StackPane centerWrapper = new StackPane(centerContainer, loadingOverlay);
        setCenter(centerWrapper);

        switchView(ViewType.DASHBOARD);
    }

    // =========================================================================
    // Sidebar Navigation
    // =========================================================================

    private VBox buildSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().add("sidebar");

        // Brand Title
        VBox brandBox = new VBox(4);
        Label brandTitle = new Label("💰 Expense AI");
        brandTitle.getStyleClass().add("sidebar-brand-title");

        Label brandSub = new Label("Smart Financial Tracker");
        brandSub.getStyleClass().add("sidebar-brand-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSub);

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-opacity: 0.15;");

        // Navigation Buttons
        VBox navBox = new VBox(6);
        navDashboardBtn = createNavButton("📊  Dashboard", () -> switchView(ViewType.DASHBOARD));
        navTransactionsBtn = createNavButton("💳  Transactions", () -> switchView(ViewType.TRANSACTIONS));
        navAnalyticsBtn = createNavButton("📈  Analytics", () -> switchView(ViewType.ANALYTICS));
        navInsightsBtn = createNavButton("💡  Spending Insights", () -> switchView(ViewType.INSIGHTS));
        navReportsBtn = createNavButton("📑  Reports & Export", () -> switchView(ViewType.REPORTS));

        navBox.getChildren().addAll(
                navDashboardBtn,
                navTransactionsBtn,
                navAnalyticsBtn,
                navInsightsBtn,
                navReportsBtn
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Storage status badge
        VBox storageBox = new VBox(4);
        storageBox.getStyleClass().add("sidebar-status-box");
        Label storageTitle = new Label("DATABASE STORAGE");
        storageTitle.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        String dbType = DBConnection.getDatabaseType();
        Label storageType = new Label("● " + dbType);
        storageType.getStyleClass().add("sidebar-status-text");
        storageBox.getChildren().addAll(storageTitle, storageType);

        sidebar.getChildren().addAll(brandBox, sep1, navBox, spacer, storageBox);
        return sidebar;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void switchView(ViewType viewType) {
        navDashboardBtn.getStyleClass().remove("nav-button-active");
        navTransactionsBtn.getStyleClass().remove("nav-button-active");
        navAnalyticsBtn.getStyleClass().remove("nav-button-active");
        if (navInsightsBtn != null) navInsightsBtn.getStyleClass().remove("nav-button-active");
        navReportsBtn.getStyleClass().remove("nav-button-active");

        switch (viewType) {
            case DASHBOARD -> {
                navDashboardBtn.getStyleClass().add("nav-button-active");
                contentHost.getChildren().setAll(dashboardScroll);
                renderDashboardCharts();
            }
            case TRANSACTIONS -> {
                navTransactionsBtn.getStyleClass().add("nav-button-active");
                contentHost.getChildren().setAll(transactionsView);
                if (mainTableView != null) mainTableView.refresh();
            }
            case ANALYTICS -> {
                navAnalyticsBtn.getStyleClass().add("nav-button-active");
                contentHost.getChildren().setAll(analyticsScroll);
                renderSpendingLineChart();
                renderCategoryBarChart();
                renderMoMBarChart();
            }
            case INSIGHTS -> {
                if (navInsightsBtn != null) navInsightsBtn.getStyleClass().add("nav-button-active");
                contentHost.getChildren().setAll(insightsScroll);
                updateInsightsView(expenseService.getFinancialIntelligence(masterExpenseList));
            }
            case REPORTS -> {
                navReportsBtn.getStyleClass().add("nav-button-active");
                contentHost.getChildren().setAll(reportsScroll);
                if (currentGeneratedReport == null) {
                    generateAndDisplayReport();
                }
            }
        }
    }

    private ScrollPane createScrollPane(VBox content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        return scroll;
    }

    // =========================================================================
    // View 1: Dashboard (Overview & KPIs)
    // =========================================================================

    private VBox buildDashboardView() {
        VBox view = new VBox(20);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Dashboard Overview");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Key financial metrics and recent transactions at a glance.");
        sub.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(title, sub);

        // KPI Cards Row
        HBox kpiGrid = new HBox(16);
        VBox card1 = createStatCard("TOTAL SPENDING", "₹0.00", "#2563EB", "💳", "All-time spending");
        kpiTotalSpending = (Label) card1.getChildren().get(1);

        VBox card2 = createStatCard("TOP CATEGORY", "None", "#7C3AED", "🏷️", "Highest expenditure");
        kpiTopCategory = (Label) card2.getChildren().get(1);

        VBox card3 = createStatCard("TRANSACTIONS", "0 Records", "#059669", "📝", "Total recorded");
        kpiTransactionCount = (Label) card3.getChildren().get(1);

        VBox card4 = createStatCard("AVERAGE EXPENSE", "₹0.00", "#D97706", "📈", "Per transaction");
        kpiAverageExpense = (Label) card4.getChildren().get(1);

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);
        kpiGrid.getChildren().addAll(card1, card2, card3, card4);

        // Visual Analytics Overview (Pie Chart + Monthly Bar Chart)
        HBox chartsRow = new HBox(16);
        chartsRow.setFillHeight(true);

        // Card 1: Category Distribution (PieChart)
        VBox pieCard = new VBox(10);
        pieCard.getStyleClass().add("card");
        pieCard.setMaxWidth(Double.MAX_VALUE);
        pieCard.setPrefWidth(0);
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        HBox pieHeader = new HBox(8);
        pieHeader.setAlignment(Pos.CENTER_LEFT);
        pieHeader.setMinHeight(36);
        Label pieTitle = new Label("📊 Category Distribution");
        pieTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region pieSpacer = new Region();
        HBox.setHgrow(pieSpacer, Priority.ALWAYS);
        Button deepDiveBtn = new Button("View Full Analytics →");
        deepDiveBtn.getStyleClass().addAll("btn", "btn-secondary");
        deepDiveBtn.setOnAction(e -> switchView(ViewType.ANALYTICS));
        pieHeader.getChildren().addAll(pieTitle, pieSpacer, deepDiveBtn);

        dashboardPieChart = new PieChart();
        dashboardPieChart.setPrefHeight(280);
        dashboardPieChart.setMinHeight(280);
        dashboardPieChart.setLegendVisible(true);
        dashboardPieChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        dashboardPieChart.setLabelsVisible(false);
        dashboardPieChart.setStartAngle(90);
        dashboardPieChart.setClockwise(true);
        dashboardPieChart.setAnimated(false);

        dashboardPieEmptyState = createEmptyStateLabel("No expense data recorded yet.\nAdd transactions to visualize category distribution.");
        dashboardPieEmptyState.setVisible(false);
        dashboardPieEmptyState.setManaged(false);
        StackPane pieStack = new StackPane(dashboardPieChart, dashboardPieEmptyState);
        pieStack.setPrefHeight(280);
        VBox.setVgrow(pieStack, Priority.ALWAYS);

        pieCard.getChildren().addAll(pieHeader, pieStack);

        // Card 2: Monthly Spending History (BarChart)
        VBox monthlyCard = new VBox(10);
        monthlyCard.getStyleClass().add("card");
        monthlyCard.setMaxWidth(Double.MAX_VALUE);
        monthlyCard.setPrefWidth(0);
        HBox.setHgrow(monthlyCard, Priority.ALWAYS);

        HBox monthlyHeader = new HBox(8);
        monthlyHeader.setAlignment(Pos.CENTER_LEFT);
        monthlyHeader.setMinHeight(36);
        Label monthlyTitle = new Label("📈 Monthly Spending History");
        monthlyTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region monthlySpacer = new Region();
        HBox.setHgrow(monthlySpacer, Priority.ALWAYS);
        Label monthlyBadge = new Label("Calendar Trajectory");
        monthlyBadge.getStyleClass().addAll("badge", "badge-neutral");
        monthlyHeader.getChildren().addAll(monthlyTitle, monthlySpacer, monthlyBadge);

        CategoryAxis mXAxis = new CategoryAxis();
        mXAxis.setLabel("Month");
        mXAxis.setAnimated(false);

        NumberAxis mYAxis = new NumberAxis();
        mYAxis.setLabel("Amount (₹)");
        mYAxis.setTickLabelFormatter(new CurrencyAxisFormatter());
        mYAxis.setAnimated(false);

        dashboardMonthlyBarChart = new BarChart<>(mXAxis, mYAxis);
        dashboardMonthlyBarChart.setPrefHeight(280);
        dashboardMonthlyBarChart.setMinHeight(280);
        dashboardMonthlyBarChart.setLegendVisible(false);
        dashboardMonthlyBarChart.setAnimated(false);

        dashboardMonthlyEmptyState = createEmptyStateLabel("Add transactions to see monthly spending.");
        dashboardMonthlyEmptyState.setVisible(false);
        dashboardMonthlyEmptyState.setManaged(false);
        StackPane monthlyStack = new StackPane(dashboardMonthlyBarChart, dashboardMonthlyEmptyState);
        monthlyStack.setPrefHeight(280);
        VBox.setVgrow(monthlyStack, Priority.ALWAYS);

        monthlyCard.getChildren().addAll(monthlyHeader, monthlyStack);

        chartsRow.getChildren().addAll(pieCard, monthlyCard);

        // Recent Transactions Section
        VBox recentBox = new VBox(12);
        recentBox.getStyleClass().add("card");

        HBox recentHeader = new HBox(12);
        recentHeader.setAlignment(Pos.CENTER_LEFT);
        Label recentTitle = new Label("Recent Transactions");
        recentTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addExpenseBtn = new Button("➕ Add Expense");
        addExpenseBtn.getStyleClass().addAll("btn", "btn-primary");
        addExpenseBtn.setOnAction(e -> switchView(ViewType.TRANSACTIONS));

        Button viewAllBtn = new Button("View All Transactions →");
        viewAllBtn.getStyleClass().addAll("btn", "btn-secondary");
        viewAllBtn.setOnAction(e -> switchView(ViewType.TRANSACTIONS));

        recentHeader.getChildren().addAll(recentTitle, spacer, addExpenseBtn, viewAllBtn);

        recentTableView = createConfiguredTableView(false);
        recentTableView.setPrefHeight(260);

        recentBox.getChildren().addAll(recentHeader, recentTableView);

        // Intelligence Highlights Banner
        HBox intelBanner = new HBox(16);
        intelBanner.getStyleClass().add("insight-card");
        intelBanner.setAlignment(Pos.CENTER_LEFT);

        VBox intelTextBox = new VBox(4);
        HBox.setHgrow(intelTextBox, Priority.ALWAYS);

        HBox intelHeader = new HBox(8);
        intelHeader.setAlignment(Pos.CENTER_LEFT);
        Label intelTitle = new Label("💡 Personal Financial Intelligence Highlights");
        intelTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        dashboardIntelBadge = new Label("Healthy Baseline");
        dashboardIntelBadge.getStyleClass().setAll("badge", "badge-neutral");
        intelHeader.getChildren().addAll(intelTitle, dashboardIntelBadge);

        dashboardIntelSummaryLbl = new Label("Loading spending patterns and statistical baseline...");
        dashboardIntelSummaryLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        dashboardIntelSummaryLbl.setWrapText(true);
        intelTextBox.getChildren().addAll(intelHeader, dashboardIntelSummaryLbl);

        Button exploreIntelBtn = new Button("Explore Spending Insights →");
        exploreIntelBtn.getStyleClass().addAll("btn", "btn-primary");
        exploreIntelBtn.setOnAction(e -> switchView(ViewType.INSIGHTS));

        intelBanner.getChildren().addAll(intelTextBox, exploreIntelBtn);

        view.getChildren().addAll(headerBox, kpiGrid, chartsRow, intelBanner, recentBox);
        return view;
    }

    private VBox createStatCard(String title, String initialVal, String accentColor, String icon, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stat-card-title");
        top.getChildren().addAll(iconLbl, titleLbl);

        Label valueLbl = new Label(initialVal);
        valueLbl.getStyleClass().add("stat-card-value");
        valueLbl.setStyle("-fx-text-fill: " + accentColor + ";");

        Label subLbl = new Label(subtitle);
        subLbl.getStyleClass().add("stat-card-sub");

        card.getChildren().addAll(top, valueLbl, subLbl);
        return card;
    }

    // =========================================================================
    // View 2: Transactions (Entry Form & Full Ledger)
    // =========================================================================

    private VBox buildTransactionsView() {
        VBox view = new VBox(16);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Transactions Ledger");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Record, edit, filter, and manage your daily expenses.");
        sub.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(title, sub);

        // Content split: Left Entry Form (320px) | Right Table + Filters
        HBox contentBox = new HBox(18);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        VBox formBox = buildExpenseEntryForm();
        VBox tableBox = buildTableLedgerSection();
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        contentBox.getChildren().addAll(formBox, tableBox);
        view.getChildren().addAll(headerBox, contentBox);
        return view;
    }

    private VBox buildExpenseEntryForm() {
        VBox form = new VBox(12);
        form.setPrefWidth(320);
        form.setMinWidth(300);
        form.setMaxWidth(340);
        form.getStyleClass().add("card");

        Label formTitle = new Label("Expense Details");
        formTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        // Amount Field
        Label amtLabel = new Label("Amount (₹)*");
        amtLabel.getStyleClass().add("form-label");
        amountField = new TextField();
        amountField.setPromptText("e.g. 150.00");

        // Description Field & AI Badge
        Label descLabel = new Label("Description*");
        descLabel.getStyleClass().add("form-label");
        descriptionField = new TextField();
        descriptionField.setPromptText("e.g. Uber ride, Starbucks, Grocery");

        aiBadge = new Label("🤖 AI: Enter description to auto-categorize");
        aiBadge.getStyleClass().addAll("badge", "badge-neutral");
        aiBadge.setWrapText(true);

        descriptionField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.trim().isEmpty()) {
                AICategorizer.PredictionResult result = expenseService.getCategorizer().predictCategory(newText);
                if (result.isConfident()) {
                    categoryComboBox.setValue(result.getCategory());
                    aiBadge.setText(String.format("🤖 Auto-categorized: %s (%.0f%%)", result.getCategory(), result.getConfidence() * 100));
                    aiBadge.getStyleClass().setAll("badge", "badge-success");
                } else {
                    aiBadge.setText("🤖 Suggested: " + result.getCategory() + " (Select manually if needed)");
                    aiBadge.getStyleClass().setAll("badge", "badge-warning");
                }
            } else {
                aiBadge.setText("🤖 AI: Enter description to auto-categorize");
                aiBadge.getStyleClass().setAll("badge", "badge-neutral");
            }
        });

        // Date Picker
        Label dateLabel = new Label("Date*");
        dateLabel.getStyleClass().add("form-label");
        datePicker = new DatePicker(LocalDate.now());
        datePicker.setMaxWidth(Double.MAX_VALUE);

        // Category ComboBox
        Label catLabel = new Label("Category*");
        catLabel.getStyleClass().add("form-label");
        categoryComboBox = new ComboBox<>();
        categoryComboBox.setItems(FXCollections.observableArrayList(expenseService.getCategorizer().getAvailableCategories()));
        categoryComboBox.setValue("Other");
        categoryComboBox.setMaxWidth(Double.MAX_VALUE);

        // Form Action Buttons
        saveExpenseButton = new Button("➕ Add Expense");
        saveExpenseButton.setMaxWidth(Double.MAX_VALUE);
        saveExpenseButton.getStyleClass().addAll("btn", "btn-primary");
        saveExpenseButton.setOnAction(e -> handleSaveExpense());

        updateExpenseButton = new Button("✏️ Update Selected");
        updateExpenseButton.setMaxWidth(Double.MAX_VALUE);
        updateExpenseButton.getStyleClass().addAll("btn", "btn-success");
        updateExpenseButton.setDisable(true);
        updateExpenseButton.setOnAction(e -> handleUpdateExpense());

        clearFormButton = new Button("🔄 Clear Form");
        clearFormButton.setMaxWidth(Double.MAX_VALUE);
        clearFormButton.getStyleClass().addAll("btn", "btn-secondary");
        clearFormButton.setOnAction(e -> clearFormFields());

        form.getChildren().addAll(
                formTitle,
                amtLabel, amountField,
                descLabel, descriptionField, aiBadge,
                dateLabel, datePicker,
                catLabel, categoryComboBox,
                saveExpenseButton,
                updateExpenseButton,
                clearFormButton
        );

        return form;
    }

    private VBox buildTableLedgerSection() {
        VBox section = new VBox(12);

        // Filter / Search Toolbar
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search description or category...");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        monthFilterComboBox = new ComboBox<>();
        monthFilterComboBox.setPrefWidth(140);
        monthFilterComboBox.setValue("All Months");
        monthFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        Button sortDateBtn = new Button("📅 Date ⇅");
        sortDateBtn.getStyleClass().addAll("btn", "btn-secondary");
        sortDateBtn.setOnAction(e -> {
            sortDateAsc = !sortDateAsc;
            sortDateBtn.setText(sortDateAsc ? "📅 Date ↑" : "📅 Date ↓");
            Comparator<Expense> cmp = Comparator.comparing(Expense::getDate);
            if (!sortDateAsc) cmp = cmp.reversed();
            FXCollections.sort(masterExpenseList, cmp);
        });

        Button sortAmountBtn = new Button("💲 Amount ⇅");
        sortAmountBtn.getStyleClass().addAll("btn", "btn-secondary");
        sortAmountBtn.setOnAction(e -> {
            sortAmountAsc = !sortAmountAsc;
            sortAmountBtn.setText(sortAmountAsc ? "💲 Amount ↑" : "💲 Amount ↓");
            Comparator<Expense> cmp = Comparator.comparing(Expense::getAmount);
            if (!sortAmountAsc) cmp = cmp.reversed();
            FXCollections.sort(masterExpenseList, cmp);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("btn", "btn-secondary");
        refreshBtn.setOnAction(e -> refreshExpensesFromDatabase());

        toolbar.getChildren().addAll(searchField, monthFilterComboBox, sortDateBtn, sortAmountBtn, spacer, refreshBtn);

        // Main TableView
        mainTableView = createConfiguredTableView(true);
        VBox.setVgrow(mainTableView, Priority.ALWAYS);

        section.getChildren().addAll(toolbar, mainTableView);
        return section;
    }

    private TableView<Expense> createConfiguredTableView(boolean includeActions) {
        TableView<Expense> table = new TableView<>();
        table.setItems(includeActions ? filteredExpenseList : masterExpenseList);
        table.getStyleClass().add("table-view");

        Label placeholder = new Label("No expense records found.\nEnter a new transaction to get started.");
        placeholder.getStyleClass().add("table-placeholder");
        table.setPlaceholder(placeholder);

        TableColumn<Expense, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedDate"));
        dateCol.setPrefWidth(95);

        TableColumn<Expense, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("category"));
        catCol.setPrefWidth(130);

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("description"));
        descCol.setPrefWidth(220);

        TableColumn<Expense, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedAmount"));
        amtCol.setPrefWidth(100);
        amtCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        table.getColumns().add(idCol);
        table.getColumns().add(dateCol);
        table.getColumns().add(catCol);
        table.getColumns().add(descCol);
        table.getColumns().add(amtCol);

        if (includeActions) {
            TableColumn<Expense, Void> actionCol = new TableColumn<>("Action");
            actionCol.setPrefWidth(75);
            actionCol.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("🗑️");

                {
                    deleteBtn.getStyleClass().addAll("btn", "btn-danger");
                    deleteBtn.setOnAction(e -> {
                        TableRow<Expense> row = getTableRow();
                        if (row != null && row.getItem() != null) {
                            handleDeleteExpense(row.getItem());
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteBtn);
                    }
                }
            });
            table.getColumns().add(actionCol);

            // Selection listener to populate form for editing
            table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel != null) {
                    selectedExpense = newSel;
                    amountField.setText(MoneyUtil.formatPlain(newSel.getAmountBigDecimal()));
                    descriptionField.setText(newSel.getDescription());
                    datePicker.setValue(newSel.getDate());
                    categoryComboBox.setValue(newSel.getCategory());
                    updateExpenseButton.setDisable(false);
                    saveExpenseButton.setDisable(true);
                }
            });
        }

        return table;
    }

    // =========================================================================
    // View 3: Analytics (Interactive Line & Grouped Bar Charts, MoM Normalization)
    // =========================================================================

    private VBox buildAnalyticsView() {
        VBox view = new VBox(20);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Spending Analytics & Visual Trends");
        title.getStyleClass().add("page-title");
        Label sub = new Label("High-resolution chronological timelines, category distributions, and normalized comparisons.");
        sub.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(title, sub);

        // Time Range Filter Toolbar & Metric Ribbon Card
        VBox filterCard = new VBox(14);
        filterCard.getStyleClass().add("card");

        HBox filterToolbar = new HBox(12);
        filterToolbar.setAlignment(Pos.CENTER_LEFT);

        Label rangeLabel = new Label("Analysis Horizon:");
        rangeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 13px;");

        lineChartRangeCombo = new ComboBox<>();
        lineChartRangeCombo.getItems().addAll(TimeRange.values());
        lineChartRangeCombo.setValue(TimeRange.LAST_30_DAYS);
        lineChartRangeCombo.setPrefWidth(160);
        lineChartRangeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                renderSpendingLineChart();
            }
        });

        Region ribbonSpacer = new Region();
        HBox.setHgrow(ribbonSpacer, Priority.ALWAYS);

        filterToolbar.getChildren().addAll(rangeLabel, lineChartRangeCombo, ribbonSpacer);

        // Quick Metric Ribbon Row
        HBox ribbon = new HBox(12);
        VBox chip1 = createMetricChip("PERIOD TOTAL", "₹0.00", "#2563EB");
        analyticsPeriodTotalLbl = (Label) chip1.getChildren().get(1);

        VBox chip2 = createMetricChip("DAILY AVERAGE", "₹0.00 / day", "#059669");
        analyticsPeriodAvgLbl = (Label) chip2.getChildren().get(1);

        VBox chip3 = createMetricChip("PEAK SPENDING", "₹0.00", "#DC2626");
        analyticsPeriodPeakLbl = (Label) chip3.getChildren().get(1);

        VBox chip4 = createMetricChip("ACTIVE ENTRIES", "0 Records", "#7C3AED");
        analyticsPeriodCountLbl = (Label) chip4.getChildren().get(1);

        HBox.setHgrow(chip1, Priority.ALWAYS);
        HBox.setHgrow(chip2, Priority.ALWAYS);
        HBox.setHgrow(chip3, Priority.ALWAYS);
        HBox.setHgrow(chip4, Priority.ALWAYS);
        ribbon.getChildren().addAll(chip1, chip2, chip3, chip4);

        filterCard.getChildren().addAll(filterToolbar, ribbon);

        // Chart 1: Spending Over Time (LineChart Card)
        VBox lineCard = new VBox(12);
        lineCard.getStyleClass().add("card");

        VBox lineHeader = new VBox(3);
        Label lineTitle = new Label("📈 Spending Over Time");
        lineTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        lineChartSubtitle = new Label("Daily expenditure trajectory");
        lineChartSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        lineHeader.getChildren().addAll(lineTitle, lineChartSubtitle);

        CategoryAxis lineXAxis = new CategoryAxis();
        lineXAxis.setLabel("Timeline");
        lineXAxis.setAnimated(false);

        NumberAxis lineYAxis = new NumberAxis();
        lineYAxis.setLabel("Spending Amount (₹)");
        lineYAxis.setTickLabelFormatter(new CurrencyAxisFormatter());
        lineYAxis.setAnimated(false);

        spendingLineChart = new LineChart<>(lineXAxis, lineYAxis);
        spendingLineChart.setPrefHeight(340);
        spendingLineChart.setLegendVisible(false);
        spendingLineChart.setAnimated(false);
        spendingLineChart.setCreateSymbols(true);

        lineChartEmptyState = createEmptyStateLabel("No spending recorded in this period.\nAdd expenses to visualize timeline trajectory.");
        lineChartEmptyState.setVisible(false);

        StackPane lineStack = new StackPane(spendingLineChart, lineChartEmptyState);
        lineCard.getChildren().addAll(lineHeader, lineStack);

        // Bottom Row: Category Breakdown (BarChart) + Month-over-Month Comparison (Grouped BarChart)
        HBox bottomRow = new HBox(16);

        // Chart 2: Category Breakdown Card
        VBox catCard = new VBox(12);
        catCard.getStyleClass().add("card");
        HBox.setHgrow(catCard, Priority.ALWAYS);

        VBox catHeader = new VBox(3);
        Label catTitle = new Label("📊 Category Spending Breakdown");
        catTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        categoryBarSubtitle = new Label("Ranked from highest to lowest expenditure");
        categoryBarSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        catHeader.getChildren().addAll(catTitle, categoryBarSubtitle);

        CategoryAxis catXAxis = new CategoryAxis();
        catXAxis.setLabel("Category");
        catXAxis.setTickLabelRotation(-25);
        catXAxis.setAnimated(false);

        NumberAxis catYAxis = new NumberAxis();
        catYAxis.setLabel("Total Amount (₹)");
        catYAxis.setTickLabelFormatter(new CurrencyAxisFormatter());
        catYAxis.setAnimated(false);

        categoryBarChart = new BarChart<>(catXAxis, catYAxis);
        categoryBarChart.setPrefHeight(340);
        categoryBarChart.setLegendVisible(false);
        categoryBarChart.setAnimated(false);

        categoryBarEmptyState = createEmptyStateLabel("No expense categories found.");
        categoryBarEmptyState.setVisible(false);

        StackPane catStack = new StackPane(categoryBarChart, categoryBarEmptyState);
        catCard.getChildren().addAll(catHeader, catStack);

        // Chart 3: Month-over-Month Comparison Card
        VBox momCard = new VBox(12);
        momCard.getStyleClass().add("card");
        HBox.setHgrow(momCard, Priority.ALWAYS);

        HBox momTopRow = new HBox(8);
        momTopRow.setAlignment(Pos.CENTER_LEFT);

        VBox momHeader = new VBox(3);
        Label momTitle = new Label("🔄 Month-over-Month Comparison");
        momTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        momSubtitle = new Label("Current Month vs Prior Month by Category");
        momSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        momHeader.getChildren().addAll(momTitle, momSubtitle);
        HBox.setHgrow(momHeader, Priority.ALWAYS);

        momNormalizeToggle = new CheckBox("Normalize MTD (Day 1–N)");
        momNormalizeToggle.setSelected(true);
        momNormalizeToggle.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        momNormalizeToggle.setOnAction(e -> renderMoMBarChart());

        momTopRow.getChildren().addAll(momHeader, momNormalizeToggle);

        CategoryAxis momXAxis = new CategoryAxis();
        momXAxis.setLabel("Category");
        momXAxis.setTickLabelRotation(-25);
        momXAxis.setAnimated(false);

        NumberAxis momYAxis = new NumberAxis();
        momYAxis.setLabel("Spending (₹)");
        momYAxis.setTickLabelFormatter(new CurrencyAxisFormatter());
        momYAxis.setAnimated(false);

        momBarChart = new BarChart<>(momXAxis, momYAxis);
        momBarChart.setPrefHeight(340);
        momBarChart.setLegendVisible(true);
        momBarChart.setAnimated(false);

        momEmptyState = createEmptyStateLabel("Requires at least 2 calendar months of records\nto plot Month-over-Month comparison.");
        momEmptyState.setVisible(false);

        momSummaryFootnote = new Label();
        momSummaryFootnote.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");
        momSummaryFootnote.setWrapText(true);

        StackPane momStack = new StackPane(momBarChart, momEmptyState);
        momCard.getChildren().addAll(momTopRow, momStack, momSummaryFootnote);

        bottomRow.getChildren().addAll(catCard, momCard);

        view.getChildren().addAll(headerBox, filterCard, lineCard, bottomRow);
        return view;
    }

    // =========================================================================
    // View 4: Reports (Export Hub & Aggregation)
    // =========================================================================

    private VBox buildReportsView() {
        VBox view = new VBox(20);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Financial Reports & Statement Generator");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Generate comprehensive financial statements, filter by reporting period and category, analyze category distributions, and export official CSV & Excel statements.");
        sub.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(title, sub);

        // Filter / Control Ribbon Card
        VBox ribbonCard = new VBox(12);
        ribbonCard.getStyleClass().add("card");

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        // Period Preset
        VBox periodBox = new VBox(4);
        Label periodLbl = new Label("REPORTING PERIOD");
        periodLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        reportPeriodPresetCombo = new ComboBox<>();
        reportPeriodPresetCombo.getItems().addAll(ReportFilter.PeriodPreset.values());
        reportPeriodPresetCombo.setValue(ReportFilter.PeriodPreset.ALL_TIME);
        reportPeriodPresetCombo.setStyle("-fx-font-weight: bold; -fx-pref-width: 170px;");
        periodBox.getChildren().addAll(periodLbl, reportPeriodPresetCombo);

        // Category Filter
        VBox catFilterBox = new VBox(4);
        Label catFilterLbl = new Label("CATEGORY SCOPE");
        catFilterLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        reportCategoryFilterCombo = new ComboBox<>();
        reportCategoryFilterCombo.getItems().add("All Categories");
        reportCategoryFilterCombo.setValue("All Categories");
        reportCategoryFilterCombo.setStyle("-fx-pref-width: 170px;");
        catFilterBox.getChildren().addAll(catFilterLbl, reportCategoryFilterCombo);

        // Custom Date Range (managed/visible only when CUSTOM is selected)
        reportCustomDatesBox = new HBox(8);
        reportCustomDatesBox.setAlignment(Pos.CENTER_LEFT);
        reportCustomDatesBox.setVisible(false);
        reportCustomDatesBox.setManaged(false);

        VBox startBox = new VBox(4);
        Label startLbl = new Label("FROM DATE");
        startLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        reportStartDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        reportStartDatePicker.setStyle("-fx-pref-width: 130px;");
        startBox.getChildren().addAll(startLbl, reportStartDatePicker);

        VBox endBox = new VBox(4);
        Label endLbl = new Label("TO DATE");
        endLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        reportEndDatePicker = new DatePicker(LocalDate.now());
        reportEndDatePicker.setStyle("-fx-pref-width: 130px;");
        endBox.getChildren().addAll(endLbl, reportEndDatePicker);

        reportCustomDatesBox.getChildren().addAll(startBox, endBox);

        // Trigger filters on selection changes
        reportPeriodPresetCombo.setOnAction(e -> {
            boolean isCustom = reportPeriodPresetCombo.getValue() == ReportFilter.PeriodPreset.CUSTOM;
            reportCustomDatesBox.setVisible(isCustom);
            reportCustomDatesBox.setManaged(isCustom);
            generateAndDisplayReport();
        });
        reportCategoryFilterCombo.setOnAction(e -> generateAndDisplayReport());
        reportStartDatePicker.setOnAction(e -> {
            if (reportPeriodPresetCombo.getValue() == ReportFilter.PeriodPreset.CUSTOM) {
                generateAndDisplayReport();
            }
        });
        reportEndDatePicker.setOnAction(e -> {
            if (reportPeriodPresetCombo.getValue() == ReportFilter.PeriodPreset.CUSTOM) {
                generateAndDisplayReport();
            }
        });

        Region ribbonSpacer = new Region();
        HBox.setHgrow(ribbonSpacer, Priority.ALWAYS);

        // Action buttons
        Button generateBtn = new Button("🔄 Refresh");
        generateBtn.getStyleClass().addAll("btn", "btn-secondary");
        generateBtn.setOnAction(e -> generateAndDisplayReport());

        Button exportExcelBtn = new Button("📗 Export Excel");
        exportExcelBtn.getStyleClass().addAll("btn", "btn-primary");
        exportExcelBtn.setOnAction(e -> handleExportReportExcel());

        Button exportCsvBtn = new Button("📊 Export CSV");
        exportCsvBtn.getStyleClass().addAll("btn", "btn-success");
        exportCsvBtn.setOnAction(e -> handleExportReportCSV());

        Button summaryModalBtn = new Button("📋 Executive Summary");
        summaryModalBtn.getStyleClass().addAll("btn", "btn-secondary");
        summaryModalBtn.setOnAction(e -> showReportSummaryModal());

        HBox actionBtnBox = new HBox(8);
        actionBtnBox.setAlignment(Pos.BOTTOM_RIGHT);
        actionBtnBox.getChildren().addAll(generateBtn, exportExcelBtn, exportCsvBtn, summaryModalBtn);

        filterRow.getChildren().addAll(periodBox, catFilterBox, reportCustomDatesBox, ribbonSpacer, actionBtnBox);

        // Status metadata label
        reportMetaStatusLbl = new Label("Select reporting criteria above to generate statement.");
        reportMetaStatusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        ribbonCard.getChildren().addAll(filterRow, reportMetaStatusLbl);

        // KPI Summary Cards Grid
        HBox kpiGrid = new HBox(16);
        VBox cardTotal = createStatCard("TOTAL EXPENDITURE", "₹0.00", "#2563EB", "💳", "Selected period");
        reportKpiTotalSpendLbl = (Label) cardTotal.getChildren().get(1);

        VBox cardCount = createStatCard("RECORDED TRANSACTIONS", "0 Records", "#059669", "📝", "Matching filter");
        reportKpiTxCountLbl = (Label) cardCount.getChildren().get(1);

        VBox cardAvg = createStatCard("AVERAGE SPEND / TX", "₹0.00", "#D97706", "📈", "Per transaction");
        reportKpiAvgSpendLbl = (Label) cardAvg.getChildren().get(1);

        VBox cardTopCat = createStatCard("TOP CATEGORY", "None", "#7C3AED", "🏷️", "Highest category");
        reportKpiTopCatLbl = (Label) cardTopCat.getChildren().get(1);

        VBox cardPeakDay = createStatCard("PEAK SPENDING DAY", "None", "#DC2626", "⚡", "Single day high");
        reportKpiPeakDayLbl = (Label) cardPeakDay.getChildren().get(1);

        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardCount, Priority.ALWAYS);
        HBox.setHgrow(cardAvg, Priority.ALWAYS);
        HBox.setHgrow(cardTopCat, Priority.ALWAYS);
        HBox.setHgrow(cardPeakDay, Priority.ALWAYS);
        kpiGrid.getChildren().addAll(cardTotal, cardCount, cardAvg, cardTopCat, cardPeakDay);

        // Section 1: Category Rollup Breakdown Table
        VBox catBreakdownCard = new VBox(12);
        catBreakdownCard.getStyleClass().add("card");
        Label catTitle = new Label("📊 Category Rollup & Expenditure Distribution");
        catTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label catSub = new Label("Aggregated totals, transaction volumes, and percentage contribution per category.");
        catSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        reportCategoryTable = new TableView<>();
        reportCategoryTable.getStyleClass().add("table-view");
        reportCategoryTable.setPrefHeight(200);

        TableColumn<CategoryReportRow, String> rCatCol = new TableColumn<>("Category");
        rCatCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("category"));
        rCatCol.setPrefWidth(220);

        TableColumn<CategoryReportRow, Integer> rCountCol = new TableColumn<>("Transactions");
        rCountCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("transactionCount"));
        rCountCol.setPrefWidth(130);
        rCountCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<CategoryReportRow, String> rAmtCol = new TableColumn<>("Total Spent");
        rAmtCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedTotalAmount"));
        rAmtCol.setPrefWidth(180);
        rAmtCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        TableColumn<CategoryReportRow, String> rPctCol = new TableColumn<>("% Share of Spend");
        rPctCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedPercentage"));
        rPctCol.setPrefWidth(160);
        rPctCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        reportCategoryTable.getColumns().addAll(rCatCol, rCountCol, rAmtCol, rPctCol);
        catBreakdownCard.getChildren().addAll(catTitle, catSub, reportCategoryTable);

        // Section 2: Statement Transaction Ledger
        VBox ledgerCard = new VBox(12);
        ledgerCard.getStyleClass().add("card");
        Label ledgerTitle = new Label("📑 Statement Itemized Transactions");
        ledgerTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label ledgerSub = new Label("All transactions included within this report's filtered date range and category scope.");
        ledgerSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        reportLedgerTable = new TableView<>();
        reportLedgerTable.getStyleClass().add("table-view");
        reportLedgerTable.setPrefHeight(300);

        TableColumn<Expense, Integer> lIdCol = new TableColumn<>("ID");
        lIdCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        lIdCol.setPrefWidth(60);

        TableColumn<Expense, String> lDateCol = new TableColumn<>("Date");
        lDateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedDate"));
        lDateCol.setPrefWidth(110);

        TableColumn<Expense, String> lCatCol = new TableColumn<>("Category");
        lCatCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("category"));
        lCatCol.setPrefWidth(150);

        TableColumn<Expense, String> lDescCol = new TableColumn<>("Description");
        lDescCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("description"));
        lDescCol.setPrefWidth(320);

        TableColumn<Expense, String> lAmtCol = new TableColumn<>("Amount");
        lAmtCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedAmount"));
        lAmtCol.setPrefWidth(140);
        lAmtCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        reportLedgerTable.getColumns().addAll(lIdCol, lDateCol, lCatCol, lDescCol, lAmtCol);
        ledgerCard.getChildren().addAll(ledgerTitle, ledgerSub, reportLedgerTable);

        view.getChildren().addAll(headerBox, ribbonCard, kpiGrid, catBreakdownCard, ledgerCard);
        return view;
    }

    // =========================================================================
    // CRUD & Background Operations
    // =========================================================================

    private void refreshExpensesFromDatabase() {
        setLoading(true);
        Task<List<Expense>> loadTask = new Task<>() {
            @Override
            protected List<Expense> call() {
                return expenseService.getAllExpenses();
            }
        };

        loadTask.setOnSucceeded(e -> {
            setLoading(false);
            masterExpenseList.setAll(loadTask.getValue());
            updateMonthFilterOptions();
            updateAnalytics();
        });

        loadTask.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = loadTask.getException();
            showStatusBanner("Database error: " + (ex != null ? ex.getMessage() : "Unknown error"), false);
        });

        executorService.submit(loadTask);
    }

    private void handleSaveExpense() {
        try {
            BigDecimal amount = MoneyUtil.parseAmount(amountField.getText());
            String desc = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
            String category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();

            setLoading(true);
            Task<Expense> addTask = new Task<>() {
                @Override
                protected Expense call() {
                    return expenseService.addExpense(amount.doubleValue(), category, desc, date);
                }
            };

            addTask.setOnSucceeded(e -> {
                setLoading(false);
                masterExpenseList.add(0, addTask.getValue());
                clearFormFields();
                updateMonthFilterOptions();
                updateAnalytics();
                showStatusBanner("Expense recorded successfully!", true);
            });

            addTask.setOnFailed(e -> {
                setLoading(false);
                Throwable ex = addTask.getException();
                showStatusBanner(ex != null ? ex.getMessage() : "Failed to record expense.", false);
            });

            executorService.submit(addTask);

        } catch (IllegalArgumentException e) {
            showStatusBanner(e.getMessage(), false);
        }
    }

    private void handleUpdateExpense() {
        if (selectedExpense == null) return;

        try {
            BigDecimal amount = MoneyUtil.parseAmount(amountField.getText());
            String desc = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
            String category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();

            selectedExpense.setAmount(amount);
            selectedExpense.setDescription(desc);
            selectedExpense.setCategory(category);
            selectedExpense.setDate(date);

            setLoading(true);
            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() {
                    expenseService.updateExpense(selectedExpense);
                    return null;
                }
            };

            updateTask.setOnSucceeded(e -> {
                setLoading(false);
                mainTableView.refresh();
                if (recentTableView != null) recentTableView.refresh();
                clearFormFields();
                updateMonthFilterOptions();
                updateAnalytics();
                showStatusBanner("Expense updated successfully!", true);
            });

            updateTask.setOnFailed(e -> {
                setLoading(false);
                Throwable ex = updateTask.getException();
                showStatusBanner(ex != null ? ex.getMessage() : "Failed to update expense.", false);
            });

            executorService.submit(updateTask);

        } catch (IllegalArgumentException e) {
            showStatusBanner(e.getMessage(), false);
        }
    }

    private void handleDeleteExpense(Expense expense) {
        if (expense == null || expense.getId() == null) return;

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete expense '" + expense.getDescription() + "' (" + expense.getFormattedAmount() + ")?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText(null);
        confirm.setTitle("Confirm Deletion");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                setLoading(true);
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() {
                        expenseService.deleteExpense(expense.getId());
                        return null;
                    }
                };

                deleteTask.setOnSucceeded(e -> {
                    setLoading(false);
                    masterExpenseList.remove(expense);
                    clearFormFields();
                    updateMonthFilterOptions();
                    updateAnalytics();
                    showStatusBanner("Expense deleted successfully.", true);
                });

                deleteTask.setOnFailed(e -> {
                    setLoading(false);
                    Throwable ex = deleteTask.getException();
                    showStatusBanner(ex != null ? ex.getMessage() : "Failed to delete expense.", false);
                });

                executorService.submit(deleteTask);
            }
        });
    }

    private void handleExportCSV() {
        if (masterExpenseList.isEmpty()) {
            showStatusBanner("No expenses available to export.", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Expenses CSV");
        fileChooser.setInitialFileName("Expense_Report_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            setLoading(true);
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return CSVExporter.exportToCSV(file, masterExpenseList);
                }
            };

            exportTask.setOnSucceeded(e -> {
                setLoading(false);
                showStatusBanner("CSV exported successfully to " + file.getName(), true);
            });

            exportTask.setOnFailed(e -> {
                setLoading(false);
                Throwable ex = exportTask.getException();
                showStatusBanner("CSV Export failed: " + (ex != null ? ex.getMessage() : "Unknown error"), false);
            });

            executorService.submit(exportTask);
        }
    }

    private void handleExportExcel() {
        if (masterExpenseList.isEmpty()) {
            showStatusBanner("No expenses available to export.", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Expenses Excel File");
        fileChooser.setInitialFileName("Expense_Report_" + LocalDate.now() + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            setLoading(true);
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return ExcelExporter.exportToExcel(file, masterExpenseList);
                }
            };

            exportTask.setOnSucceeded(e -> {
                setLoading(false);
                showStatusBanner("Excel report exported successfully to " + file.getName(), true);
            });

            exportTask.setOnFailed(e -> {
                setLoading(false);
                Throwable ex = exportTask.getException();
                showStatusBanner("Excel Export failed: " + (ex != null ? ex.getMessage() : "Unknown error"), false);
            });

            executorService.submit(exportTask);
        }
    }

    // =========================================================================
    // Filtering & Analytics Synchronization
    // =========================================================================

    private void applyFilters() {
        String search = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase()
                : "";

        String selectedMonth = monthFilterComboBox != null && monthFilterComboBox.getValue() != null
                ? monthFilterComboBox.getValue()
                : "All Months";

        filteredExpenseList.setPredicate(expense -> {
            if (expense == null) return false;

            boolean matchesSearch = true;
            if (!search.isEmpty()) {
                matchesSearch = (expense.getDescription() != null && expense.getDescription().toLowerCase().contains(search))
                        || (expense.getCategory() != null && expense.getCategory().toLowerCase().contains(search));
            }

            boolean matchesMonth = true;
            if (!selectedMonth.equals("All Months") && expense.getDate() != null) {
                String ym = expense.getDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
                matchesMonth = ym.equals(selectedMonth);
            }

            return matchesSearch && matchesMonth;
        });

        updateAnalytics();
    }

    private void updateAnalytics() {
        // All-time executive summary for Dashboard KPI cards
        ExpenseSummary summary = expenseService.calculateSummary(masterExpenseList);

        if (kpiTotalSpending != null) kpiTotalSpending.setText(summary.getFormattedTotalSpending());
        if (kpiTopCategory != null) kpiTopCategory.setText(summary.getTopCategory());
        if (kpiTransactionCount != null) kpiTransactionCount.setText(summary.getFormattedTransactionCount());
        if (kpiAverageExpense != null) kpiAverageExpense.setText(summary.getFormattedAverageExpense());

        // Update Dashboard Charts (PieChart & Monthly BarChart)
        renderDashboardCharts();

        // Update Visual Analytics Charts
        renderSpendingLineChart();
        renderCategoryBarChart();
        renderMoMBarChart();

        // Financial Intelligence analysis
        FinancialIntelligenceResult intel = expenseService.getFinancialIntelligence(masterExpenseList);
        updateInsightsView(intel);

        // Financial Reports synchronization
        updateReportCategoryFilterOptions();
        generateAndDisplayReport();
    }

    private void updateMonthFilterOptions() {
        if (monthFilterComboBox == null) return;

        String prev = monthFilterComboBox.getValue();
        monthFilterComboBox.getItems().clear();
        monthFilterComboBox.getItems().add("All Months");

        TreeMap<YearMonth, Boolean> months = new TreeMap<>();
        for (Expense exp : masterExpenseList) {
            if (exp.getDate() != null) {
                months.put(YearMonth.from(exp.getDate()), true);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        for (YearMonth ym : months.keySet()) {
            monthFilterComboBox.getItems().add(ym.format(fmt));
        }

        if (prev != null && monthFilterComboBox.getItems().contains(prev)) {
            monthFilterComboBox.setValue(prev);
        } else {
            monthFilterComboBox.setValue("All Months");
        }
    }

    private void updateReportCategoryFilterOptions() {
        if (reportCategoryFilterCombo == null) return;
        String current = reportCategoryFilterCombo.getValue();
        reportCategoryFilterCombo.getItems().clear();
        reportCategoryFilterCombo.getItems().add("All Categories");

        masterExpenseList.stream()
                .map(Expense::getCategory)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted()
                .forEach(reportCategoryFilterCombo.getItems()::add);

        if (current != null && reportCategoryFilterCombo.getItems().contains(current)) {
            reportCategoryFilterCombo.setValue(current);
        } else {
            reportCategoryFilterCombo.setValue("All Categories");
        }
    }

    private void generateAndDisplayReport() {
        ReportFilter.PeriodPreset preset = reportPeriodPresetCombo != null && reportPeriodPresetCombo.getValue() != null
                ? reportPeriodPresetCombo.getValue()
                : ReportFilter.PeriodPreset.ALL_TIME;

        String category = reportCategoryFilterCombo != null && reportCategoryFilterCombo.getValue() != null
                ? reportCategoryFilterCombo.getValue()
                : "All Categories";

        LocalDate customStart = reportStartDatePicker != null ? reportStartDatePicker.getValue() : null;
        LocalDate customEnd = reportEndDatePicker != null ? reportEndDatePicker.getValue() : null;

        ReportFilter filter = new ReportFilter(preset, category, customStart, customEnd);
        FinancialReport report = expenseService.generateReport(masterExpenseList, filter);
        this.currentGeneratedReport = report;

        // Update Report KPIs
        if (reportKpiTotalSpendLbl != null) {
            reportKpiTotalSpendLbl.setText(report.getFormattedTotalAmount());
        }
        if (reportKpiTxCountLbl != null) {
            reportKpiTxCountLbl.setText(report.getTotalTransactions() + " Records");
        }
        if (reportKpiAvgSpendLbl != null) {
            reportKpiAvgSpendLbl.setText(report.getFormattedAverageTransaction());
        }
        if (reportKpiTopCatLbl != null) {
            reportKpiTopCatLbl.setText(report.getTopCategory() + (!report.getTopCategoryPercentage().isEmpty() ? " (" + report.getTopCategoryPercentage() + ")" : ""));
        }
        if (reportKpiPeakDayLbl != null) {
            reportKpiPeakDayLbl.setText(report.getFormattedPeakSpendingDay());
        }

        // Update Status Metadata
        if (reportMetaStatusLbl != null) {
            reportMetaStatusLbl.setText(String.format("Generated on %s • %s • %d record%s matched (Scope: %s)",
                    report.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    report.getPeriodDescription(),
                    report.getTotalTransactions(),
                    report.getTotalTransactions() == 1 ? "" : "s",
                    report.getFilterCategory()
            ));
        }

        // Update Tables
        if (reportCategoryTable != null) {
            reportCategoryTable.setItems(FXCollections.observableArrayList(report.getCategoryBreakdown()));
        }
        if (reportLedgerTable != null) {
            reportLedgerTable.setItems(FXCollections.observableArrayList(report.getTransactions()));
        }
    }

    private void handleExportReportCSV() {
        if (currentGeneratedReport == null || currentGeneratedReport.isEmpty()) {
            showStatusBanner("No report data available to export. Adjust filters or record expenses.", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Financial Report CSV");
        fileChooser.setInitialFileName("Expense_Statement_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            setLoading(true);
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return CSVExporter.exportReportToCSV(file, currentGeneratedReport);
                }
            };

            exportTask.setOnSucceeded(e -> {
                setLoading(false);
                showStatusBanner("Financial report statement exported successfully to " + file.getName(), true);
            });

            exportTask.setOnFailed(e -> {
                setLoading(false);
                Throwable ex = exportTask.getException();
                showStatusBanner("Report CSV Export failed: " + (ex != null ? ex.getMessage() : "Unknown error"), false);
            });

            executorService.submit(exportTask);
        }
    }

    private void handleExportReportExcel() {
        if (currentGeneratedReport == null || currentGeneratedReport.isEmpty()) {
            showStatusBanner("No report data available to export. Adjust filters or record expenses.", false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Financial Report Excel Statement");
        fileChooser.setInitialFileName("Expense_Statement_" + LocalDate.now() + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbooks (*.xlsx)", "*.xlsx"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            setLoading(true);
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return ExcelExporter.exportReportToExcel(file, currentGeneratedReport);
                }
            };

            exportTask.setOnSucceeded(e -> {
                setLoading(false);
                showStatusBanner("Financial report statement exported successfully to " + file.getName(), true);
            });

            exportTask.setOnFailed(e -> {
                setLoading(false);
                Throwable ex = exportTask.getException();
                showStatusBanner("Report Excel Export failed: " + (ex != null ? ex.getMessage() : "Unknown error"), false);
            });

            executorService.submit(exportTask);
        }
    }

    private void showReportSummaryModal() {
        if (currentGeneratedReport == null) {
            showStatusBanner("No active report generated.", false);
            return;
        }

        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Statement Executive Summary — " + currentGeneratedReport.getTitle());

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #F8FAFC;");
        root.setPrefWidth(550);

        Label titleLbl = new Label("📊 " + currentGeneratedReport.getTitle());
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subLbl = new Label("Period: " + currentGeneratedReport.getPeriodDescription() + " | Scope: " + currentGeneratedReport.getFilterCategory());
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        VBox summaryBox = new VBox(8);
        summaryBox.getStyleClass().add("card");

        summaryBox.getChildren().addAll(
                createSummaryModalRow("Total Expenditure", currentGeneratedReport.getFormattedTotalAmount(), "#2563EB"),
                createSummaryModalRow("Matching Transactions", currentGeneratedReport.getTotalTransactions() + " Records", "#0F172A"),
                createSummaryModalRow("Average Transaction", currentGeneratedReport.getFormattedAverageTransaction(), "#D97706"),
                createSummaryModalRow("Top Category", currentGeneratedReport.getTopCategory() + (!currentGeneratedReport.getTopCategoryPercentage().isEmpty() ? " (" + currentGeneratedReport.getTopCategoryPercentage() + ")" : ""), "#7C3AED"),
                createSummaryModalRow("Peak Spending Day", currentGeneratedReport.getFormattedPeakSpendingDay(), "#DC2626")
        );

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Close Summary");
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(titleLbl, subLbl, summaryBox, footer);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/styles/app.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private HBox createSummaryModalRow(String label, String value, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private void clearFormFields() {
        selectedExpense = null;
        amountField.clear();
        descriptionField.clear();
        datePicker.setValue(LocalDate.now());
        categoryComboBox.setValue("Other");
        aiBadge.setText("🤖 AI: Enter description to auto-categorize");
        aiBadge.getStyleClass().setAll("badge", "badge-neutral");

        saveExpenseButton.setDisable(false);
        updateExpenseButton.setDisable(true);

        if (mainTableView != null) {
            mainTableView.getSelectionModel().clearSelection();
        }
    }

    private void showStatusBanner(String message, boolean success) {
        statusBannerText.setText((success ? "✓ " : "⚠️ ") + message);
        statusBanner.getStyleClass().setAll(
                "status-banner",
                success ? "status-banner-success" : "status-banner-error"
        );
        statusBanner.setVisible(true);
        statusBanner.setManaged(true);

        // Auto-dismiss after 4 seconds
        new Thread(() -> {
            try {
                Thread.sleep(4000);
                Platform.runLater(() -> {
                    statusBanner.setVisible(false);
                    statusBanner.setManaged(false);
                });
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisible(loading);
    }

    // =========================================================================
    // View 5: Personal Financial Intelligence (Velocity, Shifts & Anomalies)
    // =========================================================================

    private VBox buildInsightsView() {
        VBox view = new VBox(20);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Personal Financial Intelligence");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Statistical analysis of spending velocity, month-over-month shifts, driver categories, and anomalies.");
        sub.getStyleClass().add("page-subtitle");
        headerBox.getChildren().addAll(title, sub);

        // Section 1: KPI Velocity & MoM Grid
        HBox velocityGrid = new HBox(16);

        VBox curMonthCard = createStatCard("CURRENT MONTH", "₹0.00", "#2563EB", "📅", "This month's total");
        insightsCurrentMonthVal = (Label) curMonthCard.getChildren().get(1);
        insightsCurrentMonthSub = (Label) curMonthCard.getChildren().get(2);

        VBox prevMonthCard = createStatCard("PREVIOUS MONTH", "₹0.00", "#64748B", "⏮️", "Prior month baseline");
        insightsPrevMonthVal = (Label) prevMonthCard.getChildren().get(1);
        insightsPrevMonthSub = (Label) prevMonthCard.getChildren().get(2);

        VBox changeCard = createStatCard("MONTH-OVER-MONTH", "₹0.00", "#059669", "📊", "Net shift vs prior");
        insightsChangeVal = (Label) changeCard.getChildren().get(1);
        insightsChangeSub = (Label) changeCard.getChildren().get(2);

        VBox velocityCard = createStatCard("30-DAY DAILY VELOCITY", "₹0.00 / day", "#D97706", "⚡", "Rolling daily burn rate");
        insightsVelocityVal = (Label) velocityCard.getChildren().get(1);
        insightsVelocitySub = (Label) velocityCard.getChildren().get(2);

        HBox.setHgrow(curMonthCard, Priority.ALWAYS);
        HBox.setHgrow(prevMonthCard, Priority.ALWAYS);
        HBox.setHgrow(changeCard, Priority.ALWAYS);
        HBox.setHgrow(velocityCard, Priority.ALWAYS);
        velocityGrid.getChildren().addAll(curMonthCard, prevMonthCard, changeCard, velocityCard);

        // Section 2: Key Takeaways & Observations
        VBox summaryCard = new VBox(10);
        summaryCard.getStyleClass().add("insight-card");
        Label summaryTitle = new Label("📌 Key Algorithmic Observations");
        summaryTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        summaryInsightsBox = new VBox(6);
        summaryCard.getChildren().addAll(summaryTitle, summaryInsightsBox);

        // Section 3: MoM Spending Drivers / Category Attribution
        VBox driversCard = new VBox(12);
        driversCard.getStyleClass().add("card");
        Label driversTitle = new Label("📈 Category Shifts & Spending Growth Drivers");
        driversTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label driversSub = new Label("Categories that contributed most to spending increases compared to previous month.");
        driversSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        growthDriversBox = new VBox(10);
        driversCard.getChildren().addAll(driversTitle, driversSub, growthDriversBox);

        // Section 4: Spending Anomalies (Spikes & Surges)
        VBox anomaliesCard = new VBox(12);
        anomaliesCard.getStyleClass().add("card");
        Label anomaliesTitle = new Label("🚨 Statistical Spending Anomalies");
        anomaliesTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label anomaliesSub = new Label("Transactions or monthly surges flagged via leave-one-out standard deviation (Z ≥ 2.0, Δ ≥ ₹150, ≥ 1.5× baseline).");
        anomaliesSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        anomaliesBox = new VBox(12);
        anomaliesCard.getChildren().addAll(anomaliesTitle, anomaliesSub, anomaliesBox);

        view.getChildren().addAll(headerBox, velocityGrid, summaryCard, driversCard, anomaliesCard);
        return view;
    }

    private void updateInsightsView(FinancialIntelligenceResult intel) {
        if (intel == null) return;

        MonthComparison comp = intel.getMonthComparison();

        // 1. Update Month Comparison KPIs
        if (insightsCurrentMonthVal != null && comp != null) {
            insightsCurrentMonthVal.setText(comp.getFormattedCurrentTotal());
            insightsCurrentMonthSub.setText(comp.getCurrentMonth() != null ? comp.getCurrentMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy")) : "Current Month");

            insightsPrevMonthVal.setText(comp.getFormattedPreviousTotal());
            insightsPrevMonthSub.setText(comp.getPreviousMonth() != null ? comp.getPreviousMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy")) : "Prior Month");

            if (!comp.isSufficientHistory()) {
                insightsChangeVal.setText("Baseline Building");
                insightsChangeVal.setStyle("-fx-text-fill: #64748B;");
                insightsChangeSub.setText("Requires ≥ 2 distinct months");
            } else {
                BigDecimal diff = comp.getNetDifference();
                insightsChangeVal.setText(comp.getFormattedNetDifference());
                insightsChangeSub.setText(comp.getFormattedPercentageChange() + " vs prior");
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    insightsChangeVal.setStyle("-fx-text-fill: #DC2626;");
                } else {
                    insightsChangeVal.setStyle("-fx-text-fill: #059669;");
                }
            }

            insightsVelocityVal.setText(intel.getFormattedAverageDailySpendLast30Days() + " / day");
            insightsVelocitySub.setText(String.format("Last 30 days (%d active spending day%s)",
                    intel.getTotalActiveSpendingDays(),
                    intel.getTotalActiveSpendingDays() == 1 ? "" : "s"));
        }

        // 2. Update Dashboard banner
        if (dashboardIntelSummaryLbl != null && dashboardIntelBadge != null) {
            int anomalyCount = intel.getAnomalies().size();
            if (anomalyCount > 0) {
                dashboardIntelBadge.setText(anomalyCount + " Anomaly" + (anomalyCount > 1 ? "ies" : "") + " Flagged");
                dashboardIntelBadge.getStyleClass().setAll("badge", "badge-danger");
            } else {
                dashboardIntelBadge.setText("Healthy Baseline");
                dashboardIntelBadge.getStyleClass().setAll("badge", "badge-neutral");
            }

            if (intel.getSummaryInsight() != null && !intel.getSummaryInsight().isEmpty()) {
                dashboardIntelSummaryLbl.setText(intel.getSummaryInsight());
            } else {
                dashboardIntelSummaryLbl.setText("No expense records available to establish baseline.");
            }
        }

        // 3. Update Key Algorithmic Observations Box
        if (summaryInsightsBox != null) {
            summaryInsightsBox.getChildren().clear();
            if (intel.getSummaryInsight() == null || intel.getSummaryInsight().isEmpty()) {
                Label noInsights = new Label("Add expense transactions to generate automated personal finance observations.");
                noInsights.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
                summaryInsightsBox.getChildren().add(noInsights);
            } else {
                // Primary executive summary
                HBox row1 = new HBox(8);
                row1.setAlignment(Pos.TOP_LEFT);
                Label dot1 = new Label("•");
                dot1.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3B82F6;");
                Label txt1 = new Label(intel.getSummaryInsight());
                txt1.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
                txt1.setWrapText(true);
                row1.getChildren().addAll(dot1, txt1);
                summaryInsightsBox.getChildren().add(row1);

                // 30-day velocity observation
                if (intel.getAverageDailySpendLast30Days().compareTo(BigDecimal.ZERO) > 0) {
                    HBox row2 = new HBox(8);
                    row2.setAlignment(Pos.TOP_LEFT);
                    Label dot2 = new Label("•");
                    dot2.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
                    Label txt2 = new Label(String.format("30-Day Spending Velocity: %s per day across %d active spending days.",
                            intel.getFormattedAverageDailySpendLast30Days(), intel.getTotalActiveSpendingDays()));
                    txt2.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
                    txt2.setWrapText(true);
                    row2.getChildren().addAll(dot2, txt2);
                    summaryInsightsBox.getChildren().add(row2);
                }

                // Anomaly status observation
                if (!intel.getAnomalies().isEmpty()) {
                    HBox row3 = new HBox(8);
                    row3.setAlignment(Pos.TOP_LEFT);
                    Label dot3 = new Label("•");
                    dot3.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
                    Label txt3 = new Label(String.format("Review flagged transactions below: %d statistical anomaly(ies) exceed category mean by ≥ 2.0σ.",
                            intel.getAnomalies().size()));
                    txt3.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
                    txt3.setWrapText(true);
                    row3.getChildren().addAll(dot3, txt3);
                    summaryInsightsBox.getChildren().add(row3);
                }
            }
        }

        // 4. Update Growth Drivers / Category Shifts
        if (growthDriversBox != null) {
            growthDriversBox.getChildren().clear();
            if (comp == null || (comp.getTopIncreaseContributors().isEmpty() && !comp.isSufficientHistory() && (comp.getCategoryTrends() == null || comp.getCategoryTrends().isEmpty()))) {
                Label info = new Label("Add expense records to identify category trends and growth drivers.");
                info.setStyle("-fx-text-fill: #64748B; -fx-font-style: italic;");
                growthDriversBox.getChildren().add(info);
            } else if (!comp.isSufficientHistory()) {
                // Single month dataset: display current category distribution with inspect buttons
                VBox singleMonthNotice = new VBox(6);
                singleMonthNotice.setStyle("-fx-background-color: #F0F9FF; -fx-border-color: #BAE6FD; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12;");
                Label noticeTitle = new Label("ℹ️ Baseline Building (Single Month Dataset)");
                noticeTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0369A1; -fx-font-size: 13px;");
                Label noticeSub = new Label("Showing active category distribution for "
                        + (comp.getCurrentMonth() != null ? comp.getCurrentMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy")) : "current month")
                        + ". Month-over-month shifts will activate once records span a 2nd calendar month.");
                noticeSub.setStyle("-fx-text-fill: #0284C7; -fx-font-size: 12px;");
                singleMonthNotice.getChildren().addAll(noticeTitle, noticeSub);
                growthDriversBox.getChildren().add(singleMonthNotice);

                if (comp.getCategoryTrends() != null) {
                    for (CategoryTrend driver : comp.getCategoryTrends()) {
                        HBox driverRow = new HBox(12);
                        driverRow.setAlignment(Pos.CENTER_LEFT);
                        driverRow.getStyleClass().add("insight-card");

                        VBox infoBox = new VBox(4);
                        HBox.setHgrow(infoBox, Priority.ALWAYS);

                        HBox nameRow = new HBox(8);
                        nameRow.setAlignment(Pos.CENTER_LEFT);
                        Label catName = new Label(driver.getCategory());
                        catName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

                        Label contribBadge = new Label(driver.getFormattedPercentageChange().equals("N/A (New)") ? "Active Category" : driver.getFormattedContribution() + " of total");
                        contribBadge.getStyleClass().setAll("badge", "badge-purple");
                        nameRow.getChildren().addAll(catName, contribBadge);

                        String shiftText = String.format("Current Spend: %s (%d transaction%s)",
                                driver.getFormattedCurrentAmount(),
                                driver.getCurrentTransactions().size(),
                                driver.getCurrentTransactions().size() == 1 ? "" : "s"
                        );
                        Label shiftLbl = new Label(shiftText);
                        shiftLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
                        infoBox.getChildren().addAll(nameRow, shiftLbl);

                        Button inspectBtn = new Button("🔍 Inspect Records");
                        inspectBtn.getStyleClass().addAll("btn", "btn-secondary");
                        inspectBtn.setOnAction(e -> showTransactionInspectorDialog(
                                "Category Records — " + driver.getCategory(),
                                "Showing current records for '" + driver.getCategory() + "'.",
                                driver.getCurrentTransactions()
                        ));

                        driverRow.getChildren().addAll(infoBox, inspectBtn);
                        growthDriversBox.getChildren().add(driverRow);
                    }
                }
            } else if (comp.getTopIncreaseContributors().isEmpty()) {
                Label info = new Label("✓ No category spending surges detected compared to previous month.");
                info.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                growthDriversBox.getChildren().add(info);
            } else {
                for (CategoryTrend driver : comp.getTopIncreaseContributors()) {
                    HBox driverRow = new HBox(12);
                    driverRow.setAlignment(Pos.CENTER_LEFT);
                    driverRow.getStyleClass().add("insight-card");

                    VBox infoBox = new VBox(4);
                    HBox.setHgrow(infoBox, Priority.ALWAYS);

                    HBox nameRow = new HBox(8);
                    nameRow.setAlignment(Pos.CENTER_LEFT);
                    Label catName = new Label(driver.getCategory());
                    catName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

                    Label contribBadge = new Label(driver.getFormattedContribution() + " of net surge");
                    contribBadge.getStyleClass().setAll("badge", "badge-trend-up");
                    nameRow.getChildren().addAll(catName, contribBadge);

                    String shiftText = String.format("Current: %s | Previous: %s | Shift: %s (%s)",
                            driver.getFormattedCurrentAmount(),
                            driver.getFormattedPreviousAmount(),
                            driver.getFormattedDifference(),
                            driver.getFormattedPercentageChange()
                    );
                    Label shiftLbl = new Label(shiftText);
                    shiftLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
                    infoBox.getChildren().addAll(nameRow, shiftLbl);

                    Button inspectBtn = new Button("🔍 Inspect Records");
                    inspectBtn.getStyleClass().addAll("btn", "btn-secondary");
                    inspectBtn.setOnAction(e -> showTransactionInspectorDialog(
                            "Supporting Transactions — " + driver.getCategory(),
                            "Showing current month records for '" + driver.getCategory() + "' contributing to month-over-month increase.",
                            driver.getCurrentTransactions()
                    ));

                    driverRow.getChildren().addAll(infoBox, inspectBtn);
                    growthDriversBox.getChildren().add(driverRow);
                }
            }
        }

        // 5. Update Anomalies Section
        if (anomaliesBox != null) {
            anomaliesBox.getChildren().clear();
            if (intel.getAnomalies().isEmpty()) {
                VBox cleanCard = new VBox(6);
                cleanCard.getStyleClass().add("card");
                cleanCard.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #86EFAC;");
                Label cleanTitle = new Label("✓ No Spending Anomalies Detected");
                cleanTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #166534; -fx-font-size: 14px;");
                Label cleanSub = new Label("All transactions and category monthly totals are within standard statistical tolerances (mean ± 2.0σ).");
                cleanSub.setStyle("-fx-text-fill: #15803D; -fx-font-size: 13px;");
                cleanCard.getChildren().addAll(cleanTitle, cleanSub);
                anomaliesBox.getChildren().add(cleanCard);
            } else {
                for (SpendingAnomaly anomaly : intel.getAnomalies()) {
                    VBox card = new VBox(10);
                    card.getStyleClass().add("anomaly-card");

                    HBox top = new HBox(10);
                    top.setAlignment(Pos.CENTER_LEFT);

                    Label typeBadge = new Label(anomaly.getType() == SpendingAnomaly.AnomalyType.TRANSACTION_SPIKE
                            ? "🚨 TRANSACTION SPIKE"
                            : "📈 MONTHLY SURGE");
                    typeBadge.getStyleClass().setAll("badge",
                            anomaly.getType() == SpendingAnomaly.AnomalyType.TRANSACTION_SPIKE ? "badge-danger" : "badge-purple");

                    Label catLbl = new Label(anomaly.getCategory());
                    catLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

                    Region sp = new Region();
                    HBox.setHgrow(sp, Priority.ALWAYS);

                    Label scoreLbl = new Label(String.format("Deviation: Z = %.2fσ", anomaly.getDeviationScore()));
                    scoreLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626; -fx-font-weight: bold;");

                    top.getChildren().addAll(typeBadge, catLbl, sp, scoreLbl);

                    HBox metricsRow = new HBox(20);
                    Label observedLbl = new Label("Observed: " + MoneyUtil.formatRupees(anomaly.getObservedAmount()));
                    observedLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

                    Label baselineLbl = new Label("Baseline: " + MoneyUtil.formatRupees(anomaly.getBaselineAmount()));
                    baselineLbl.setStyle("-fx-text-fill: #64748B;");

                    Label deltaLbl = new Label("Excess: +" + MoneyUtil.formatRupees(anomaly.getDifference()));
                    deltaLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");

                    metricsRow.getChildren().addAll(observedLbl, baselineLbl, deltaLbl);

                    HBox bottom = new HBox(12);
                    bottom.setAlignment(Pos.CENTER_LEFT);

                    Label expLbl = new Label(anomaly.getExplanation());
                    expLbl.setWrapText(true);
                    expLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
                    HBox.setHgrow(expLbl, Priority.ALWAYS);

                    Button inspectBtn = new Button("🔍 Inspect Records");
                    inspectBtn.getStyleClass().addAll("btn", "btn-secondary");
                    inspectBtn.setOnAction(e -> showTransactionInspectorDialog(
                            "Supporting Evidence — " + anomaly.getCategory() + " Anomaly",
                            anomaly.getExplanation(),
                            anomaly.getSupportingTransactions()
                    ));

                    bottom.getChildren().addAll(expLbl, inspectBtn);

                    card.getChildren().addAll(top, metricsRow, bottom);
                    anomaliesBox.getChildren().add(card);
                }
            }
        }
    }

    private void showTransactionInspectorDialog(String title, String subtitle, List<Expense> transactions) {
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F8FAFC;");
        root.setPrefWidth(680);
        root.setPrefHeight(450);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        subLbl.setWrapText(true);

        TableView<Expense> table = new TableView<>();
        table.getStyleClass().add("table-view");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Expense, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedDate"));
        dateCol.setPrefWidth(100);

        TableColumn<Expense, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("category"));
        catCol.setPrefWidth(130);

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("description"));
        descCol.setPrefWidth(240);

        TableColumn<Expense, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("formattedAmount"));
        amtCol.setPrefWidth(110);
        amtCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        table.getColumns().addAll(idCol, dateCol, catCol, descCol, amtCol);
        if (transactions != null) {
            table.setItems(FXCollections.observableArrayList(transactions));
        }

        BigDecimal total = transactions != null
                ? transactions.stream().map(Expense::getAmountBigDecimal).reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label totalLbl = new Label("Records: " + (transactions != null ? transactions.size() : 0)
                + " | Total Sum: " + MoneyUtil.formatRupees(total));
        totalLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("btn", "btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());

        footer.getChildren().addAll(totalLbl, spacer, closeBtn);

        root.getChildren().addAll(titleLbl, subLbl, table, footer);

        Scene scene = new Scene(root);
        try {
            var css = getClass().getResource("/styles/app.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // =========================================================================
    // Analytics Visual Rendering & Tooltip Binding
    // =========================================================================

    private VBox createMetricChip(String title, String val, String color) {
        VBox chip = new VBox(2);
        chip.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-padding: 8 12;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        chip.getChildren().addAll(t, v);
        return chip;
    }

    private VBox createEmptyStateLabel(String text) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: rgba(248, 250, 252, 0.85); -fx-background-radius: 8; -fx-padding: 24;");
        Label icon = new Label("📊");
        icon.setStyle("-fx-font-size: 24px;");
        Label msg = new Label(text);
        msg.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-alignment: CENTER; -fx-text-alignment: CENTER;");
        box.getChildren().addAll(icon, msg);
        return box;
    }

    private void renderDashboardCharts() {
        if (dashboardPieChart != null) {
            Map<String, BigDecimal> categoryMap = new HashMap<>();
            BigDecimal totalSpend = BigDecimal.ZERO;
            for (Expense exp : masterExpenseList) {
                if (exp == null || exp.getAmount() <= 0) continue;
                String cat = exp.getCategory() != null && !exp.getCategory().isBlank() ? exp.getCategory() : "Other";
                BigDecimal amt = exp.getAmountBigDecimal();
                categoryMap.put(cat, categoryMap.getOrDefault(cat, BigDecimal.ZERO).add(amt));
                totalSpend = totalSpend.add(amt);
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, BigDecimal> entry : categoryMap.entrySet()) {
                double pct = totalSpend.compareTo(BigDecimal.ZERO) > 0
                        ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalSpend, 1, java.math.RoundingMode.HALF_UP).doubleValue()
                        : 0.0;
                String label = String.format("%s (%.1f%%)", entry.getKey(), pct);
                PieChart.Data slice = new PieChart.Data(label, entry.getValue().doubleValue());
                pieData.add(slice);
            }
            dashboardPieChart.setData(pieData);
            if (dashboardPieEmptyState != null) {
                boolean empty = pieData.isEmpty();
                dashboardPieEmptyState.setVisible(empty);
                dashboardPieEmptyState.setManaged(empty);
                dashboardPieChart.setVisible(!empty);
                dashboardPieChart.setManaged(!empty);
            }

            for (PieChart.Data slice : pieData) {
                installPieSliceTooltip(slice, String.format("%s: %s", slice.getName(), MoneyUtil.formatRupees(BigDecimal.valueOf(slice.getPieValue()))));
            }
        }

        if (dashboardMonthlyBarChart != null) {
            TreeMap<YearMonth, BigDecimal> monthMap = new TreeMap<>();
            for (Expense exp : masterExpenseList) {
                if (exp == null || exp.getDate() == null || exp.getAmount() <= 0) continue;
                YearMonth ym = YearMonth.from(exp.getDate());
                monthMap.put(ym, monthMap.getOrDefault(ym, BigDecimal.ZERO).add(exp.getAmountBigDecimal()));
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Monthly Spending");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
            for (Map.Entry<YearMonth, BigDecimal> entry : monthMap.entrySet()) {
                String label = entry.getKey().format(fmt);
                XYChart.Data<String, Number> data = new XYChart.Data<>(label, entry.getValue().doubleValue());
                installDataTooltip(data, String.format("%s: %s", label, MoneyUtil.formatRupees(entry.getValue())));
                series.getData().add(data);
            }

            dashboardMonthlyBarChart.getData().setAll(series);
            if (dashboardMonthlyEmptyState != null) {
                boolean empty = series.getData().isEmpty();
                dashboardMonthlyEmptyState.setVisible(empty);
                dashboardMonthlyEmptyState.setManaged(empty);
                dashboardMonthlyBarChart.setVisible(!empty);
                dashboardMonthlyBarChart.setManaged(!empty);
            }
        }
    }

    private void installPieSliceTooltip(PieChart.Data slice, String tooltipText) {
        if (slice == null) return;
        Consumer<Node> attacher = node -> {
            if (node != null) {
                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #0F172A; -fx-text-fill: #F8FAFC; -fx-padding: 8 12; -fx-background-radius: 6;");
                tooltip.setShowDelay(javafx.util.Duration.millis(50));
                Tooltip.install(node, tooltip);
                node.setStyle("-fx-cursor: hand;");
            }
        };

        if (slice.getNode() != null) {
            attacher.accept(slice.getNode());
        } else {
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    attacher.accept(newNode);
                }
            });
        }
    }

    private void renderSpendingLineChart() {
        if (spendingLineChart == null) return;

        TimeRange range = lineChartRangeCombo != null && lineChartRangeCombo.getValue() != null
                ? lineChartRangeCombo.getValue()
                : TimeRange.LAST_30_DAYS;

        List<TimeSeriesPoint> points = expenseService.getSpendingOverTime(masterExpenseList, range);

        BigDecimal total = MoneyUtil.ZERO;
        BigDecimal peakAmt = MoneyUtil.ZERO;
        String peakLabel = "None";
        int activePeriods = 0;
        int totalTx = 0;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(range.getAggregationLabel());

        for (TimeSeriesPoint p : points) {
            BigDecimal amt = p.getAmount();
            total = total.add(amt);
            totalTx += p.getTransactionCount();
            if (amt.compareTo(BigDecimal.ZERO) > 0) {
                activePeriods++;
            }
            if (amt.compareTo(peakAmt) > 0) {
                peakAmt = amt;
                peakLabel = p.getLabel() + " (" + MoneyUtil.formatRupees(amt) + ")";
            }

            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getLabel(), p.getAmountDouble());
            installDataTooltip(data, p.getTooltipText());
            series.getData().add(data);
        }

        if (analyticsPeriodTotalLbl != null) {
            analyticsPeriodTotalLbl.setText(MoneyUtil.formatRupees(total));
        }
        if (analyticsPeriodAvgLbl != null) {
            int denom = Math.max(1, points.size());
            BigDecimal avg = total.divide(BigDecimal.valueOf(denom), 2, java.math.RoundingMode.HALF_UP);
            analyticsPeriodAvgLbl.setText(MoneyUtil.formatRupees(avg) + (range.isDaily() ? " / day" : " / month"));
        }
        if (analyticsPeriodPeakLbl != null) {
            analyticsPeriodPeakLbl.setText(peakLabel);
        }
        if (analyticsPeriodCountLbl != null) {
            analyticsPeriodCountLbl.setText(totalTx + " Records (" + activePeriods + " active)");
        }

        if (lineChartSubtitle != null) {
            String desc = range.isDaily()
                    ? String.format("Daily expenditure trajectory (%d consecutive days evaluated)", points.size())
                    : String.format("Monthly expenditure trajectory (%d consecutive months evaluated)", points.size());
            lineChartSubtitle.setText(desc);
        }

        if (lineChartEmptyState != null) {
            lineChartEmptyState.setVisible(total.compareTo(BigDecimal.ZERO) <= 0);
        }

        spendingLineChart.getData().setAll(series);
    }

    private void renderCategoryBarChart() {
        if (categoryBarChart == null) return;

        List<CategorySpendingPoint> points = expenseService.getCategorySpendingRanked(masterExpenseList);

        if (points.isEmpty()) {
            if (categoryBarEmptyState != null) categoryBarEmptyState.setVisible(true);
            categoryBarChart.getData().clear();
            if (categoryBarSubtitle != null) categoryBarSubtitle.setText("No category expenses found");
            return;
        }

        if (categoryBarEmptyState != null) categoryBarEmptyState.setVisible(false);
        if (categoryBarSubtitle != null) {
            categoryBarSubtitle.setText(String.format("%d categories tracked (Ranked by expenditure)", points.size()));
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Category Spending");

        for (CategorySpendingPoint p : points) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(p.getCategory(), p.getAmountDouble());
            installDataTooltip(data, p.getTooltipText());
            series.getData().add(data);
        }

        categoryBarChart.getData().setAll(series);
    }

    private void renderMoMBarChart() {
        if (momBarChart == null) return;

        boolean normalizeMtd = momNormalizeToggle != null && momNormalizeToggle.isSelected();
        MoMComparisonResult result = expenseService.getMoMComparison(masterExpenseList, normalizeMtd);

        if (!result.isSufficientHistory()) {
            if (momEmptyState != null) momEmptyState.setVisible(true);
            if (momSubtitle != null) momSubtitle.setText("Insufficient comparison history");
            if (momSummaryFootnote != null) momSummaryFootnote.setText(result.getStatusMessage());
            momBarChart.getData().clear();
            return;
        }

        if (momEmptyState != null) momEmptyState.setVisible(false);
        if (momSubtitle != null) momSubtitle.setText(result.getComparisonSummary());
        if (momSummaryFootnote != null) {
            momSummaryFootnote.setText(result.isDayNormalized()
                    ? String.format("⚖️ Day-Normalized Comparison: Both months evaluated up to Day %d to ensure fair trajectory without partial-month skew.", result.getCurrentDaysEvaluated())
                    : "📅 Full Calendar Month Comparison: Entire month totals evaluated.");
        }

        XYChart.Series<String, Number> prevSeries = new XYChart.Series<>();
        prevSeries.setName(result.getPreviousSeriesName());

        XYChart.Series<String, Number> curSeries = new XYChart.Series<>();
        curSeries.setName(result.getCurrentSeriesName());

        for (CategoryMoMPoint p : result.getCategoryPoints()) {
            XYChart.Data<String, Number> prevData = new XYChart.Data<>(p.getCategory(), p.getPreviousAmountDouble());
            installDataTooltip(prevData, p.getPreviousTooltipText());
            prevSeries.getData().add(prevData);

            XYChart.Data<String, Number> curData = new XYChart.Data<>(p.getCategory(), p.getCurrentAmountDouble());
            installDataTooltip(curData, p.getCurrentTooltipText());
            curSeries.getData().add(curData);
        }

        momBarChart.getData().setAll(prevSeries, curSeries);
    }

    private void installDataTooltip(XYChart.Data<String, Number> data, String tooltipText) {
        if (data == null) return;
        Consumer<Node> attacher = node -> {
            if (node != null) {
                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #0F172A; -fx-text-fill: #F8FAFC; -fx-padding: 8 12; -fx-background-radius: 6;");
                tooltip.setShowDelay(javafx.util.Duration.millis(50));
                Tooltip.install(node, tooltip);
                node.setStyle("-fx-cursor: hand;");
            }
        };

        if (data.getNode() != null) {
            attacher.accept(data.getNode());
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    attacher.accept(newNode);
                }
            });
        }
    }
}