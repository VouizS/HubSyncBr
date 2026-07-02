package br.com.hubsyncbr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 10, 18);
    private static final int PANEL = Color.rgb(16, 20, 32);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(175, 180, 200);
    private static final int PURPLE = Color.rgb(139, 92, 246);
    private static final int BLUE = Color.rgb(63, 151, 255);
    private static final int ICON_NORMAL = Color.rgb(167, 139, 250);
    private static final int ICON_ACTIVE = Color.rgb(196, 181, 253);
    private static final int ICON_BLUE = Color.rgb(56, 189, 248);
    private static final int ICON_DISABLED = Color.rgb(107, 114, 128);

    private static final int MAX_OPEN_WINDOWS = 8;
    private static final int MAX_VISIBLE_WINDOWS = 4;

    private LinearLayout root;
    private LinearLayout shell;
    private LinearLayout sidebar;
    private LinearLayout mainArea;
    private LinearLayout topBar;
    private LinearLayout windowContainer;
    private LinearLayout emptyState;
    private TextView screenStatusTitle;
    private TextView screenStatusDesc;
    private TextView headerTitle;
    private TextView headerDesc;

    private final List<StreamPane> panes = new ArrayList<>();
    private final List<StreamPane> visiblePanes = new ArrayList<>();

    private boolean verticalSplit = false;
    private boolean focusMode = false;
    private boolean sidebarCollapsed = false;
    private int layoutMode = 0; // 0 = igual, 1 = primeira maior, 2 = segunda maior, 3 = grade 2x2
    private int nextPaneNumber = 1;
    private int nextGroupNumber = 1;
    private boolean groupOverviewMode = false;

    private final List<WindowGroup> groups = new ArrayList<>();
    private WindowGroup activeGroup;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private FrameLayout fullscreenHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(false);
        configureWindow();
        buildUi();
        showLegalNoticeOnce();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildUi() {
        fullscreenHost = new FrameLayout(this);
        fullscreenHost.setBackgroundColor(Color.BLACK);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(shell, new LinearLayout.LayoutParams(-1, 0, 1));

        sidebar = createSidebar();
        shell.addView(sidebar, new LinearLayout.LayoutParams(dp(200), -1));

        mainArea = new LinearLayout(this);
        mainArea.setOrientation(LinearLayout.VERTICAL);
        mainArea.setPadding(dp(6), 0, 0, 0);
        shell.addView(mainArea, new LinearLayout.LayoutParams(0, -1, 1));

        topBar = createTopBar();
        mainArea.addView(topBar, new LinearLayout.LayoutParams(-1, dp(48)));

        windowContainer = new LinearLayout(this);
        windowContainer.setOrientation(LinearLayout.HORIZONTAL);
        mainArea.addView(windowContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        emptyState = createEmptyState();

        activeGroup = createGroup("Meu Hub");
        StreamPane a = createPane("Hub Home", getHomepageUrl(), PURPLE);
        a.loadDefault();
        showPane(a);

        // Em telas estreitas/retrato, o app começa focado no núcleo.
        if (getResources().getConfiguration().screenWidthDp < 760) {
            sidebarCollapsed = true;
            sidebar.setVisibility(View.GONE);
            mainArea.setPadding(0, 0, 0, 0);
        }

        updateWindowLayout();

        FrameLayout outer = new FrameLayout(this);
        outer.addView(root, new FrameLayout.LayoutParams(-1, -1));
        outer.addView(fullscreenHost, new FrameLayout.LayoutParams(-1, -1));
        fullscreenHost.setVisibility(View.GONE);
        setContentView(outer);
    }

    private LinearLayout createSidebar() {
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(dp(10), dp(8), dp(10), dp(8));
        side.setBackground(cardBg(Color.rgb(9, 13, 24), Color.rgb(26, 31, 48), dp(24), 1));

        TextView logo = label("HubSyncBr", 20, TEXT, true);
        setLeftIcon(logo, R.drawable.ic_hs_sync, ICON_ACTIVE, 30);
        logo.setPadding(dp(8), dp(10), dp(8), dp(4));
        side.addView(logo, new LinearLayout.LayoutParams(-1, dp(58)));

        TextView sub = label("Watch more. Sync more.", 11, MUTED, false);
        sub.setPadding(dp(8), 0, dp(8), dp(10));
        side.addView(sub, new LinearLayout.LayoutParams(-1, dp(32)));

        side.addView(navButton("Home", R.drawable.ic_hs_home, true, v -> showGroupsOverview()));
        side.addView(navButton("Multi Screen", R.drawable.ic_hs_grid, false, v -> { if (focusMode) exitFocus(); enterGroup(activeGroup); }));
        side.addView(navButton("Favorites", R.drawable.ic_hs_heart, false, v -> Toast.makeText(this, "Favoritos entram depois", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Sports", R.drawable.ic_hs_sports, false, v -> Toast.makeText(this, "Modo esportes entra depois", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Browser", R.drawable.ic_hs_browser, false, v -> { if (groupOverviewMode) enterGroup(activeGroup); addWindow(); }));
        side.addView(navButton("Settings", R.drawable.ic_hs_settings, false, v -> showAboutDialog()));

        View flex = new View(this);
        side.addView(flex, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(8), dp(12), dp(8), dp(12));
        status.setBackground(cardBg(Color.rgb(18, 24, 39), Color.rgb(28, 35, 55), dp(16), 1));
        TextView icon = label("", 28, BLUE, true);
        setCenterIcon(icon, R.drawable.ic_hs_grid, ICON_BLUE, 34);
        icon.setGravity(Gravity.CENTER);
        screenStatusTitle = label("Hub View  •", 14, TEXT, true);
        screenStatusTitle.setGravity(Gravity.CENTER);
        screenStatusDesc = label("2 visíveis / 2 abertas", 12, MUTED, false);
        screenStatusDesc.setGravity(Gravity.CENTER);
        status.addView(icon, new LinearLayout.LayoutParams(-1, dp(36)));
        status.addView(screenStatusTitle, new LinearLayout.LayoutParams(-1, dp(28)));
        status.addView(screenStatusDesc, new LinearLayout.LayoutParams(-1, dp(24)));
        side.addView(status, new LinearLayout.LayoutParams(-1, dp(128)));
        return side;
    }

    private TextView navButton(String text, int iconRes, boolean active, View.OnClickListener listener) {
        TextView b = label(text, 16, TEXT, true);
        setLeftIcon(b, iconRes, active ? ICON_ACTIVE : ICON_NORMAL, 21);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(18), 0, dp(12), 0);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                active ? new int[]{Color.rgb(55, 42, 96), Color.rgb(86, 118, 255)} : new int[]{Color.rgb(17, 22, 35), Color.rgb(17, 22, 35)}
        );
        bg.setCornerRadius(dp(14));
        bg.setStroke(1, active ? Color.rgb(110, 90, 255) : Color.rgb(31, 38, 57));
        b.setBackground(bg);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(62));
        params.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(params);
        return b;
    }

    private LinearLayout createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, 0, 0, dp(4));

        TextView menu = topIcon(R.drawable.ic_hs_menu, ICON_NORMAL, "Abrir/recolher menu", v -> toggleSidebar());
        bar.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(40)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(6), 0, 0, 0);
        headerTitle = label("Hub View", 18, TEXT, true);
        setLeftIcon(headerTitle, R.drawable.ic_hs_sync, ICON_BLUE, 19);
        headerDesc = label("Workspace de janelas web", 11, MUTED, false);
        titles.addView(headerTitle, new LinearLayout.LayoutParams(-1, dp(24)));
        titles.addView(headerDesc, new LinearLayout.LayoutParams(-1, dp(18)));
        bar.addView(titles, new LinearLayout.LayoutParams(0, -1, 1));

        TextView add = topIcon(R.drawable.ic_hs_plus, ICON_ACTIVE, "Nova janela", v -> addWindow());
        bar.addView(add, topIconParams());

        TextView swap = topIcon(R.drawable.ic_hs_swap, ICON_ACTIVE, "Trocar janelas", v -> swapFirstTwoVisible());
        bar.addView(swap, topIconParams());

        TextView size = topIcon(R.drawable.ic_hs_resize, ICON_NORMAL, "Trocar layout", v -> cycleLayoutMode());
        bar.addView(size, topIconParams());

        TextView manager = topIcon(R.drawable.ic_hs_layout, ICON_NORMAL, groupOverviewMode ? "Gerenciar janelas" : "Ver grupos", v -> { if (groupOverviewMode) showWindowManager(); else showGroupsOverview(); });
        bar.addView(manager, topIconParams());

        TextView more = topIcon(R.drawable.ic_hs_more, ICON_NORMAL, "Mais opções", v -> showMorePanel());
        bar.addView(more, topIconParams());
        return bar;
    }

    private LinearLayout.LayoutParams topIconParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(40));
        lp.setMargins(dp(8), 0, 0, 0);
        return lp;
    }

    private TextView topIcon(int iconRes, int color, String tip, View.OnClickListener listener) {
        TextView b = label("", 18, color, true);
        b.setGravity(Gravity.CENTER);
        setCenterIcon(b, iconRes, color, 22);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(cardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(12), 0));
        b.setOnClickListener(listener);
        attachTip(b, tip);
        return b;
    }

    private LinearLayout createEmptyState() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackground(cardBg(Color.rgb(10, 13, 23), Color.rgb(45, 56, 86), dp(18), 1));

        TextView plus = label("", 48, BLUE, true);
        setCenterIcon(plus, R.drawable.ic_hs_plus, ICON_BLUE, 52);
        plus.setGravity(Gravity.CENTER);
        TextView title = label("Adicionar janela", 22, TEXT, true);
        title.setGravity(Gravity.CENTER);
        TextView desc = label("Abra um site, use como navegador ou combine com outra transmissão.", 13, MUTED, false);
        desc.setGravity(Gravity.CENTER);
        Button add = chip("Nova janela", PURPLE);
        setButtonIcon(add, R.drawable.ic_hs_plus, ICON_ACTIVE, 18);
        add.setOnClickListener(v -> addWindow());

        box.addView(plus, new LinearLayout.LayoutParams(-1, dp(62)));
        box.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        box.addView(desc, new LinearLayout.LayoutParams(-1, dp(36)));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(178), dp(44));
        alp.setMargins(0, dp(18), 0, 0);
        box.addView(add, alp);
        return box;
    }


    private WindowGroup createGroup(String name) {
        WindowGroup group = new WindowGroup(nextGroupNumber++, name == null || name.trim().isEmpty() ? "Grupo" : name.trim(), accentForIndex(groups.size()));
        groups.add(group);
        if (activeGroup == null) activeGroup = group;
        return group;
    }

    private void showGroupsOverview() {
        if (focusMode) exitFocus();
        groupOverviewMode = true;
        windowContainer.removeAllViews();
        detach(emptyState);
        for (StreamPane pane : panes) detach(pane.container);
        windowContainer.setOrientation(LinearLayout.VERTICAL);
        windowContainer.addView(createGroupsOverviewView(), new LinearLayout.LayoutParams(-1, -1));
        updateStatusText();
    }

    private View createGroupsOverviewView() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(4), dp(4), dp(4), dp(4));
        panel.setBackground(cardBg(Color.rgb(10, 13, 23), Color.rgb(45, 56, 86), dp(18), 1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("Grupos do Hub", 22, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_layout, ICON_BLUE, 24);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        TextView add = topIcon(R.drawable.ic_hs_plus, ICON_ACTIVE, "Novo grupo", v -> createAndEnterGroup());
        header.addView(add, new LinearLayout.LayoutParams(dp(48), dp(44)));
        panel.addView(header, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView desc = label(groups.size() + " grupos • entre em um grupo para abrir o workspace de janelas", 13, MUTED, false);
        desc.setPadding(dp(12), 0, dp(12), dp(6));
        panel.addView(desc, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        panel.addView(rows, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout currentRow = null;
        for (int i = 0; i < groups.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                rows.addView(currentRow, new LinearLayout.LayoutParams(-1, dp(180)));
            }
            WindowGroup group = groups.get(i);
            LinearLayout card = groupCard(group);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(168), 1);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            currentRow.addView(card, lp);
        }

        LinearLayout addCard = createAddGroupCard();
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, dp(112));
        alp.setMargins(dp(6), dp(10), dp(6), dp(6));
        panel.addView(addCard, alp);

        scroll.addView(panel);
        return scroll;
    }

    private LinearLayout groupCard(WindowGroup group) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(cardBg(Color.rgb(14, 18, 30), group.accent, dp(18), 1));
        card.setOnClickListener(v -> enterGroup(group));
        attachTip(card, "Entrar no grupo " + group.name);

        TextView title = label(group.name, 18, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_grid, ICON_BLUE, 22);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(32)));

        TextView info = label(group.panes.size() + " janelas • " + group.visible.size() + " visíveis", 12, MUTED, false);
        card.addView(info, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(4), dp(4), dp(4), dp(4));
        preview.setBackground(cardBg(Color.rgb(8, 11, 20), Color.rgb(31, 38, 57), dp(14), 1));
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        preview.addView(r1, new LinearLayout.LayoutParams(-1, 0, 1));
        preview.addView(r2, new LinearLayout.LayoutParams(-1, 0, 1));
        for (int i = 0; i < 4; i++) {
            TextView mini = label(i < group.panes.size() ? String.valueOf(i + 1) : "", 12, i < group.panes.size() ? TEXT : MUTED, true);
            mini.setGravity(Gravity.CENTER);
            mini.setBackground(cardBg(i < group.panes.size() ? Color.rgb(20, 26, 42) : Color.rgb(10, 13, 23), i < group.panes.size() ? group.accent : Color.rgb(28, 35, 55), dp(10), 1));
            LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, -1, 1);
            mlp.setMargins(dp(3), dp(3), dp(3), dp(3));
            if (i < 2) r1.addView(mini, mlp); else r2.addView(mini, mlp);
        }
        card.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        return card;
    }

    private LinearLayout createAddGroupCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(12), dp(18), dp(12));
        card.setBackground(cardBg(Color.rgb(12, 16, 28), Color.rgb(52, 62, 90), dp(18), 1));
        card.setOnClickListener(v -> createAndEnterGroup());
        TextView icon = label("", 24, ICON_BLUE, true);
        setCenterIcon(icon, R.drawable.ic_hs_plus, ICON_BLUE, 30);
        card.addView(icon, new LinearLayout.LayoutParams(dp(48), -1));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = label("Criar novo grupo", 18, TEXT, true);
        TextView d = label("Um grupo pode guardar até 8 janelas e abrir até 4 visíveis nesta versão.", 12, MUTED, false);
        texts.addView(t, new LinearLayout.LayoutParams(-1, dp(30)));
        texts.addView(d, new LinearLayout.LayoutParams(-1, dp(34)));
        card.addView(texts, new LinearLayout.LayoutParams(0, -1, 1));
        return card;
    }

    private void createAndEnterGroup() {
        WindowGroup group = createGroup("Grupo " + nextGroupNumber);
        activeGroup = group;
        visiblePanes.clear();
        groupOverviewMode = false;
        updateWindowLayout();
        Toast.makeText(this, "Grupo criado", Toast.LENGTH_SHORT).show();
    }

    private void enterGroup(WindowGroup group) {
        if (group == null) {
            if (groups.isEmpty()) group = createGroup("Meu Hub");
            else group = groups.get(0);
        }
        activeGroup = group;
        groupOverviewMode = false;
        if (focusMode) exitFocus();
        visiblePanes.clear();
        visiblePanes.addAll(group.visible);
        if (visiblePanes.isEmpty()) {
            for (StreamPane p : group.panes) {
                if (visiblePanes.size() >= Math.min(MAX_VISIBLE_WINDOWS, 2)) break;
                visiblePanes.add(p);
            }
        }
        updateWindowLayout();
    }

    private StreamPane createPane(String name, String url, int accent) {
        StreamPane pane = new StreamPane(this, nextPaneNumber++, name, url, accent);
        panes.add(pane);
        if (activeGroup != null && !activeGroup.panes.contains(pane)) activeGroup.panes.add(pane);
        return pane;
    }

    private int accentForIndex(int index) {
        int[] accents = new int[]{PURPLE, BLUE, Color.rgb(34, 211, 238), Color.rgb(168, 85, 247), Color.rgb(14, 165, 233), Color.rgb(99, 102, 241)};
        return accents[Math.abs(index) % accents.length];
    }

    private void addWindow() {
        if (activeGroup == null) activeGroup = groups.isEmpty() ? createGroup("Meu Hub") : groups.get(0);
        if (groupOverviewMode) groupOverviewMode = false;
        if (activeGroup.panes.size() >= MAX_OPEN_WINDOWS) {
            Toast.makeText(this, "Limite do grupo nesta versão: " + MAX_OPEN_WINDOWS + " janelas", Toast.LENGTH_LONG).show();
            return;
        }
        if (panes.size() >= MAX_OPEN_WINDOWS * Math.max(1, groups.size())) {
            Toast.makeText(this, "Limite técnico desta versão: " + MAX_OPEN_WINDOWS + " janelas abertas", Toast.LENGTH_LONG).show();
            return;
        }
        StreamPane pane = createPane("Hub Home", getHomepageUrl(), accentForIndex(panes.size()));
        pane.loadDefault();
        if (visiblePanes.size() < MAX_VISIBLE_WINDOWS) {
            showPane(pane);
            Toast.makeText(this, "Janela adicionada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Janela criada e minimizada. Abra o gerenciador para trocar.", Toast.LENGTH_LONG).show();
            showWindowManager();
        }
    }

    private void showPane(StreamPane pane) {
        if (!panes.contains(pane)) panes.add(pane);
        if (activeGroup != null && !activeGroup.panes.contains(pane)) activeGroup.panes.add(pane);
        if (!visiblePanes.contains(pane)) {
            if (visiblePanes.size() >= MAX_VISIBLE_WINDOWS) {
                Toast.makeText(this, "Máximo visível agora: 4 janelas", Toast.LENGTH_SHORT).show();
                return;
            }
            visiblePanes.add(pane);
        }
        if (activeGroup != null && !activeGroup.visible.contains(pane)) activeGroup.visible.add(pane);
        pane.visible = true;
        updateWindowLayout();
    }

    private void hidePane(StreamPane pane) {
        if (focusMode) exitFocus();
        visiblePanes.remove(pane);
        if (activeGroup != null) activeGroup.visible.remove(pane);
        pane.visible = false;
        updateWindowLayout();
    }

    private void closePane(StreamPane pane) {
        if (focusMode) exitFocus();
        visiblePanes.remove(pane);
        for (WindowGroup g : groups) { g.panes.remove(pane); g.visible.remove(pane); }
        panes.remove(pane);
        detach(pane.container);
        try { pane.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
        try { pane.webView.destroy(); } catch (Exception ignored) {}
        updateWindowNumbers();
        updateWindowLayout();
    }

    private void closeAllPanes() {
        if (focusMode) exitFocus();
        List<StreamPane> target = activeGroup == null ? new ArrayList<>(panes) : new ArrayList<>(activeGroup.panes);
        for (StreamPane pane : target) {
            detach(pane.container);
            try { pane.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
            try { pane.webView.destroy(); } catch (Exception ignored) {}
            panes.remove(pane);
            for (WindowGroup g : groups) { g.panes.remove(pane); g.visible.remove(pane); }
        }
        visiblePanes.clear();
        updateWindowLayout();
    }

    private void updateWindowNumbers() {
        for (int i = 0; i < panes.size(); i++) {
            panes.get(i).number = i + 1;
            panes.get(i).updateHeader();
        }
        nextPaneNumber = panes.size() + 1;
    }

    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        sidebar.setVisibility(sidebarCollapsed ? View.GONE : View.VISIBLE);
        mainArea.setPadding(sidebarCollapsed ? 0 : dp(12), 0, 0, 0);
        Toast.makeText(this, sidebarCollapsed ? "Menu recolhido" : "Menu aberto", Toast.LENGTH_SHORT).show();
    }

    private void cycleLayoutMode() {
        layoutMode = (layoutMode + 1) % 4;
        updateWindowLayout();
        String msg;
        if (layoutMode == 0) msg = "Layout igual";
        else if (layoutMode == 1) msg = "Primeira janela maior";
        else if (layoutMode == 2) msg = "Segunda janela maior";
        else msg = "Grade 2x2";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updateWindowLayout() {
        if (focusMode || windowContainer == null) return;
        if (groupOverviewMode) {
            showGroupsOverview();
            return;
        }
        windowContainer.removeAllViews();
        detach(emptyState);
        for (StreamPane pane : panes) {
            detach(pane.container);
            pane.setTileCompact(false);
        }

        if (visiblePanes.isEmpty()) {
            windowContainer.setOrientation(LinearLayout.VERTICAL);
            windowContainer.addView(emptyState, new LinearLayout.LayoutParams(-1, -1));
            updateStatusText();
            return;
        }

        // Em retrato e em 3/4 janelas, o app vira workspace: prioriza o conteúdo.
        if ((visiblePanes.size() >= 3 || (isPortraitWorkspace() && visiblePanes.size() >= 2)) && !sidebarCollapsed) {
            sidebarCollapsed = true;
            sidebar.setVisibility(View.GONE);
            mainArea.setPadding(0, 0, 0, 0);
        }

        boolean compactTiles = shouldUseCompactTiles();
        for (StreamPane pane : visiblePanes) pane.setTileCompact(compactTiles);

        if (visiblePanes.size() <= 4) {
            renderSmartSlotLayout();
            updateStatusText();
            return;
        }

        renderGridLayout();
        updateStatusText();
    }

    private boolean isPortraitWorkspace() {
        return getResources().getConfiguration().screenHeightDp > getResources().getConfiguration().screenWidthDp;
    }

    private boolean shouldUseCompactTiles() {
        // Em retrato, 3/4 janelas precisam esconder a barra URL para o site respirar.
        return isPortraitWorkspace() && visiblePanes.size() >= 3;
    }

    private void renderPortraitAdaptiveLayout() {
        windowContainer.setOrientation(LinearLayout.VERTICAL);
        if (visiblePanes.size() == 2) {
            // Em retrato, duas janelas empilhadas mostram melhor o conteúdo do site do que lado a lado.
            windowContainer.addView(visiblePanes.get(0).container, new LinearLayout.LayoutParams(-1, 0, 1));
            windowContainer.addView(spacer(true), new LinearLayout.LayoutParams(-1, dp(5)));
            windowContainer.addView(visiblePanes.get(1).container, new LinearLayout.LayoutParams(-1, 0, 1));
            return;
        }

        // Três/quatro janelas em retrato: grade 2x2 compacta.
        LinearLayout row1 = gridRow();
        LinearLayout row2 = gridRow();
        windowContainer.addView(row1, new LinearLayout.LayoutParams(-1, 0, 1));
        windowContainer.addView(spacer(true), new LinearLayout.LayoutParams(-1, dp(5)));
        windowContainer.addView(row2, new LinearLayout.LayoutParams(-1, 0, 1));

        addPaneToRow(row1, visiblePanes.get(0));
        if (visiblePanes.size() > 1) addPaneToRow(row1, visiblePanes.get(1));
        if (visiblePanes.size() > 2) addPaneToRow(row2, visiblePanes.get(2));
        if (visiblePanes.size() > 3) addPaneToRow(row2, visiblePanes.get(3));
    }

    private void renderTwoPaneLayout() {
        windowContainer.setOrientation(verticalSplit ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        StreamPane first = visiblePanes.get(0);
        StreamPane second = visiblePanes.get(1);
        float weightA = layoutMode == 1 ? 1.7f : layoutMode == 2 ? 1f : 1f;
        float weightB = layoutMode == 1 ? 1f : layoutMode == 2 ? 1.7f : 1f;

        if (verticalSplit) {
            windowContainer.addView(first.container, new LinearLayout.LayoutParams(-1, 0, weightA));
            windowContainer.addView(spacer(true), new LinearLayout.LayoutParams(-1, dp(6)));
            windowContainer.addView(second.container, new LinearLayout.LayoutParams(-1, 0, weightB));
        } else {
            windowContainer.addView(first.container, new LinearLayout.LayoutParams(0, -1, weightA));
            windowContainer.addView(spacer(false), new LinearLayout.LayoutParams(dp(6), -1));
            windowContainer.addView(second.container, new LinearLayout.LayoutParams(0, -1, weightB));
        }
    }

    private void renderGridLayout() {
        windowContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row1 = gridRow();
        LinearLayout row2 = gridRow();
        windowContainer.addView(row1, new LinearLayout.LayoutParams(-1, 0, 1));
        if (visiblePanes.size() > 2) {
            View gap = new View(this);
            windowContainer.addView(gap, new LinearLayout.LayoutParams(-1, dp(6)));
            windowContainer.addView(row2, new LinearLayout.LayoutParams(-1, 0, 1));
        }

        addPaneToRow(row1, visiblePanes.get(0));
        if (visiblePanes.size() > 1) addPaneToRow(row1, visiblePanes.get(1));
        if (visiblePanes.size() > 2) addPaneToRow(row2, visiblePanes.get(2));
        if (visiblePanes.size() > 3) addPaneToRow(row2, visiblePanes.get(3));
    }

    private LinearLayout gridRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void addPaneToRow(LinearLayout row, StreamPane pane) {
        if (row.getChildCount() > 0) row.addView(spacer(false), new LinearLayout.LayoutParams(dp(6), -1));
        row.addView(pane.container, new LinearLayout.LayoutParams(0, -1, 1));
    }


    private LinearLayout createAddSlot(String tip) {
        LinearLayout slot = new LinearLayout(this);
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setGravity(Gravity.CENTER);
        slot.setPadding(dp(12), dp(12), dp(12), dp(12));
        slot.setBackground(cardBg(Color.rgb(8, 11, 20), Color.rgb(45, 56, 86), dp(18), 1));
        slot.setOnClickListener(v -> addWindow());
        attachTip(slot, tip == null ? "Adicionar janela" : tip);

        TextView plus = label("", 34, BLUE, true);
        plus.setGravity(Gravity.CENTER);
        setCenterIcon(plus, R.drawable.ic_hs_plus, ICON_BLUE, 42);
        slot.addView(plus, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView txt = label("Nova janela", 13, MUTED, true);
        txt.setGravity(Gravity.CENTER);
        slot.addView(txt, new LinearLayout.LayoutParams(-1, dp(28)));
        return slot;
    }

    private void addSlotToRow(LinearLayout row, String tip) {
        if (row.getChildCount() > 0) row.addView(spacer(false), new LinearLayout.LayoutParams(dp(6), -1));
        row.addView(createAddSlot(tip), new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void renderSmartSlotLayout() {
        windowContainer.setOrientation(LinearLayout.VERTICAL);
        int count = visiblePanes.size();

        if (count == 1) {
            LinearLayout row = gridRow();
            windowContainer.addView(row, new LinearLayout.LayoutParams(-1, -1));
            addPaneToRow(row, visiblePanes.get(0));
            addSlotToRow(row, "Adicionar segunda janela");
            return;
        }

        LinearLayout row1 = gridRow();
        LinearLayout row2 = gridRow();
        windowContainer.addView(row1, new LinearLayout.LayoutParams(-1, 0, 1));
        windowContainer.addView(spacer(true), new LinearLayout.LayoutParams(-1, dp(6)));
        windowContainer.addView(row2, new LinearLayout.LayoutParams(-1, 0, 1));

        addPaneToRow(row1, visiblePanes.get(0));
        if (count > 1) addPaneToRow(row1, visiblePanes.get(1));
        else addSlotToRow(row1, "Adicionar segunda janela");

        if (count == 2) {
            addSlotToRow(row2, "Adicionar terceira janela");
        } else if (count == 3) {
            addPaneToRow(row2, visiblePanes.get(2));
            addSlotToRow(row2, "Adicionar quarta janela");
        } else {
            addPaneToRow(row2, visiblePanes.get(2));
            addPaneToRow(row2, visiblePanes.get(3));
        }
    }

    private View spacer(boolean horizontalLine) {
        View v = new View(this);
        v.setTag("split-spacer");
        return v;
    }

    private void updateStatusText() {
        if (groupOverviewMode) {
            if (screenStatusDesc != null) screenStatusDesc.setText(groups.size() + " grupos salvos");
            if (screenStatusTitle != null) screenStatusTitle.setText("Groups  •");
            if (headerTitle != null) headerTitle.setText("Groups");
            if (headerDesc != null) headerDesc.setText("Escolha um grupo para abrir o workspace");
            return;
        }
        if (screenStatusDesc != null) screenStatusDesc.setText(visiblePanes.size() + " visíveis / " + (activeGroup == null ? panes.size() : activeGroup.panes.size()) + " no grupo");
        if (screenStatusTitle != null) screenStatusTitle.setText(visiblePanes.size() >= 4 ? "Grid Mode  •" : "Hub View  •");
        if (headerTitle != null) headerTitle.setText(visiblePanes.size() >= 4 ? "Grid View" : "Hub View");
        if (headerDesc != null) {
            if (visiblePanes.size() == 0) headerDesc.setText("Adicione uma janela para começar");
            else if (visiblePanes.size() == 1) headerDesc.setText("Uma janela web ativa no núcleo");
            else if (isPortraitWorkspace()) headerDesc.setText(visiblePanes.size() + " janelas adaptadas ao retrato");
            else headerDesc.setText(visiblePanes.size() + " janelas web visíveis simultaneamente");
        }
    }

    private void swapFirstTwoVisible() {
        if (visiblePanes.size() < 2) {
            Toast.makeText(this, "Abra duas janelas visíveis para trocar", Toast.LENGTH_SHORT).show();
            return;
        }
        StreamPane first = visiblePanes.get(0);
        StreamPane second = visiblePanes.get(1);
        int i1 = visiblePanes.indexOf(first);
        int i2 = visiblePanes.indexOf(second);
        visiblePanes.set(i1, second);
        visiblePanes.set(i2, first);
        updateWindowLayout();
    }

    private void enterFocus(StreamPane pane) {
        if (!visiblePanes.contains(pane)) return;
        focusMode = true;
        sidebar.setVisibility(View.GONE);
        topBar.setVisibility(View.GONE);
        for (StreamPane p : visiblePanes) {
            p.container.setVisibility(p == pane ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < windowContainer.getChildCount(); i++) {
            View child = windowContainer.getChildAt(i);
            if (child != pane.container) child.setVisibility(child instanceof LinearLayout ? child.getVisibility() : View.GONE);
        }
    }

    private void exitFocus() {
        focusMode = false;
        sidebar.setVisibility(sidebarCollapsed ? View.GONE : View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        updateWindowLayout();
    }

    private void showWindowManager() {
        final AlertDialog[] holder = new AlertDialog[1];
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = dialogPanel();
        TextView title = label("Janelas abertas", 22, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_layout, ICON_BLUE, 22);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        List<StreamPane> managedPanes = activeGroup == null ? panes : activeGroup.panes;
        TextView info = label(managedPanes.size() + " abertas no grupo • " + visiblePanes.size() + " visíveis • limite visível atual: 4", 13, MUTED, false);
        panel.addView(info, new LinearLayout.LayoutParams(-1, dp(30)));

        TextView add = dialogAction("Nova janela", R.drawable.ic_hs_plus, () -> { if (holder[0] != null) holder[0].dismiss(); addWindow(); });
        add.setTextColor(TEXT);
        panel.addView(add, new LinearLayout.LayoutParams(-1, dp(42)));

        if (managedPanes.isEmpty()) {
            TextView empty = label("Nenhuma janela aberta ainda.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(80)));
        } else {
            for (StreamPane pane : new ArrayList<>(managedPanes)) {
                panel.addView(windowCard(pane, holder), new LinearLayout.LayoutParams(-1, dp(84)));
            }
        }

        scroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        holder[0] = dialog;
        dialog.setView(scroll);
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        });
        dialog.show();
    }

    private LinearLayout windowCard(StreamPane pane, AlertDialog[] holder) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(7), dp(12), dp(7));
        card.setBackground(cardBg(Color.rgb(14, 18, 30), visiblePanes.contains(pane) ? pane.accent : Color.rgb(41, 48, 66), dp(14), 1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = label(pane.displayTitle(), 15, TEXT, true);
        setLeftIcon(title, visiblePanes.contains(pane) ? R.drawable.ic_hs_grid : R.drawable.ic_hs_browser, visiblePanes.contains(pane) ? ICON_BLUE : ICON_NORMAL, 18);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(28), 1));

        TextView show = topIcon(visiblePanes.contains(pane) ? R.drawable.ic_hs_close : R.drawable.ic_hs_plus, ICON_NORMAL, visiblePanes.contains(pane) ? "Ocultar janela" : "Mostrar janela", v -> {
            if (holder[0] != null) holder[0].dismiss();
            if (visiblePanes.contains(pane)) hidePane(pane); else showPane(pane);
        });
        top.addView(show, new LinearLayout.LayoutParams(dp(36), dp(30)));

        TextView focus = topIcon(R.drawable.ic_hs_focus, ICON_NORMAL, "Focar janela", v -> {
            if (holder[0] != null) holder[0].dismiss();
            if (!visiblePanes.contains(pane)) showPane(pane);
            enterFocus(pane);
        });
        top.addView(focus, new LinearLayout.LayoutParams(dp(36), dp(30)));

        TextView close = topIcon(R.drawable.ic_hs_close, ICON_NORMAL, "Fechar janela", v -> { if (holder[0] != null) holder[0].dismiss(); closePane(pane); });
        top.addView(close, new LinearLayout.LayoutParams(dp(36), dp(30)));
        card.addView(top, new LinearLayout.LayoutParams(-1, dp(32)));

        TextView url = label(shortUrl(pane.currentUrl()), 11, MUTED, false);
        url.setPadding(dp(2), 0, 0, 0);
        card.addView(url, new LinearLayout.LayoutParams(-1, dp(22)));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(84));
        cardParams.setMargins(0, dp(7), 0, 0);
        card.setLayoutParams(cardParams);
        return card;
    }

    private void showMorePanel() {
        final AlertDialog[] holder = new AlertDialog[1];
        LinearLayout panel = dialogPanel();
        TextView title = label("HubSyncBr", 22, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_sync, ICON_ACTIVE, 24);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));

        panel.addView(dialogAction("Adicionar janela", R.drawable.ic_hs_plus, () -> { if (holder[0] != null) holder[0].dismiss(); addWindow(); }));
        panel.addView(dialogAction("Ver grupos", R.drawable.ic_hs_grid, () -> { if (holder[0] != null) holder[0].dismiss(); showGroupsOverview(); }));
        panel.addView(dialogAction("Novo grupo", R.drawable.ic_hs_plus, () -> { if (holder[0] != null) holder[0].dismiss(); createAndEnterGroup(); }));
        panel.addView(dialogAction("Gerenciar janelas", R.drawable.ic_hs_layout, () -> { if (holder[0] != null) holder[0].dismiss(); showWindowManager(); }));
        panel.addView(dialogAction("Recolher/abrir menu", R.drawable.ic_hs_menu, () -> { if (holder[0] != null) holder[0].dismiss(); toggleSidebar(); }));
        panel.addView(dialogAction("Trocar layout", R.drawable.ic_hs_resize, () -> { if (holder[0] != null) holder[0].dismiss(); cycleLayoutMode(); }));
        panel.addView(dialogAction("Recarregar visíveis", R.drawable.ic_hs_reload, () -> { if (holder[0] != null) holder[0].dismiss(); reloadVisible(); }));
        panel.addView(dialogAction("Fechar todas", R.drawable.ic_hs_close, () -> { if (holder[0] != null) holder[0].dismiss(); closeAllPanes(); }));
        panel.addView(dialogAction("Sobre", R.drawable.ic_hs_settings, () -> { if (holder[0] != null) holder[0].dismiss(); showAboutDialog(); }));
        panel.addView(dialogAction("Configurações do navegador", R.drawable.ic_hs_settings, () -> { if (holder[0] != null) holder[0].dismiss(); showBrowserSettings(); }));
        panel.addView(dialogAction("Aviso legal", R.drawable.ic_hs_browser, () -> { if (holder[0] != null) holder[0].dismiss(); showLegalDialog(null); }));

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        holder[0] = dialog;
        dialog.setView(panel);
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        });
        dialog.show();
    }

    private void showPaneActions(StreamPane pane) {
        final AlertDialog[] holder = new AlertDialog[1];
        LinearLayout panel = dialogPanel();
        TextView title = label(pane.displayTitle(), 20, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_layout, ICON_BLUE, 22);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(46)));

        panel.addView(dialogAction(focusMode ? "Voltar do foco" : "Focar janela", R.drawable.ic_hs_focus, () -> {
            if (holder[0] != null) holder[0].dismiss();
            if (focusMode) exitFocus(); else enterFocus(pane);
        }));
        panel.addView(dialogAction(pane.muted ? "Ativar som" : "Mutar janela", pane.muted ? R.drawable.ic_hs_volume : R.drawable.ic_hs_mute, () -> {
            if (holder[0] != null) holder[0].dismiss();
            pane.toggleMute();
        }));
        panel.addView(dialogAction("Volume 45%", R.drawable.ic_hs_volume, () -> {
            if (holder[0] != null) holder[0].dismiss();
            pane.cycleVolume();
        }));
        panel.addView(dialogAction(pane.desktopMode ? "Modo mobile" : "Modo desktop", R.drawable.ic_hs_desktop, () -> {
            if (holder[0] != null) holder[0].dismiss();
            pane.toggleDesktopMode();
        }));
        panel.addView(dialogAction("Abrir externo", R.drawable.ic_hs_external, () -> {
            if (holder[0] != null) holder[0].dismiss();
            openExternal(pane.currentUrl());
        }));
        panel.addView(dialogAction("Recarregar", R.drawable.ic_hs_reload, () -> {
            if (holder[0] != null) holder[0].dismiss();
            pane.webView.reload();
        }));
        panel.addView(dialogAction("Ocultar/minimizar", R.drawable.ic_hs_close, () -> {
            if (holder[0] != null) holder[0].dismiss();
            hidePane(pane);
        }));
        panel.addView(dialogAction("Fechar janela", R.drawable.ic_hs_close, () -> {
            if (holder[0] != null) holder[0].dismiss();
            closePane(pane);
        }));

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        holder[0] = dialog;
        dialog.setView(panel);
        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        });
        dialog.show();
    }

    private void reloadVisible() {
        for (StreamPane pane : visiblePanes) {
            try { pane.webView.reload(); } catch (Exception ignored) {}
        }
    }

    private LinearLayout dialogPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(cardBg(Color.rgb(12, 16, 28), Color.rgb(88, 70, 180), dp(22), 1));
        return panel;
    }

    private TextView dialogAction(String text, int iconRes, Runnable action) {
        TextView item = label(text, 16, TEXT, false);
        setLeftIcon(item, iconRes, ICON_NORMAL, 20);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(8), 0, dp(8), 0);
        item.setBackground(cardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(12), 0));
        item.setOnClickListener(v -> action.run());
        item.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
        return item;
    }

    private Button panelButton(String text, int iconRes, int accent) {
        Button b = chip(text, accent);
        setButtonIcon(b, iconRes, ICON_ACTIVE, 18);
        return b;
    }

    private Button panelSmallButton(String text, int iconRes, int accent) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(cardBg(Color.rgb(17, 22, 35), accent, dp(11), 1));
        setButtonIcon(b, iconRes, ICON_NORMAL, 14);
        return b;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Drawable iconDrawable(int resId, int color, int sizeDp) {
        Drawable d = getResources().getDrawable(resId).mutate();
        d.setTint(color);
        d.setBounds(0, 0, dp(sizeDp), dp(sizeDp));
        return d;
    }

    private void attachTip(View view, String message) {
        view.setOnLongClickListener(v -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void setLeftIcon(TextView target, int resId, int color, int sizeDp) {
        target.setCompoundDrawables(iconDrawable(resId, color, sizeDp), null, null, null);
        target.setCompoundDrawablePadding(dp(10));
    }

    private void setCenterIcon(TextView target, int resId, int color, int sizeDp) {
        target.setCompoundDrawables(null, iconDrawable(resId, color, sizeDp), null, null);
    }

    private void setButtonIcon(Button target, int resId, int color, int sizeDp) {
        target.setCompoundDrawables(iconDrawable(resId, color, sizeDp), null, null, null);
        target.setCompoundDrawablePadding(dp(6));
    }

    private Button chip(String text, int accent) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(cardBg(Color.rgb(19, 24, 38), accent, dp(10), 2));
        return b;
    }

    private GradientDrawable cardBg(int fill, int stroke, int radius, int strokeWidth) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0) bg.setStroke(dp(strokeWidth), stroke);
        return bg;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void detach(View view) {
        if (view == null) return;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) parent.removeView(view);
        view.setVisibility(View.VISIBLE);
    }


    private SharedPreferences hubPrefs() {
        return getSharedPreferences("hub", MODE_PRIVATE);
    }

    private String getHomepageUrl() {
        SharedPreferences prefs = hubPrefs();
        String mode = prefs.getString("homepage_mode", "hub");
        if ("blank".equals(mode)) return "about:blank";
        if ("google".equals(mode)) return "https://www.google.com";
        if ("custom".equals(mode)) {
            String custom = prefs.getString("custom_homepage", "https://www.google.com");
            if (custom == null || custom.trim().isEmpty()) return hubHomeDataUrl();
            String c = custom.trim();
            if (c.startsWith("http://") || c.startsWith("https://")) return c;
            if (c.contains(".")) return "https://" + c;
            return searchUrl(c);
        }
        return hubHomeDataUrl();
    }

    private String searchUrl(String query) {
        String q = query == null ? "" : query.trim();
        try { q = URLEncoder.encode(q, "UTF-8"); } catch (Exception ignored) { q = q.replace(" ", "+"); }
        String engine = hubPrefs().getString("search_engine", "google");
        if ("duckduckgo".equals(engine)) return "https://duckduckgo.com/?q=" + q;
        if ("bing".equals(engine)) return "https://www.bing.com/search?q=" + q;
        if ("brave".equals(engine)) return "https://search.brave.com/search?q=" + q;
        return "https://www.google.com/search?q=" + q;
    }

    private String hubHomeDataUrl() {
        
        String brandLogo = "data:image/png;base64,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";
String engine = hubPrefs().getString("search_engine", "google");
        String engineName = "Google";
        if ("duckduckgo".equals(engine)) engineName = "DuckDuckGo";
        else if ("bing".equals(engine)) engineName = "Bing";
        else if ("brave".equals(engine)) engineName = "Brave";
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<style>body{margin:0;background:#070a12;color:#ecf0ff;font-family:system-ui,-apple-system,Segoe UI,Arial;padding:24px;}" +
                ".wrap{max-width:900px;margin:auto}.logo{font-size:34px;font-weight:800;margin-top:20px;display:flex;align-items:center;gap:12px}.tag{color:#9aa3b8;margin:8px 0 24px}" +
                ".search{display:flex;gap:8px;background:#101420;border:1px solid #4f46e5;border-radius:22px;padding:8px;box-shadow:0 0 18px #1d4ed833}" +
                "input{flex:1;background:transparent;border:0;color:#fff;font-size:16px;outline:0;padding:12px}button{background:#8b5cf6;color:white;border:0;border-radius:16px;padding:0 18px;font-weight:700}" +
                ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:12px;margin-top:24px}.card{display:block;text-decoration:none;color:#ecf0ff;background:#101420;border:1px solid #25304a;border-radius:18px;padding:18px}" +
                ".card b{display:block;margin-bottom:6px}.card span{color:#9aa3b8;font-size:12px}.engine{color:#38bdf8;font-size:12px;margin-top:10px}</style></head><body><div class='wrap'>" +
                "<div class='logo'><img src='" + brandLogo + "' style='width:58px;height:58px;border-radius:18px;object-fit:contain'>HubSyncBr</div><div class='tag'>Seu browser workspace para janelas, grupos e transmissões.</div>" +
                "<form class='search' onsubmit=\"var q=document.getElementById('q').value.trim(); if(!q)return false; if(q.indexOf('.')>-1 && q.indexOf(' ')==-1){location.href=q.indexOf('http')==0?q:'https://'+q}else{location.href='" + searchUrl("__Q__").replace("__Q__", "'+encodeURIComponent(q)+'") + "'} return false;\">" +
                "<input id='q' placeholder='Pesquisar ou digitar URL'><button>Ir</button></form><div class='engine'>Busca atual: " + engineName + "</div>" +
                "<div class='grid'>" +
                "<a class='card' href='https://www.youtube.com'><b>YouTube</b><span>Vídeos e transmissões</span></a>" +
                "<a class='card' href='https://www.twitch.tv'><b>Twitch</b><span>Lives e canais</span></a>" +
                "<a class='card' href='https://www.kick.com'><b>Kick</b><span>Streams</span></a>" +
                "<a class='card' href='https://www.google.com'><b>Google</b><span>Pesquisa web</span></a>" +
                "<a class='card' href='https://chat.openai.com'><b>ChatGPT</b><span>Assistente web</span></a>" +
                "<a class='card' href='https://github.com'><b>GitHub</b><span>Código e projetos</span></a>" +
                "</div></div></body></html>";
        return "data:text/html;charset=utf-8," + Uri.encode(html);
    }

    private void showBrowserSettings() {
        final String[] items = new String[]{
                "Homepage: HubSyncBr Home",
                "Homepage: Google",
                "Homepage: Em branco",
                "Busca: Google",
                "Busca: DuckDuckGo",
                "Busca: Bing",
                "Busca: Brave Search"
        };
        new AlertDialog.Builder(this)
                .setTitle("Navegador HubSyncBr")
                .setItems(items, (dialog, which) -> {
                    SharedPreferences.Editor e = hubPrefs().edit();
                    if (which == 0) e.putString("homepage_mode", "hub");
                    if (which == 1) e.putString("homepage_mode", "google");
                    if (which == 2) e.putString("homepage_mode", "blank");
                    if (which == 3) e.putString("search_engine", "google");
                    if (which == 4) e.putString("search_engine", "duckduckgo");
                    if (which == 5) e.putString("search_engine", "bing");
                    if (which == 6) e.putString("search_engine", "brave");
                    e.apply();
                    Toast.makeText(this, "Configuração salva. Novas janelas usarão a nova opção.", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private String normalizeUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) return getHomepageUrl();
        String u = raw.trim();
        if (u.equals("about:blank")) return u;
        if (u.equals("hubsyncbr://home")) return getHomepageUrl();
        if (u.startsWith("data:")) return u;
        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file://")) return u;
        boolean looksLikeUrl = u.contains(".") && !u.contains(" ");
        if (looksLikeUrl) return "https://" + u;
        return searchUrl(u);
    }

    private String shortUrl(String raw) {
        String u = raw == null ? "about:blank" : raw;
        if (u.length() > 72) return u.substring(0, 69) + "...";
        return u;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(url))));
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir externo", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLegalNoticeOnce() {
        SharedPreferences prefs = getSharedPreferences("hub", MODE_PRIVATE);
        if (!prefs.getBoolean("legal_ok", false)) {
            showLegalDialog(() -> prefs.edit().putBoolean("legal_ok", true).apply());
        }
    }

    private void showLegalDialog(final Runnable onOk) {
        new AlertDialog.Builder(this)
                .setTitle("HubSyncBr — uso transparente")
                .setMessage("O HubSyncBr não hospeda, retransmite, baixa, extrai ou modifica transmissões. Cada janela abre o site escolhido pelo usuário e respeita login, anúncios, créditos, player e regras da plataforma original.")
                .setPositiveButton("Entendi", (d, w) -> { if (onOk != null) onOk.run(); })
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("HubSyncBr 0.5.1 — Adaptive Portrait Workspace")
                .setMessage("Workspace adaptativo: modo retrato com janelas ajustáveis, grade 2x2 quando houver 4 visíveis, cards de janelas mais limpos e núcleo preparado para grupos estilo Chrome.")
                .setPositiveButton("OK", null)
                .show();
    }


    private class WindowGroup {
        final int id;
        final String name;
        final int accent;
        final List<StreamPane> panes = new ArrayList<>();
        final List<StreamPane> visible = new ArrayList<>();

        WindowGroup(int idValue, String groupName, int groupAccent) {
            id = idValue;
            name = groupName;
            accent = groupAccent;
        }
    }

    private class StreamPane {
        final LinearLayout container;
        final TextView title;
        final EditText urlBar;
        final LinearLayout toolbar;
        final WebView webView;
        final Button focusButton;
        final Button muteButton;
        final int accent;
        final String defaultUrl;
        final String initialName;
        final String mobileUa;
        boolean visible = true;
        boolean desktopMode = false;
        boolean muted = false;
        int number;
        String pageTitle = "";

        StreamPane(Context ctx, int numberValue, String name, String url, int accentColor) {
            number = numberValue;
            initialName = name;
            defaultUrl = url;
            accent = accentColor;
            webView = new WebView(ctx);

            container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp(4), dp(4), dp(4), dp(4));
            container.setBackground(cardBg(Color.rgb(10, 13, 23), accent, dp(10), 1));

            LinearLayout header = new LinearLayout(ctx);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(6), 0, dp(4), 0);
            title = label("", 12, TEXT, true);
            header.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
            TextView actions = label("", 18, MUTED, true);
            setCenterIcon(actions, R.drawable.ic_hs_more, ICON_NORMAL, 18);
            actions.setGravity(Gravity.CENTER);
            actions.setOnClickListener(v -> showPaneActions(this));
            attachTip(actions, "Ações da janela");
            header.addView(actions, new LinearLayout.LayoutParams(dp(30), -1));
            TextView close = label("", 18, MUTED, true);
            setCenterIcon(close, R.drawable.ic_hs_close, ICON_NORMAL, 18);
            close.setGravity(Gravity.CENTER);
            close.setOnClickListener(v -> closePane(this));
            attachTip(close, "Fechar janela");
            header.addView(close, new LinearLayout.LayoutParams(dp(30), -1));
            container.addView(header, new LinearLayout.LayoutParams(-1, dp(28)));
            updateHeader();

            View accentLine = new View(ctx);
            accentLine.setBackgroundColor(accent);
            container.addView(accentLine, new LinearLayout.LayoutParams(-1, dp(2)));

            toolbar = new LinearLayout(ctx);
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            toolbar.setGravity(Gravity.CENTER_VERTICAL);
            toolbar.setPadding(dp(2), dp(3), dp(2), dp(3));
            toolbar.setBackgroundColor(Color.rgb(9, 13, 23));

            Button back = miniButton("");
            setButtonIcon(back, R.drawable.ic_hs_back, ICON_NORMAL, 18);
            back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
            attachTip(back, "Voltar");
            toolbar.addView(back, new LinearLayout.LayoutParams(dp(34), dp(36)));

            Button forward = miniButton("");
            setButtonIcon(forward, R.drawable.ic_hs_forward, ICON_NORMAL, 18);
            forward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
            attachTip(forward, "Avançar");
            toolbar.addView(forward, new LinearLayout.LayoutParams(dp(34), dp(36)));

            Button reload = miniButton("");
            setButtonIcon(reload, R.drawable.ic_hs_reload, ICON_NORMAL, 18);
            reload.setOnClickListener(v -> webView.reload());
            attachTip(reload, "Recarregar");
            toolbar.addView(reload, new LinearLayout.LayoutParams(dp(34), dp(36)));

            urlBar = new EditText(ctx);
            urlBar.setSingleLine(true);
            urlBar.setText(url);
            urlBar.setTextSize(11);
            urlBar.setTextColor(TEXT);
            urlBar.setHintTextColor(MUTED);
            urlBar.setHint("URL ou pesquisa");
            urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
            urlBar.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
            urlBar.setPadding(dp(10), 0, dp(10), 0);
            urlBar.setBackground(cardBg(Color.rgb(15, 19, 30), Color.rgb(31, 38, 57), dp(20), 1));
            urlBar.setSelectAllOnFocus(true);
            urlBar.setOnEditorActionListener((v, actionId, event) -> {
                boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    loadUrl(urlBar.getText().toString());
                    return true;
                }
                return false;
            });
            toolbar.addView(urlBar, new LinearLayout.LayoutParams(0, dp(36), 1));

            Button go = miniButton("Go");
            go.setTextSize(11);
            go.setOnClickListener(v -> loadUrl(urlBar.getText().toString()));
            attachTip(go, "Ir");
            toolbar.addView(go, new LinearLayout.LayoutParams(dp(38), dp(36)));
            container.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(42)));

            configureWebView(webView);
            mobileUa = webView.getSettings().getUserAgentString();
            webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!hubPrefs().getBoolean("nav_autohide", true)) return;
                if (urlBar.hasFocus()) return;
                if (scrollY > oldScrollY + dp(3)) toolbar.setVisibility(View.GONE);
                else if (scrollY < oldScrollY) toolbar.setVisibility(View.VISIBLE);
            });
            webView.setOnClickListener(v -> toolbar.setVisibility(View.VISIBLE));
            container.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

            focusButton = new Button(MainActivity.this);
            muteButton = new Button(MainActivity.this);
        }

        void setTileCompact(boolean compact) {
            // Compacta cada janela quando o núcleo precisa mostrar 3/4 telas no modo retrato.
            container.setPadding(dp(3), dp(3), dp(3), dp(3));
            View headerView = container.getChildAt(0);
            if (headerView != null) {
                ViewGroup.LayoutParams hp = headerView.getLayoutParams();
                hp.height = compact ? dp(22) : dp(28);
                headerView.setLayoutParams(hp);
            }
            View lineView = container.getChildAt(1);
            if (lineView != null) {
                ViewGroup.LayoutParams lp = lineView.getLayoutParams();
                lp.height = compact ? dp(1) : dp(2);
                lineView.setLayoutParams(lp);
            }
            View toolbarView = container.getChildAt(2);
            if (toolbarView != null) toolbarView.setVisibility(compact ? View.GONE : View.VISIBLE);
            title.setTextSize(compact ? 10 : 12);
        }

        private Button miniButton(String text) {
            Button b = new Button(MainActivity.this);
            b.setText(text);
            b.setTextColor(TEXT);
            b.setAllCaps(false);
            b.setTextSize(12);
            b.setPadding(0, 0, 0, 0);
            b.setMinWidth(0);
            b.setMinimumWidth(0);
            b.setMinHeight(0);
            b.setMinimumHeight(0);
            b.setBackground(cardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(10), 0));
            return b;
        }

        private Button actionButton(String text, int accent) {
            Button b = new Button(MainActivity.this);
            b.setText(text);
            b.setTextColor(TEXT);
            b.setAllCaps(false);
            b.setTextSize(10);
            b.setPadding(0, 0, 0, 0);
            b.setBackground(cardBg(Color.rgb(19, 24, 38), accent, dp(12), 1));
            return b;
        }

        private void configureWebView(WebView w) {
            WebSettings s = w.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setLoadWithOverviewMode(true);
            s.setUseWideViewPort(true);
            s.setSupportZoom(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setJavaScriptCanOpenWindowsAutomatically(true);
            s.setSupportMultipleWindows(true);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(w, true);

            w.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String u = request.getUrl().toString();
                    if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("about:")) return false;
                    openExternal(u);
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    urlBar.setText(url);
                    if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
                    updateHeader();
                }
            });
            w.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onReceivedTitle(WebView view, String titleValue) {
                    pageTitle = titleValue == null ? "" : titleValue;
                    updateHeader();
                }

                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    if (customView != null) {
                        callback.onCustomViewHidden();
                        return;
                    }
                    customView = view;
                    customViewCallback = callback;
                    fullscreenHost.setVisibility(View.VISIBLE);
                    fullscreenHost.addView(view, new FrameLayout.LayoutParams(-1, -1));
                    root.setVisibility(View.GONE);
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }

                @Override
                public void onHideCustomView() {
                    exitFullscreenVideo();
                }

                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    runOnUiThread(() -> {
                        List<String> grants = new ArrayList<>();
                        for (String r : request.getResources()) {
                            if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)
                                    || PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)
                                    || PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) {
                                grants.add(r);
                            }
                        }
                        if (grants.isEmpty()) request.deny();
                        else request.grant(grants.toArray(new String[0]));
                    });
                }
            });
        }

        void loadDefault() { loadUrl(defaultUrl); }

        void loadUrl(String raw) {
            String u = normalizeUrl(raw);
            urlBar.setText(u);
            if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
            webView.loadUrl(u);
        }

        String currentUrl() {
            String current = webView.getUrl();
            if (current == null || current.trim().isEmpty()) current = urlBar.getText().toString();
            return normalizeUrl(current);
        }

        String displayTitle() {
            String t = pageTitle == null ? "" : pageTitle.trim();
            if (t.length() > 28) t = t.substring(0, 25) + "...";
            if (t.isEmpty() || t.equals("about:blank")) t = initialName;
            return "Janela " + number + "  " + t;
        }

        void updateHeader() {
            if (title != null) title.setText(displayTitle());
        }

        void toggleMute() {
            muted = !muted;
            String js = "javascript:(function(){var x=document.querySelectorAll('video,audio');for(var i=0;i<x.length;i++){x[i].muted=" + muted + ";}})()";
            try {
                webView.evaluateJavascript(js, null);
                muteButton.setText(muted ? "Mudo" : "Som");
                setButtonIcon(muteButton, muted ? R.drawable.ic_hs_mute : R.drawable.ic_hs_volume, muted ? ICON_ACTIVE : ICON_NORMAL, 15);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Mute depende do player do site", Toast.LENGTH_SHORT).show();
            }
        }

        void cycleVolume() {
            String js = "javascript:(function(){var x=document.querySelectorAll('video,audio');for(var i=0;i<x.length;i++){x[i].volume=0.45;x[i].muted=false;}})()";
            try {
                webView.evaluateJavascript(js, null);
                muted = false;
                muteButton.setText("45%");
                setButtonIcon(muteButton, R.drawable.ic_hs_volume, ICON_ACTIVE, 15);
                Toast.makeText(MainActivity.this, "Volume HTML5 em 45% quando o site permitir", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Volume depende do player do site", Toast.LENGTH_SHORT).show();
            }
        }

        void toggleDesktopMode() {
            desktopMode = !desktopMode;
            WebSettings settings = webView.getSettings();
            if (desktopMode) {
                settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36 HubSyncBr/0.5.1");
                Toast.makeText(MainActivity.this, "Modo desktop", Toast.LENGTH_SHORT).show();
            } else {
                settings.setUserAgentString(mobileUa);
                Toast.makeText(MainActivity.this, "Modo mobile", Toast.LENGTH_SHORT).show();
            }
            webView.reload();
        }
    }

    private void exitFullscreenVideo() {
        if (customView == null) return;
        fullscreenHost.removeView(customView);
        fullscreenHost.setVisibility(View.GONE);
        customView = null;
        root.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        customViewCallback = null;
        if (focusMode) exitFocus();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            exitFullscreenVideo();
            return;
        }
        if (focusMode) {
            exitFocus();
            return;
        }
        if (!visiblePanes.isEmpty()) {
            StreamPane first = visiblePanes.get(0);
            if (first.webView.canGoBack()) {
                first.webView.goBack();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (StreamPane pane : panes) {
            try { pane.webView.destroy(); } catch (Exception ignored) {}
        }
    }
}
