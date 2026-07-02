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
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
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

    private LinearLayout root;
    private LinearLayout shell;
    private LinearLayout sidebar;
    private LinearLayout mainArea;
    private LinearLayout topBar;
    private LinearLayout dualContainer;
    private LinearLayout emptyState;
    private StreamPane paneA;
    private StreamPane paneB;

    private boolean verticalSplit = false;
    private boolean focusMode = false;
    private boolean sidebarCollapsed = false;
    private int sizeMode = 0; // 0 = igual, 1 = A maior, 2 = B maior

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

        dualContainer = new LinearLayout(this);
        dualContainer.setOrientation(LinearLayout.HORIZONTAL);
        mainArea.addView(dualContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        paneA = new StreamPane(this, "Screen 1", "YouTube", "https://www.youtube.com", PURPLE);
        paneB = new StreamPane(this, "Screen 2", "Twitch", "https://www.twitch.tv", BLUE);
        emptyState = createEmptyState();

        paneA.loadDefault();
        paneB.loadDefault();

        // Em telas estreitas/retrato, começa com a sidebar recolhida para dar mais espaço ao núcleo.
        if (getResources().getConfiguration().screenWidthDp < 700) {
            sidebarCollapsed = true;
            sidebar.setVisibility(View.GONE);
            mainArea.setPadding(0, 0, 0, 0);
        }

        updatePanesLayout();

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
        side.addView(navButton("Multi Screen", R.drawable.ic_hs_grid, false, v -> { if (focusMode) exitFocus(); updatePanesLayout(); }));
        side.addView(navButton("Favorites", R.drawable.ic_hs_heart, false, v -> Toast.makeText(this, "Favoritos entram na próxima etapa", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Sports", R.drawable.ic_hs_sports, false, v -> Toast.makeText(this, "Modo esportes entra depois", Toast.LENGTH_SHORT).show()));
        side.addView(navButton("Browser", R.drawable.ic_hs_browser, false, v -> addScreen()));
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
        TextView title = label("Dual Screen Mode  •", 14, TEXT, true);
        title.setGravity(Gravity.CENTER);
        TextView desc = label("2 Active Streams", 12, MUTED, false);
        desc.setGravity(Gravity.CENTER);
        status.addView(icon, new LinearLayout.LayoutParams(-1, dp(36)));
        status.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));
        status.addView(desc, new LinearLayout.LayoutParams(-1, dp(24)));
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
        TextView title = label("Dual Screen", 20, TEXT, true);
        setLeftIcon(title, R.drawable.ic_hs_grid, ICON_BLUE, 21);
        TextView desc = label("Watch two official web streams at the same time", 12, MUTED, false);
        titles.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));
        titles.addView(desc, new LinearLayout.LayoutParams(-1, dp(24)));
        bar.addView(titles, new LinearLayout.LayoutParams(0, -1, 1));

        Button add = chip("Tela", BLUE);
        setButtonIcon(add, R.drawable.ic_hs_plus, ICON_ACTIVE, 17);
        add.setOnClickListener(v -> addScreen());
        bar.addView(add, new LinearLayout.LayoutParams(dp(92), dp(42)));

        Button swap = chip("Swap", PURPLE);
        setButtonIcon(swap, R.drawable.ic_hs_swap, ICON_ACTIVE, 17);
        swap.setOnClickListener(v -> swapScreens());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(112), dp(42));
        slp.setMargins(dp(8), 0, 0, 0);
        bar.addView(swap, slp);

        Button size = chip("Size", BLUE);
        setButtonIcon(size, R.drawable.ic_hs_resize, ICON_NORMAL, 17);
        size.setOnClickListener(v -> cycleSizeMode());
        LinearLayout.LayoutParams zlp = new LinearLayout.LayoutParams(dp(92), dp(42));
        zlp.setMargins(dp(8), 0, 0, 0);
        bar.addView(size, zlp);

        Button layout = chip("", BLUE);
        setButtonIcon(layout, R.drawable.ic_hs_layout, ICON_NORMAL, 19);
        layout.setOnClickListener(v -> toggleSplitOrientation());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(58), dp(42));
        lp.setMargins(dp(8), 0, 0, 0);
        bar.addView(layout, lp);

        Button more = chip("", MUTED);
        setButtonIcon(more, R.drawable.ic_hs_more, ICON_NORMAL, 20);
        more.setOnClickListener(v -> showMoreDialog());
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
        TextView title = label("Adicionar tela", 22, TEXT, true);
        title.setGravity(Gravity.CENTER);
        TextView desc = label("Abra um site, use como navegador ou combine com outra transmissão.", 13, MUTED, false);
        desc.setGravity(Gravity.CENTER);
        Button add = chip("Nova tela", PURPLE);
        setButtonIcon(add, R.drawable.ic_hs_plus, ICON_ACTIVE, 18);
        add.setOnClickListener(v -> addScreen());

        box.addView(plus, new LinearLayout.LayoutParams(-1, dp(62)));
        box.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        box.addView(desc, new LinearLayout.LayoutParams(-1, dp(36)));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(160), dp(44));
        alp.setMargins(0, dp(18), 0, 0);
        box.addView(add, alp);
        return box;
    }

    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        sidebar.setVisibility(sidebarCollapsed ? View.GONE : View.VISIBLE);
        mainArea.setPadding(sidebarCollapsed ? 0 : dp(12), 0, 0, 0);
        Toast.makeText(this, sidebarCollapsed ? "Menu recolhido" : "Menu aberto", Toast.LENGTH_SHORT).show();
    }

    private void toggleSplitOrientation() {
        verticalSplit = !verticalSplit;
        updatePanesLayout();
        Toast.makeText(this, verticalSplit ? "Layout vertical ativado" : "Layout horizontal ativado", Toast.LENGTH_SHORT).show();
    }

    private void cycleSizeMode() {
        sizeMode = (sizeMode + 1) % 3;
        updatePanesLayout();
        String msg = sizeMode == 0 ? "Telas 50/50" : sizeMode == 1 ? "Tela A maior" : "Tela B maior";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void updatePanesLayout() {
        if (focusMode || dualContainer == null) return;
        dualContainer.removeAllViews();
        dualContainer.setOrientation(verticalSplit ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        int activeCount = (paneA.active ? 1 : 0) + (paneB.active ? 1 : 0);
        if (activeCount == 0) {
            dualContainer.addView(emptyState, new LinearLayout.LayoutParams(-1, -1));
            return;
        }

        if (paneA.active) paneA.container.setVisibility(View.VISIBLE);
        if (paneB.active) paneB.container.setVisibility(View.VISIBLE);

        if (activeCount == 1) {
            StreamPane only = paneA.active ? paneA : paneB;
            dualContainer.addView(only.container, new LinearLayout.LayoutParams(-1, -1));
            return;
        }

        float weightA = 1f;
        float weightB = 1f;
        if (sizeMode == 1) { weightA = 1.7f; weightB = 1f; }
        if (sizeMode == 2) { weightA = 1f; weightB = 1.7f; }

        dualContainer.addView(paneA.container, new LinearLayout.LayoutParams(0, -1, weightA));
        View spacer = new View(this);
        spacer.setTag("split-spacer");
        LinearLayout.LayoutParams sp = verticalSplit ? new LinearLayout.LayoutParams(-1, dp(10)) : new LinearLayout.LayoutParams(dp(10), -1);
        dualContainer.addView(spacer, sp);
        dualContainer.addView(paneB.container, new LinearLayout.LayoutParams(0, -1, weightB));
    }

    private void addScreen() {
        if (!paneA.active) {
            activatePane(paneA);
            return;
        }
        if (!paneB.active) {
            activatePane(paneB);
            return;
        }
        Toast.makeText(this, "Limite da versão grátis: 2 telas. Mais telas entram depois.", Toast.LENGTH_LONG).show();
    }

    private void activatePane(StreamPane pane) {
        if (focusMode) exitFocus();
        pane.active = true;
        pane.container.setVisibility(View.VISIBLE);
        if (pane.isBlank()) pane.loadUrl("https://www.google.com");
        updatePanesLayout();
    }

    private void closePane(StreamPane pane) {
        if (focusMode) exitFocus();
        pane.active = false;
        try { pane.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
        updatePanesLayout();
    }

    private void closeAllPanes() {
        if (focusMode) exitFocus();
        paneA.active = false;
        paneB.active = false;
        try { paneA.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
        try { paneB.webView.loadUrl("about:blank"); } catch (Exception ignored) {}
        updatePanesLayout();
    }

    private void swapScreens() {
        if (!paneA.active || !paneB.active) {
            Toast.makeText(this, "Abra duas telas para trocar", Toast.LENGTH_SHORT).show();
            return;
        }
        String a = paneA.currentUrl();
        String b = paneB.currentUrl();
        paneA.loadUrl(b);
        paneB.loadUrl(a);
    }

    private void enterFocus(StreamPane pane) {
        if (!pane.active) return;
        focusMode = true;
        sidebar.setVisibility(View.GONE);
        topBar.setVisibility(View.GONE);
        if (pane == paneA) {
            paneB.container.setVisibility(View.GONE);
            paneA.focusButton.setText("↙ Voltar");
        } else {
            paneA.container.setVisibility(View.GONE);
            paneB.focusButton.setText("↙ Voltar");
        }
        View spacer = dualContainer.findViewWithTag("split-spacer");
        if (spacer != null) spacer.setVisibility(View.GONE);
    }

    private void exitFocus() {
        focusMode = false;
        sidebar.setVisibility(sidebarCollapsed ? View.GONE : View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        paneA.focusButton.setText("Foco");
        paneB.focusButton.setText("Foco");
        updatePanesLayout();
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
        bg.setStroke(dp(strokeWidth), stroke);
        return bg;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
                .setTitle("HubSyncBr 0.3 — Icon Pack")
                .setMessage("Atualização visual com pacote de ícones roxo/azul estilo premium nos botões pequenos, sidebar, topbar e controles das janelas. As funções principais da 0.2 foram mantidas.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showMoreDialog() {
        String[] items = new String[]{"Adicionar tela", "Recolher/abrir menu", "Ajustar tamanho", "Recarregar telas", "Fechar todas", "Sobre", "Aviso legal"};
        new AlertDialog.Builder(this)
                .setTitle("HubSyncBr")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: addScreen(); break;
                        case 1: toggleSidebar(); break;
                        case 2: cycleSizeMode(); break;
                        case 3:
                            if (paneA.active) paneA.webView.reload();
                            if (paneB.active) paneB.webView.reload();
                            break;
                        case 4: closeAllPanes(); break;
                        case 5: showAboutDialog(); break;
                        case 6: showLegalDialog(null); break;
                    }
                })
                .show();
    }

    private class StreamPane {
        final LinearLayout container;
        final TextView title;
        final EditText urlBar;
        final WebView webView;
        final Button focusButton;
        final Button muteButton;
        boolean active = true;
        boolean desktopMode = false;
        boolean muted = false;
        final String defaultUrl;
        final String mobileUa;

        StreamPane(Context ctx, String slot, String name, String url, int accent) {
            defaultUrl = url;
            webView = new WebView(ctx);

            container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp(6), dp(6), dp(6), dp(6));
            container.setBackground(cardBg(Color.rgb(10, 13, 23), accent, dp(12), 1));

            LinearLayout header = new LinearLayout(ctx);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(8), 0, dp(8), 0);
            title = label(slot + "   " + name, 13, TEXT, true);
            header.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
            TextView close = label("", 22, MUTED, true);
            setCenterIcon(close, R.drawable.ic_hs_close, ICON_NORMAL, 20);
            close.setGravity(Gravity.CENTER);
            close.setOnClickListener(v -> closePane(this));
            header.addView(close, new LinearLayout.LayoutParams(dp(36), -1));
            container.addView(header, new LinearLayout.LayoutParams(-1, dp(38)));

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
            urlBar.setHint("Digite URL ou pesquisa");
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
            toolbar.addView(go, new LinearLayout.LayoutParams(dp(54), dp(42)));
            container.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));

            configureWebView(webView);
            mobileUa = webView.getSettings().getUserAgentString();
            container.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

            LinearLayout controls = new LinearLayout(ctx);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER_VERTICAL);
            controls.setPadding(dp(4), dp(6), dp(4), 0);
            focusButton = actionButton("Foco", accent);
            setButtonIcon(focusButton, R.drawable.ic_hs_focus, ICON_ACTIVE, 16);
            focusButton.setOnClickListener(v -> { if (focusMode) exitFocus(); else enterFocus(this); });
            controls.addView(focusButton, new LinearLayout.LayoutParams(0, dp(42), 1));

            muteButton = actionButton("Som", accent);
            setButtonIcon(muteButton, R.drawable.ic_hs_volume, ICON_NORMAL, 16);
            muteButton.setOnClickListener(v -> toggleMute());
            controls.addView(muteButton, new LinearLayout.LayoutParams(0, dp(42), 1));

            Button desktop = actionButton("Desktop", accent);
            setButtonIcon(desktop, R.drawable.ic_hs_desktop, ICON_NORMAL, 16);
            desktop.setOnClickListener(v -> toggleDesktopMode());
            controls.addView(desktop, new LinearLayout.LayoutParams(0, dp(42), 1));

            Button external = actionButton("Externo", accent);
            setButtonIcon(external, R.drawable.ic_hs_external, ICON_NORMAL, 16);
            external.setOnClickListener(v -> openExternal(currentUrl()));
            controls.addView(external, new LinearLayout.LayoutParams(0, dp(42), 1));
            container.addView(controls, new LinearLayout.LayoutParams(-1, dp(50)));
        }

        private Button miniButton(String text) {
            Button b = new Button(MainActivity.this);
            b.setText(text);
            b.setTextColor(TEXT);
            b.setAllCaps(false);
            b.setTextSize(13);
            b.setPadding(0, 0, 0, 0);
            b.setBackground(cardBg(Color.rgb(17, 22, 35), Color.rgb(36, 43, 64), dp(12), 1));
            return b;
        }

        private Button actionButton(String text, int accent) {
            Button b = new Button(MainActivity.this);
            b.setText(text);
            b.setTextColor(TEXT);
            b.setAllCaps(false);
            b.setTextSize(11);
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
                }
            });
            w.setWebChromeClient(new WebChromeClient() {
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

        boolean isBlank() {
            String current = webView.getUrl();
            return current == null || current.trim().isEmpty() || current.equals("about:blank");
        }

        void toggleMute() {
            muted = !muted;
            String js = "javascript:(function(){var x=document.querySelectorAll('video,audio');for(var i=0;i<x.length;i++){x[i].muted=" + muted + ";}})()";
            try {
                webView.evaluateJavascript(js, null);
                muteButton.setText(muted ? "Mudo" : "Som");
                setButtonIcon(muteButton, muted ? R.drawable.ic_hs_mute : R.drawable.ic_hs_volume, muted ? ICON_ACTIVE : ICON_NORMAL, 16);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Mute depende do player do site", Toast.LENGTH_SHORT).show();
            }
        }

        void toggleDesktopMode() {
            desktopMode = !desktopMode;
            WebSettings settings = webView.getSettings();
            if (desktopMode) {
                settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36 HubSyncBr/0.3");
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
        if (paneA != null && paneA.active && paneA.webView.canGoBack()) {
            paneA.webView.goBack();
            return;
        }
        if (paneB != null && paneB.active && paneB.webView.canGoBack()) {
            paneB.webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (paneA != null) paneA.webView.destroy(); } catch (Exception ignored) {}
        try { if (paneB != null) paneB.webView.destroy(); } catch (Exception ignored) {}
    }
}
