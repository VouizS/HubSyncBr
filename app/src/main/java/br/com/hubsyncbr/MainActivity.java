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
        
        String brandLogo = "file:///android_asset/hubsyncbr_mark.png";
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
