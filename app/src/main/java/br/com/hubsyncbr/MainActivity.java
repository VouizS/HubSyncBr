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
        mainArea.setPadding(dp(12), 0, 0, 0);
        shell.addView(mainArea, new LinearLayout.LayoutParams(0, -1, 1));

        topBar = createTopBar();
        mainArea.addView(topBar, new LinearLayout.LayoutParams(-1, dp(58)));

        windowContainer = new LinearLayout(this);
        windowContainer.setOrientation(LinearLayout.HORIZONTAL);
        mainArea.addView(windowContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        emptyState = createEmptyState();

        StreamPane a = createPane("YouTube", "https://www.youtube.com", PURPLE);
        StreamPane b = createPane("Twitch", "https://www.twitch.tv", BLUE);
        a.loadDefault();
        b.loadDefault();
        showPane(a);
        showPane(b);

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

        side.addView(navButton("Home", R.drawable.ic_hs_home, true, v -> Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Multi Screen", R.drawable.ic_hs_grid, false, v -> { if (focusMode) exitFocus(); updateWindowLayout(); }));
        side.addView(navButton("Favorites", R.drawable.ic_hs_heart, false, v -> Toast.makeText(this, "Favoritos entram depois", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Sports", R.drawable.ic_hs_sports, false, v -> Toast.makeText(this, "Modo esportes entra depois", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Browser", R.drawable.ic_hs_browser, false, v -> addWindow()));
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
        bar.setPadding(0, 0, 0, dp(8));

        Button menu = chip("", MUTED);
        setButtonIcon(menu, R.drawable.ic_hs_menu, ICON_NORMAL, 20);
        menu.setOnClickListener(v -> toggleSidebar());
        bar.addView(menu, new LinearLayout.LayoutParams(dp(52), dp(42)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(8), 0, 0, 0);
        headerTitle = label("Hub View", 20, TEXT, true);
        setLeftIcon(headerTitle, R.drawable.ic_hs_grid, ICON_BLUE, 21);
        headerDesc = label("Gerencie janelas web, multitela e grupos futuros", 12, MUTED, false);
        titles.addView(headerTitle, new LinearLayout.LayoutParams(-1, dp(28)));
        titles.addView(headerDesc, new LinearLayout.LayoutParams(-1, dp(24)));
        bar.addView(titles, new LinearLayout.LayoutParams(0, -1, 1));

        Button add = chip("Janela", BLUE);
        setButtonIcon(add, R.drawable.ic_hs_plus, ICON_ACTIVE, 17);
        add.setOnClickListener(v -> addWindow());
        bar.addView(add, new LinearLayout.LayoutParams(dp(104), dp(42)));

        Button swap = chip("Swap", PURPLE);
        setButtonIcon(swap, R.drawable.ic_hs_swap, ICON_ACTIVE, 17);
        swap.setOnClickListener(v -> swapFirstTwoVisible());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(108), dp(42));
        slp.setMargins(dp(8), 0, 0, 0);
        bar.addView(swap, slp);

        Button size = chip("Layout", BLUE);
        setButtonIcon(size, R.drawable.ic_hs_resize, ICON_NORMAL, 17);
        size.setOnClickListener(v -> cycleLayoutMode());
        LinearLayout.LayoutParams zlp = new LinearLayout.LayoutParams(dp(104), dp(42));
        zlp.setMargins(dp(8), 0, 0, 0);
        bar.addView(size, zlp);

        Button manager = chip("", BLUE);
        setButtonIcon(manager, R.drawable.ic_hs_layout, ICON_NORMAL, 19);
        manager.setOnClickListener(v -> showWindowManager());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(58), dp(42));
        lp.setMargins(dp(8), 0, 0, 0);
        bar.addView(manager, lp);

        Button more = chip("", MUTED);
        setButtonIcon(more, R.drawable.ic_hs_more, ICON_NORMAL, 20);
        more.setOnClickListener(v -> showMorePanel());
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(dp(58), dp(42));
        mlp.setMargins(dp(8), 0, 0, 0);
        bar.addView(more, mlp);
        return bar;
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

    private StreamPane createPane(String name, String url, int accent) {
        StreamPane pane = new StreamPane(this, nextPaneNumber++, name, url, accent);
        panes.add(pane);
        return pane;
    }

    private int accentForIndex(int index) {
        int[] accents = new int[]{PURPLE, BLUE, Color.rgb(34, 211, 238), Color.rgb(168, 85, 247), Color.rgb(14, 165, 233), Color.rgb(99, 102, 241)};
        return accents[Math.abs(index) % accents.length];
    }

    private void addWindow() {
        if (panes.size() >= MAX_OPEN_WINDOWS) {
            Toast.makeText(this, "Limite técnico desta versão: " + MAX_OPEN_WINDOWS + " janelas abertas", Toast.LENGTH_LONG).show();
            return;
        }
        StreamPane pane = createPane("Web", "https://www.google.com", accentForIndex(panes.size()));
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
        if (!visiblePanes.contains(pane)) {
            if (visiblePanes.size() >= MAX_VISIBLE_WINDOWS) {
                Toast.makeText(this, "Máximo visível agora: 4 janelas", Toast.LENGTH_SHORT).show();
                return;
            }
            visiblePanes.add(pane);
        }
        pane.visible = true;
        updateWindowLayout();
    }

    private void hidePane(StreamPane pane) {
        if (focusMode) exitFocus();
        visiblePanes.remove(pane);
        pane.visible = false;
        updateWindowLayout();
    }

    private void closePane(StreamPane pane) {
        if (focusMode) exitFocus();
        visiblePanes.remove(pane);
        panes.remove(pane);
        detach(pane.container);
        try { pane.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
        try { pane.webView.destroy(); } catch (Exception ignored) {}
        updateWindowNumbers();
        updateWindowLayout();
    }

    private void closeAllPanes() {
        if (focusMode) exitFocus();
        for (StreamPane pane : new ArrayList<>(panes)) {
            detach(pane.container);
            try { pane.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
            try { pane.webView.destroy(); } catch (Exception ignored) {}
        }
        panes.clear();
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
        windowContainer.removeAllViews();
        detach(emptyState);
        for (StreamPane pane : panes) detach(pane.container);

        if (visiblePanes.isEmpty()) {
            windowContainer.setOrientation(LinearLayout.VERTICAL);
            windowContainer.addView(emptyState, new LinearLayout.LayoutParams(-1, -1));
            updateStatusText();
            return;
        }

        if (visiblePanes.size() == 1) {
            windowContainer.setOrientation(LinearLayout.VERTICAL);
            windowContainer.addView(visiblePanes.get(0).container, new LinearLayout.LayoutParams(-1, -1));
            updateStatusText();
            return;
        }

        if (visiblePanes.size() <= 2 && layoutMode != 3) {
            renderTwoPaneLayout();
        } else {
            renderGridLayout();
        }
        updateStatusText();
    }

    private void renderTwoPaneLayout() {
        windowContainer.setOrientation(verticalSplit ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        StreamPane first = visiblePanes.get(0);
        StreamPane second = visiblePanes.get(1);
        float weightA = layoutMode == 1 ? 1.7f : layoutMode == 2 ? 1f : 1f;
        float weightB = layoutMode == 1 ? 1f : layoutMode == 2 ? 1.7f : 1f;

        if (verticalSplit) {
            windowContainer.addView(first.container, new LinearLayout.LayoutParams(-1, 0, weightA));
            windowContainer.addView(spacer(true), new LinearLayout.LayoutParams(-1, dp(10)));
            windowContainer.addView(second.container, new LinearLayout.LayoutParams(-1, 0, weightB));
        } else {
            windowContainer.addView(first.container, new LinearLayout.LayoutParams(0, -1, weightA));
            windowContainer.addView(spacer(false), new LinearLayout.LayoutParams(dp(10), -1));
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
            windowContainer.addView(gap, new LinearLayout.LayoutParams(-1, dp(10)));
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
        if (row.getChildCount() > 0) row.addView(spacer(false), new LinearLayout.LayoutParams(dp(10), -1));
        row.addView(pane.container, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private View spacer(boolean horizontalLine) {
        View v = new View(this);
        v.setTag("split-spacer");
        return v;
    }

    private void updateStatusText() {
        if (screenStatusDesc != null) screenStatusDesc.setText(visiblePanes.size() + " visíveis / " + panes.size() + " abertas");
        if (screenStatusTitle != null) screenStatusTitle.setText(visiblePanes.size() >= 4 ? "Grid Mode  •" : "Hub View  •");
        if (headerTitle != null) headerTitle.setText(visiblePanes.size() >= 4 ? "Grid View" : "Hub View");
        if (headerDesc != null) {
            if (visiblePanes.size() == 0) headerDesc.setText("Adicione uma janela para começar");
            else if (visiblePanes.size() == 1) headerDesc.setText("Uma janela web ativa no núcleo");
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
            p.focusButton.setText(p == pane ? "Voltar" : "Foco");
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
        for (StreamPane p : panes) p.focusButton.setText("Foco");
        updateWindowLayout();
    }

    private void showWindowManager() {
        final AlertDialog[] holder = new AlertDialog[1];
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = dialogPanel();
        TextView title = label("Janelas abertas", 22, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_layout, ICON_BLUE, 22);
        panel.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        TextView info = label(panes.size() + " abertas • " + visiblePanes.size() + " visíveis • limite visível atual: 4", 13, MUTED, false);
        panel.addView(info, new LinearLayout.LayoutParams(-1, dp(30)));

        Button add = panelButton("Nova janela", R.drawable.ic_hs_plus, BLUE);
        add.setOnClickListener(v -> { if (holder[0] != null) holder[0].dismiss(); addWindow(); });
        panel.addView(add, new LinearLayout.LayoutParams(-1, dp(46)));

        if (panes.isEmpty()) {
            TextView empty = label("Nenhuma janela aberta ainda.", 15, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(80)));
        } else {
            for (StreamPane pane : new ArrayList<>(panes)) {
                panel.addView(windowCard(pane, holder), new LinearLayout.LayoutParams(-1, dp(106)));
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
        card.setPadding(dp(12), dp(8), dp(12), dp(8));
        card.setBackground(cardBg(Color.rgb(14, 18, 30), visiblePanes.contains(pane) ? pane.accent : Color.rgb(41, 48, 66), dp(14), 1));

        TextView title = label(pane.displayTitle(), 15, TEXT, true);
        setLeftIcon(title, visiblePanes.contains(pane) ? R.drawable.ic_hs_grid : R.drawable.ic_hs_browser, visiblePanes.contains(pane) ? ICON_BLUE : ICON_NORMAL, 18);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));

        TextView url = label(shortUrl(pane.currentUrl()), 11, MUTED, false);
        card.addView(url, new LinearLayout.LayoutParams(-1, dp(22)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button show = panelSmallButton(visiblePanes.contains(pane) ? "Ocultar" : "Mostrar", visiblePanes.contains(pane) ? R.drawable.ic_hs_close : R.drawable.ic_hs_plus, pane.accent);
        show.setOnClickListener(v -> {
            if (holder[0] != null) holder[0].dismiss();
            if (visiblePanes.contains(pane)) hidePane(pane); else showPane(pane);
        });
        actions.addView(show, new LinearLayout.LayoutParams(0, dp(38), 1));

        Button focus = panelSmallButton("Foco", R.drawable.ic_hs_focus, pane.accent);
        focus.setOnClickListener(v -> {
            if (holder[0] != null) holder[0].dismiss();
            if (!visiblePanes.contains(pane)) showPane(pane);
            enterFocus(pane);
        });
        actions.addView(focus, new LinearLayout.LayoutParams(0, dp(38), 1));

        Button close = panelSmallButton("Fechar", R.drawable.ic_hs_close, pane.accent);
        close.setOnClickListener(v -> { if (holder[0] != null) holder[0].dismiss(); closePane(pane); });
        actions.addView(close, new LinearLayout.LayoutParams(0, dp(38), 1));
        card.addView(actions, new LinearLayout.LayoutParams(-1, dp(42)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(106));
        cardParams.setMargins(0, dp(8), 0, 0);
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
        panel.addView(dialogAction("Gerenciar janelas", R.drawable.ic_hs_layout, () -> { if (holder[0] != null) holder[0].dismiss(); showWindowManager(); }));
        panel.addView(dialogAction("Recolher/abrir menu", R.drawable.ic_hs_menu, () -> { if (holder[0] != null) holder[0].dismiss(); toggleSidebar(); }));
        panel.addView(dialogAction("Trocar layout", R.drawable.ic_hs_resize, () -> { if (holder[0] != null) holder[0].dismiss(); cycleLayoutMode(); }));
        panel.addView(dialogAction("Recarregar visíveis", R.drawable.ic_hs_reload, () -> { if (holder[0] != null) holder[0].dismiss(); reloadVisible(); }));
        panel.addView(dialogAction("Fechar todas", R.drawable.ic_hs_close, () -> { if (holder[0] != null) holder[0].dismiss(); closeAllPanes(); }));
        panel.addView(dialogAction("Sobre", R.drawable.ic_hs_settings, () -> { if (holder[0] != null) holder[0].dismiss(); showAboutDialog(); }));
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

    private String normalizeUrl(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "https://www.google.com";
        String u = raw.trim();
        if (u.equals("about:blank")) return u;
        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file://")) return u;
        boolean looksLikeUrl = u.contains(".") && !u.contains(" ");
        if (looksLikeUrl) return "https://" + u;
        try {
            return "https://www.google.com/search?q=" + URLEncoder.encode(u, "UTF-8");
        } catch (Exception e) {
            return "https://www.google.com/search?q=" + u.replace(" ", "+");
        }
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
                .setTitle("HubSyncBr 0.4 — Window Manager")
                .setMessage("Atualização maior: gerenciador de janelas, até 4 janelas visíveis, até 8 abertas/minimizadas, popup visual melhorado, botão + Janela, modo grade 2x2 e base para grupos estilo Chrome.")
                .setPositiveButton("OK", null)
                .show();
    }

    private class StreamPane {
        final LinearLayout container;
        final TextView title;
        final EditText urlBar;
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
            container.setPadding(dp(6), dp(6), dp(6), dp(6));
            container.setBackground(cardBg(Color.rgb(10, 13, 23), accent, dp(12), 1));

            LinearLayout header = new LinearLayout(ctx);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(8), 0, dp(8), 0);
            title = label("", 13, TEXT, true);
            header.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
            TextView close = label("", 22, MUTED, true);
            setCenterIcon(close, R.drawable.ic_hs_close, ICON_NORMAL, 20);
            close.setGravity(Gravity.CENTER);
            close.setOnClickListener(v -> closePane(this));
            header.addView(close, new LinearLayout.LayoutParams(dp(36), -1));
            container.addView(header, new LinearLayout.LayoutParams(-1, dp(34)));
            updateHeader();

            View accentLine = new View(ctx);
            accentLine.setBackgroundColor(accent);
            container.addView(accentLine, new LinearLayout.LayoutParams(-1, dp(3)));

            LinearLayout toolbar = new LinearLayout(ctx);
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            toolbar.setGravity(Gravity.CENTER_VERTICAL);
            toolbar.setPadding(dp(4), dp(5), dp(4), dp(5));
            toolbar.setBackgroundColor(Color.rgb(11, 15, 25));

            Button back = miniButton("");
            setButtonIcon(back, R.drawable.ic_hs_back, ICON_NORMAL, 18);
            back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
            toolbar.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));

            Button forward = miniButton("");
            setButtonIcon(forward, R.drawable.ic_hs_forward, ICON_NORMAL, 18);
            forward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
            toolbar.addView(forward, new LinearLayout.LayoutParams(dp(42), dp(42)));

            Button reload = miniButton("");
            setButtonIcon(reload, R.drawable.ic_hs_reload, ICON_NORMAL, 18);
            reload.setOnClickListener(v -> webView.reload());
            toolbar.addView(reload, new LinearLayout.LayoutParams(dp(42), dp(42)));

            urlBar = new EditText(ctx);
            urlBar.setSingleLine(true);
            urlBar.setText(url);
            urlBar.setTextSize(12);
            urlBar.setTextColor(TEXT);
            urlBar.setHintTextColor(MUTED);
            urlBar.setHint("URL ou pesquisa");
            urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
            urlBar.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
            urlBar.setPadding(dp(12), 0, dp(12), 0);
            urlBar.setBackground(cardBg(Color.rgb(18, 22, 32), Color.rgb(36, 43, 64), dp(24), 1));
            urlBar.setSelectAllOnFocus(true);
            urlBar.setOnEditorActionListener((v, actionId, event) -> {
                boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
                if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                    loadUrl(urlBar.getText().toString());
                    return true;
                }
                return false;
            });
            toolbar.addView(urlBar, new LinearLayout.LayoutParams(0, dp(42), 1));

            Button go = miniButton("Go");
            go.setOnClickListener(v -> loadUrl(urlBar.getText().toString()));
            toolbar.addView(go, new LinearLayout.LayoutParams(dp(50), dp(42)));
            container.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));

            configureWebView(webView);
            mobileUa = webView.getSettings().getUserAgentString();
            container.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

            LinearLayout controls = new LinearLayout(ctx);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER_VERTICAL);
            controls.setPadding(dp(4), dp(6), dp(4), 0);
            focusButton = actionButton("Foco", accent);
            setButtonIcon(focusButton, R.drawable.ic_hs_focus, ICON_ACTIVE, 15);
            focusButton.setOnClickListener(v -> { if (focusMode) exitFocus(); else enterFocus(this); });
            controls.addView(focusButton, new LinearLayout.LayoutParams(0, dp(40), 1));

            muteButton = actionButton("Som", accent);
            setButtonIcon(muteButton, R.drawable.ic_hs_volume, ICON_NORMAL, 15);
            muteButton.setOnClickListener(v -> toggleMute());
            muteButton.setOnLongClickListener(v -> { cycleVolume(); return true; });
            controls.addView(muteButton, new LinearLayout.LayoutParams(0, dp(40), 1));

            Button desktop = actionButton("Desk", accent);
            setButtonIcon(desktop, R.drawable.ic_hs_desktop, ICON_NORMAL, 15);
            desktop.setOnClickListener(v -> toggleDesktopMode());
            controls.addView(desktop, new LinearLayout.LayoutParams(0, dp(40), 1));

            Button external = actionButton("Ext", accent);
            setButtonIcon(external, R.drawable.ic_hs_external, ICON_NORMAL, 15);
            external.setOnClickListener(v -> openExternal(currentUrl()));
            controls.addView(external, new LinearLayout.LayoutParams(0, dp(40), 1));
            container.addView(controls, new LinearLayout.LayoutParams(-1, dp(48)));
        }

        private Button miniButton(String text) {
            Button b = new Button(MainActivity.this);
            b.setText(text);
            b.setTextColor(TEXT);
            b.setAllCaps(false);
            b.setTextSize(12);
            b.setPadding(0, 0, 0, 0);
            b.setBackground(cardBg(Color.rgb(17, 22, 35), Color.rgb(36, 43, 64), dp(12), 1));
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
                settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36 HubSyncBr/0.4");
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

