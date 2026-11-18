package com.eduportal.ui;

import com.eduportal.dao.CourseDAO;
import com.eduportal.dao.MessageDAO;
import com.eduportal.dao.NewsTickerDAO;
import com.eduportal.dao.UserDAO;
import com.eduportal.models.NewsTicker;
import com.eduportal.models.User;
import com.eduportal.utils.SessionManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MainLayout extends BorderPane {

    private static final List<MainLayout> activeInstances = new ArrayList<>();

    private final MainApp mainApp;
    private final MessageDAO messageDAO = new MessageDAO();
    private final NewsTickerDAO tickerDAO = new NewsTickerDAO();
    private final UserDAO userDAO = new UserDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    private VBox sidebarNav;
    private ScrollPane centerScrollPane;
    private HBox tickerBar;
    private Button msgBtn;

    // High-performance smooth animation timer state
    private AnimationTimer tickerAnimationTimer;
    private ScheduledExecutorService tickerExecutor;
    private HBox tickerTrack;
    private double trackTranslateX = 0.0;
    private double singleSegmentWidth = 0.0;
    private long lastFrameTimeNano = 0;
    private List<String> currentTickerCache = new ArrayList<>();

    public MainLayout(MainApp mainApp) {
        this.mainApp = mainApp;
        activeInstances.add(this);
        initUI();
    }

    public static void triggerTickerSync() {
        for (MainLayout instance : new ArrayList<>(activeInstances)) {
            instance.pollTickerInBackground();
        }
    }

    private void initUI() {
        this.getStyleClass().add("background-pane");

        // 1. Top News Ticker Bar (Hardware-Accelerated Infinite Smooth Marquee)
        tickerBar = createTickerBar();

        // 2. Top Header Bar (EduPortal Brand + Messages)
        HBox topBar = createTopBar();

        VBox topContainer = new VBox(tickerBar, topBar);
        this.setTop(topContainer);

        // 3. Left Navigation Sidebar
        VBox sidebar = createSidebar();
        this.setLeft(sidebar);

        // 4. Center Content View wrapped in responsive ScrollPane
        centerScrollPane = new ScrollPane();
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(false);
        centerScrollPane.getStyleClass().add("scroll-pane");

        this.setCenter(centerScrollPane);

        // Load Default Dashboard View
        showDashboard();

        // Start background executor for 5-second polling (Zero UI thread blocking)
        startTickerPolling();
    }

    private HBox createTickerBar() {
        HBox bar = new HBox(0);
        bar.getStyleClass().add("news-ticker-pane");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(36);
        bar.setMinHeight(36);
        bar.setMaxHeight(36);

        // Ticker Title Badge
        HBox badgeBox = new HBox(8);
        badgeBox.setAlignment(Pos.CENTER);
        badgeBox.setPadding(new Insets(0, 16, 0, 16));
        badgeBox.setStyle("-fx-background-color: #27272A; -fx-border-color: rgba(250, 250, 250, 0.12); -fx-border-width: 0 1px 0 0;");
        Label label = new Label("📢 NEWS TICKER");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: -fx-base-primary; -fx-letter-spacing: 0.5px;");
        badgeBox.getChildren().add(label);

        // Clip pane and ticker track
        tickerTrack = new HBox(0);
        tickerTrack.setAlignment(Pos.CENTER_LEFT);

        Pane clipPane = new Pane(tickerTrack);
        HBox.setHgrow(clipPane, Priority.ALWAYS);

        Rectangle clipRect = new Rectangle();
        clipRect.setHeight(36);
        clipPane.setClip(clipRect);

        clipPane.widthProperty().addListener((obs, oldV, newV) -> {
            clipRect.setWidth(newV.doubleValue());
        });

        rebuildTickerTrack(fetchActiveTickerStrings());

        // High precision 60fps/144fps vsync-aligned animation timer (GPU transform-based setTranslateX)
        final double pixelsPerSecond = 55.0; // Smooth readable marquee speed

        tickerAnimationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameTimeNano == 0) {
                    lastFrameTimeNano = now;
                    return;
                }
                double deltaSeconds = (now - lastFrameTimeNano) / 1_000_000_000.0;
                lastFrameTimeNano = now;

                if (deltaSeconds > 0.1) {
                    deltaSeconds = 0.016; // Clamp large frame drops
                }

                if (singleSegmentWidth > 0) {
                    trackTranslateX -= pixelsPerSecond * deltaSeconds;
                    if (trackTranslateX <= -singleSegmentWidth) {
                        trackTranslateX += singleSegmentWidth;
                    }
                    tickerTrack.setTranslateX(trackTranslateX);
                }
            }
        };
        tickerAnimationTimer.start();

        bar.getChildren().addAll(badgeBox, clipPane);
        return bar;
    }

    private List<String> fetchActiveTickerStrings() {
        User u = SessionManager.getCurrentUser();
        String role = u != null ? u.getRole() : "student";
        List<NewsTicker> tickers = tickerDAO.getActiveTickersForRole(role);

        List<String> items = new ArrayList<>();
        if (tickers.isEmpty()) {
            items.add("Welcome to EduPortal Student Management System!");
        } else {
            for (NewsTicker t : tickers) {
                items.add(t.getContent());
            }
        }
        return items;
    }

    private double calculateItemsWidth(List<String> items) {
        double width = 0;
        Font font = Font.font("Segoe UI", 13);
        for (String text : items) {
            Text t = new Text(text);
            t.setFont(font);
            width += t.getLayoutBounds().getWidth() + 69.0;
        }
        return Math.max(width, 300.0);
    }

    private void rebuildTickerTrack(List<String> items) {
        currentTickerCache = new ArrayList<>(items);
        singleSegmentWidth = calculateItemsWidth(items);

        HBox seg1 = createSegmentHBox(items);
        HBox seg2 = createSegmentHBox(items);
        HBox seg3 = createSegmentHBox(items);

        tickerTrack.getChildren().setAll(seg1, seg2, seg3);
    }

    private HBox createSegmentHBox(List<String> items) {
        HBox seg = new HBox(0);
        seg.setAlignment(Pos.CENTER_LEFT);
        for (String text : items) {
            HBox itemBox = new HBox(8);
            itemBox.setAlignment(Pos.CENTER_LEFT);
            itemBox.setPadding(new Insets(0, 25, 0, 25));

            Label bullet = new Label("◆");
            bullet.setStyle("-fx-text-fill: #F97316; -fx-font-size: 11px;");

            Label msg = new Label(text);
            msg.setStyle("-fx-text-fill: #FAFAFA; -fx-font-size: 13px; -fx-font-weight: 500;");

            itemBox.getChildren().addAll(bullet, msg);
            seg.getChildren().add(itemBox);
        }
        return seg;
    }

    private void pollTickerInBackground() {
        new Thread(() -> {
            try {
                List<String> items = fetchActiveTickerStrings();
                if (!items.equals(currentTickerCache)) {
                    Platform.runLater(() -> rebuildTickerTrack(items));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void startTickerPolling() {
        tickerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Ticker-Poll-Thread");
            t.setDaemon(true);
            return t;
        });

        tickerExecutor.scheduleAtFixedRate(() -> {
            try {
                List<String> items = fetchActiveTickerStrings();
                if (!items.equals(currentTickerCache)) {
                    Platform.runLater(() -> rebuildTickerTrack(items));
                }
            } catch (Exception ignored) {}
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void refreshTickerBar() {
        pollTickerInBackground();
    }

    private HBox createTopBar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(12, 24, 12, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: -fx-card-bg; -fx-border-color: -fx-border-color-glass; -fx-border-width: 0 0 1px 0;");

        // EduPortal Brand Logo
        Label brandLabel = new Label("🎓 EduPortal");
        brandLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -fx-base-primary;");

        Label tagline = new Label("Student Management System");
        tagline.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-muted;");

        VBox brandBox = new VBox(2, brandLabel, tagline);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Messages Button with unread count
        User currentUser = SessionManager.getCurrentUser();
        int unread = currentUser != null ? messageDAO.getUnreadCount(currentUser.getId()) : 0;
        msgBtn = new Button(unread > 0 ? "✉ Messages (" + unread + ")" : "✉ Messages");
        msgBtn.getStyleClass().add("btn-ghost");
        msgBtn.setOnAction(e -> navigateTo(() -> new MessagesView(this)));

        bar.getChildren().addAll(brandBox, spacer, msgBtn);
        return bar;
    }

    /** Refresh the message counter on the top bar */
    public void refreshMessageCount() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || msgBtn == null) return;
        int unread = messageDAO.getUnreadCount(currentUser.getId());
        if (unread > 0) {
            msgBtn.setText("✉ Messages (" + unread + ")");
        } else {
            msgBtn.setText("✉ Messages");
        }
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.getStyleClass().add("sidebar-pane");
        sidebar.setPadding(new Insets(20, 15, 20, 15));

        sidebarNav = new VBox(6);

        User u = SessionManager.getCurrentUser();
        String role = u != null ? u.getRole() : "student";

        Button dashBtn = createSidebarBtn("📊 Dashboard", () -> showDashboard());
        sidebarNav.getChildren().add(dashBtn);

        if ("student".equalsIgnoreCase(role)) {
            sidebarNav.getChildren().add(createSidebarBtn("📚 Take Course", () -> navigateTo(() -> new StudentDashboardView("take_course"))));
            sidebarNav.getChildren().add(createSidebarBtn("📑 Grade Report", () -> navigateTo(() -> new StudentDashboardView("grade_report"))));
            sidebarNav.getChildren().add(createSidebarBtn("📅 Attendance", () -> navigateTo(AttendanceView::new)));
        } else if ("teacher".equalsIgnoreCase(role)) {
            sidebarNav.getChildren().add(createSidebarBtn("📚 Apply for Course", () -> navigateTo(() -> new TeacherDashboardView("take_course"))));
            sidebarNav.getChildren().add(createSidebarBtn("📝 Enter Marks", () -> navigateTo(() -> new TeacherDashboardView("enter_marks"))));
            sidebarNav.getChildren().add(createSidebarBtn("📅 Attendance", () -> navigateTo(AttendanceView::new)));
        } else if ("admin".equalsIgnoreCase(role)) {
            sidebarNav.getChildren().add(createSidebarBtn("👥 Manage Users", () -> navigateTo(() -> new AdminDashboardView(this, "manage_users"))));
            sidebarNav.getChildren().add(createSidebarBtn("📖 Manage Courses", () -> navigateTo(() -> new AdminDashboardView(this, "courses"))));
            sidebarNav.getChildren().add(createSidebarBtn("✅ Approve Results", () -> navigateTo(() -> new AdminDashboardView(this, "results"))));
            sidebarNav.getChildren().add(createSidebarBtn("📅 Attendance", () -> navigateTo(() -> new AdminDashboardView(this, "attendance"))));
            sidebarNav.getChildren().add(createSidebarBtn("📅 Set Deadlines", () -> navigateTo(() -> new AdminDashboardView(this, "deadlines"))));
            sidebarNav.getChildren().add(createSidebarBtn("📢 News Tickers", () -> navigateTo(() -> new AdminDashboardView(this, "tickers"))));
        }

        sidebarNav.getChildren().add(createSidebarBtn("✉ Messages", () -> navigateTo(() -> new MessagesView(this))));
        sidebarNav.getChildren().add(createSidebarBtn("👤 My Profile", () -> navigateTo(() -> new ProfileView(this))));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User Profile & Sign Out
        VBox userBox = new VBox(8);
        userBox.setPadding(new Insets(12));
        userBox.setStyle("-fx-background-color: -fx-badge-bg; -fx-background-radius: 12px;");

        Label uName = new Label(u != null ? u.getName() : "User");
        uName.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-main; -fx-font-size: 13px;");

        Label uRole = new Label(u != null ? u.getRole().toUpperCase() + " • " + u.getId() : "");
        uRole.getStyleClass().add("text-muted");
        uRole.setStyle("-fx-font-size: 11px;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            activeInstances.remove(this);
            if (tickerAnimationTimer != null) tickerAnimationTimer.stop();
            if (tickerExecutor != null) tickerExecutor.shutdownNow();
            SessionManager.logout();
            mainApp.showLoginView();
        });

        userBox.getChildren().addAll(uName, uRole, logoutBtn);
        sidebar.getChildren().addAll(sidebarNav, spacer, userBox);
        return sidebar;
    }

    private Button createSidebarBtn(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            for (Node n : sidebarNav.getChildren()) {
                n.getStyleClass().remove("sidebar-button-active");
            }
            btn.getStyleClass().add("sidebar-button-active");
            action.run();
        });
        return btn;
    }

    public void showDashboard() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        if ("admin".equals(u.getRole())) {
            navigateTo(() -> new AdminDashboardView(this, "overview"));
        } else if ("teacher".equals(u.getRole())) {
            navigateTo(() -> new TeacherDashboardView("overview"));
        } else {
            navigateTo(() -> new StudentDashboardView("overview"));
        }
    }

    public void navigateTo(Supplier<Node> viewSupplier) {
        Node view = viewSupplier.get();
        if ("student".equalsIgnoreCase(
                SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : "")) {
            Platform.runLater(() -> setView(view));
        } else {
            setView(view);
        }
    }

    public void setView(Node node) {
        VBox pageContent = new VBox(node);
        pageContent.setPadding(new Insets(20));
        pageContent.setFillWidth(true);
        centerScrollPane.setContent(pageContent);
        pageContent.applyCss();
        pageContent.requestLayout();
        centerScrollPane.requestLayout();
        centerScrollPane.setVvalue(0);
    }
}
