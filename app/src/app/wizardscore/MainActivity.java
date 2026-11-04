package app.wizardscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    static final int BG = 0xFF14112B, CARD = 0xFF231E48, CARD2 = 0xFF302A62, GOLD = 0xFFF2C14E,
            PURPLE = 0xFF7B5CD6, TEXT = 0xFFF4F1FF, MUTED = 0xFFA9A3CC, GREEN = 0xFF5FD38D,
            RED = 0xFFFF6B6B, DARK = 0xFF14112B;

    SharedPreferences prefs;
    Game game;
    String screen = "home";

    // setup state
    int setupCount = 4;
    final String[] setupNames = new String[6];
    boolean setupHook = false;
    int setupDealer = 0;

    // round screen live views
    final List<TextView> bidViews = new ArrayList<TextView>();
    final List<TextView> tookViews = new ArrayList<TextView>();
    final List<TextView> noteViews = new ArrayList<TextView>();
    TextView statusView, hookView, primaryBtn;

    interface IntSetter { void set(int v); }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("wizard", MODE_PRIVATE);
        for (int i = 0; i < 6; i++) setupNames[i] = prefs.getString("name" + i, "");
        setupCount = prefs.getInt("count", 4);
        setupHook = prefs.getBoolean("hook", false);
        game = Game.fromJson(prefs.getString("game", null));
        if (game != null && !game.isOver()) showRound(); else showHome();
    }

    @Override
    public void onBackPressed() {
        if (screen.equals("scores")) {
            if (game != null && (game.isOver() || endedEarly)) showOver(); else showRound();
        } else if (screen.equals("over")) {
            leaveOver();
        } else if (screen.equals("round")) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    void save() {
        SharedPreferences.Editor e = prefs.edit();
        e.putString("game", game == null ? null : game.toJson());
        for (int i = 0; i < 6; i++) e.putString("name" + i, setupNames[i] == null ? "" : setupNames[i]);
        e.putInt("count", setupCount);
        e.putBoolean("hook", setupHook);
        e.apply();
    }

    // ------------------------------------------------------------------ helpers

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    GradientDrawable rounded(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    LinearLayout card(LinearLayout parent) {
        LinearLayout c = vbox();
        c.setBackground(rounded(CARD, 16));
        c.setPadding(dp(16), dp(14), dp(16), dp(16));
        LinearLayout.LayoutParams p = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(12);
        parent.addView(c, p);
        return c;
    }

    TextView button(String label, boolean primary, View.OnClickListener l) {
        TextView b = text(label, 16, primary ? DARK : TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(primary ? GOLD : CARD2, 12));
        b.setPadding(dp(14), dp(13), dp(14), dp(13));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    void setEnabled(TextView b, boolean on) {
        b.setEnabled(on);
        b.setAlpha(on ? 1f : 0.4f);
    }

    TextView circleBtn(String label, View.OnClickListener l) {
        TextView b = text(label, 22, TEXT, true);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(CARD2);
        b.setBackground(g);
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    /** A [-] value [+] control. Returns the row; the value label is added to target. */
    LinearLayout stepper(final int initial, final int max, List<TextView> target, final IntSetter setter) {
        LinearLayout row = hbox();
        final TextView value = text(String.valueOf(initial), 24, GOLD, true);
        value.setGravity(Gravity.CENTER);
        final int[] v = {initial};
        TextView minus = circleBtn("−", new View.OnClickListener() {
            public void onClick(View x) {
                if (v[0] > 0) { v[0]--; setter.set(v[0]); }
            }
        });
        TextView plus = circleBtn("+", new View.OnClickListener() {
            public void onClick(View x) {
                if (v[0] < max) { v[0]++; setter.set(v[0]); }
            }
        });
        row.addView(minus, lp(dp(36), dp(36)));
        row.addView(value, lp(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(plus, lp(dp(36), dp(36)));
        target.add(value);
        return row;
    }

    /** Scrollable page with an optional fixed bottom bar. Returns the content column. */
    LinearLayout page(LinearLayout bottomBar) {
        LinearLayout root = vbox();
        root.setBackgroundColor(BG);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout content = vbox();
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(content);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        if (bottomBar != null) {
            bottomBar.setPadding(dp(16), dp(10), dp(16), dp(14));
            bottomBar.setBackgroundColor(0xFF1B1739);
            root.addView(bottomBar);
        }
        setContentView(root);
        return content;
    }

    LinearLayout twoButtons(TextView a, TextView b) {
        LinearLayout bar = hbox();
        if (a != null) {
            LinearLayout.LayoutParams p = weight(1);
            p.rightMargin = dp(10);
            bar.addView(a, p);
        }
        bar.addView(b, weight(a == null ? 1 : 1.4f));
        return bar;
    }

    String signed(int v) { return v > 0 ? "+" + v : (v < 0 ? "−" + (-v) : "0"); }

    String plural(int n, String word) { return n + " " + word + (n == 1 ? "" : "s"); }

    // ------------------------------------------------------------------ home / setup

    void showHome() {
        screen = "home";
        LinearLayout c = page(null);

        LinearLayout head = hbox();
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        head.addView(logo, lp(dp(64), dp(64)));
        LinearLayout titles = vbox();
        titles.setPadding(dp(14), 0, 0, 0);
        titles.addView(text("Wizard Score", 26, TEXT, true));
        titles.addView(text("Bid your tricks. We'll keep the score.", 14, MUTED, false));
        head.addView(titles);
        c.addView(head);

        if (game != null) {
            LinearLayout g = card(c);
            if (game.isOver()) {
                g.addView(text("Last game finished", 18, TEXT, true));
                g.addView(text(Arrays.toString(game.names).replace("[", "").replace("]", ""), 14, MUTED, false));
                TextView res = button("See results", true, new View.OnClickListener() {
                    public void onClick(View v) { showOver(); }
                });
                addSpaced(g, res, 12);
            } else {
                g.addView(text("Game in progress", 18, TEXT, true));
                g.addView(text("Round " + game.cards() + " of " + game.totalRounds() + " · "
                        + Arrays.toString(game.names).replace("[", "").replace("]", ""), 14, MUTED, false));
                TextView res = button("Resume game", true, new View.OnClickListener() {
                    public void onClick(View v) { showRound(); }
                });
                addSpaced(g, res, 12);
            }
        }

        LinearLayout s = card(c);
        s.addView(text("New game", 18, TEXT, true));
        s.addView(spacer(8));
        s.addView(text("Number of players", 14, MUTED, false));
        LinearLayout counts = hbox();
        for (int k = 2; k <= 6; k++) {
            final int kk = k;
            TextView chip = text(String.valueOf(k), 18, k == setupCount ? DARK : TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(rounded(k == setupCount ? GOLD : CARD2, 10));
            chip.setPadding(0, dp(10), 0, dp(10));
            chip.setClickable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setupCount = kk;
                    if (setupDealer >= kk) setupDealer = 0;
                    showHome();
                }
            });
            LinearLayout.LayoutParams p = weight(1);
            if (k < 6) p.rightMargin = dp(8);
            counts.addView(chip, p);
        }
        addSpaced(s, counts, 6);
        int rounds = Game.DECK / setupCount;
        TextView info = text(setupCount + " players → " + rounds + " rounds (1 to " + rounds + " cards each)", 13, GOLD, false);
        addSpaced(s, info, 8);

        s.addView(spacer(12));
        s.addView(text("Player names (in seating order)", 14, MUTED, false));
        for (int i = 0; i < setupCount; i++) {
            final int idx = i;
            EditText et = new EditText(this);
            et.setSingleLine(true);
            et.setHint("Player " + (i + 1));
            et.setHintTextColor(0xFF6E6896);
            et.setTextColor(TEXT);
            et.setText(setupNames[i]);
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            et.setImeOptions(i == setupCount - 1 ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
            et.setBackground(rounded(CARD2, 10));
            et.setPadding(dp(12), dp(10), dp(12), dp(10));
            et.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) {}
                public void afterTextChanged(Editable e) { setupNames[idx] = e.toString(); }
            });
            addSpaced(s, et, 8);
        }

        s.addView(spacer(14));
        LinearLayout dealerRow = hbox();
        LinearLayout dl = vbox();
        dl.addView(text("First dealer", 15, TEXT, true));
        dl.addView(text("Deal passes to the left each round", 12, MUTED, false));
        dealerRow.addView(dl, weight(1));
        final TextView dealerBtn = button(setupName(setupDealer) + "  ▸", false, null);
        dealerBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setupDealer = (setupDealer + 1) % setupCount;
                dealerBtn.setText(setupName(setupDealer) + "  ▸");
            }
        });
        dealerRow.addView(dealerBtn);
        s.addView(dealerRow);

        s.addView(spacer(14));
        LinearLayout hookRow = hbox();
        LinearLayout hl = vbox();
        hl.addView(text("Hook rule", 15, TEXT, true));
        hl.addView(text("Dealer can't bid so total bids equal the cards dealt", 12, MUTED, false));
        hookRow.addView(hl, weight(1));
        Switch sw = new Switch(this);
        sw.setChecked(setupHook);
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) { setupHook = on; }
        });
        hookRow.addView(sw);
        s.addView(hookRow);

        TextView start = button("Start game", true, new View.OnClickListener() {
            public void onClick(View v) {
                if (game != null && !game.isOver()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Start a new game?")
                            .setMessage("The game in progress will be replaced.")
                            .setPositiveButton("Start new", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int w) { startGame(); }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    startGame();
                }
            }
        });
        addSpaced(s, start, 18);

        LinearLayout h = card(c);
        h.addView(text("Scoring", 18, TEXT, true));
        h.addView(bullet("Each round, every player bids how many tricks they'll take."));
        h.addView(bullet("Bid exactly right: 20 points + 10 per trick taken."));
        h.addView(bullet("Missed it: −10 points for each trick over or under."));
        h.addView(bullet("Round 1 deals 1 card, round 2 deals 2, and so on. 60 cards ÷ players = number of rounds (2 players: 30, 3: 20, 4: 15, 5: 12, 6: 10)."));
    }

    TextView bullet(String s) {
        TextView t = text("•  " + s, 14, MUTED, false);
        t.setPadding(0, dp(6), 0, 0);
        return t;
    }

    View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(lp(1, dp(h)));
        return v;
    }

    void addSpaced(LinearLayout parent, View v, int top) {
        LinearLayout.LayoutParams p = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        parent.addView(v, p);
    }

    String setupName(int i) {
        String s = setupNames[i] == null ? "" : setupNames[i].trim();
        return s.isEmpty() ? "Player " + (i + 1) : s;
    }

    void startGame() {
        String[] names = new String[setupCount];
        for (int i = 0; i < setupCount; i++) names[i] = setupName(i);
        game = new Game(names, setupHook, setupDealer);
        save();
        showRound();
    }

    // ------------------------------------------------------------------ round

    void showRound() {
        if (game == null) { showHome(); return; }
        if (game.isOver()) { showOver(); return; }
        screen = "round";
        bidViews.clear();
        tookViews.clear();
        noteViews.clear();
        final Game g = game;

        TextView left = button("Scoreboard", false, new View.OnClickListener() {
            public void onClick(View v) { showScores(); }
        });
        primaryBtn = button("Finish round", true, new View.OnClickListener() {
            public void onClick(View v) { finishRound(); }
        });
        LinearLayout c = page(twoButtons(left, primaryBtn));
        c.setPadding(dp(12), dp(16), dp(12), dp(24));

        LinearLayout head = hbox();
        LinearLayout ht = vbox();
        ht.addView(text("Round " + g.cards() + " of " + g.totalRounds(), 24, TEXT, true));
        ht.addView(text(plural(g.cards(), "card") + " each · " + plural(g.cards(), "trick") + " to win", 14, MUTED, false));
        head.addView(ht, weight(1));
        TextView menu = text("Menu", 14, GOLD, true);
        menu.setPadding(dp(12), dp(8), dp(4), dp(8));
        menu.setClickable(true);
        menu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { roundMenu(); }
        });
        head.addView(menu);
        c.addView(head);

        LinearLayout chips = hbox();
        chips.addView(chip("🎩 Dealer: " + g.names[g.dealer()]));
        addSpaced(c, chips, 10);

        LinearLayout list = card(c);
        list.setPadding(dp(12), dp(14), dp(12), dp(16));
        list.addView(text("Bid and actual tricks", 18, TEXT, true));
        list.addView(text("Enter every bid before play (in order, starting left of the dealer). "
                + "After the round, enter the tricks each player actually took.", 13, MUTED, false));

        for (int i = 0; i < g.n(); i++) {
            final int p = g.bidder(i);
            LinearLayout block = vbox();
            block.setPadding(0, dp(14), 0, 0);

            LinearLayout top = hbox();
            String name = g.names[p] + (p == g.dealer() ? "  🎩" : "");
            top.addView(text(name, 17, TEXT, true), weight(1));
            top.addView(text("Total " + g.total(p), 13, MUTED, false));
            block.addView(top);

            LinearLayout row = hbox();
            row.setPadding(0, dp(6), 0, 0);
            LinearLayout bidCol = vbox();
            bidCol.setGravity(Gravity.CENTER_HORIZONTAL);
            bidCol.addView(text("BID", 11, GOLD, true));
            bidCol.addView(stepper(g.curBids[p], g.cards(), bidViews, new IntSetter() {
                public void set(int v) { g.curBids[p] = v; save(); refreshRound(); }
            }));
            row.addView(bidCol, weight(1));
            LinearLayout tookCol = vbox();
            tookCol.setGravity(Gravity.CENTER_HORIZONTAL);
            tookCol.addView(text("TOOK", 11, GOLD, true));
            tookCol.addView(stepper(g.curTricks[p], g.cards(), tookViews, new IntSetter() {
                public void set(int v) { g.curTricks[p] = v; save(); refreshRound(); }
            }));
            row.addView(tookCol, weight(1));
            LinearLayout ptsCol = vbox();
            ptsCol.setGravity(Gravity.CENTER_HORIZONTAL);
            ptsCol.addView(text("PTS", 11, GOLD, true));
            TextView pts = text("", 18, TEXT, true);
            pts.setGravity(Gravity.CENTER);
            pts.setPadding(0, dp(8), 0, 0);
            ptsCol.addView(pts);
            noteViews.add(pts);
            row.addView(ptsCol, lp(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT));
            block.addView(row);

            if (i < g.n() - 1) {
                View line = new View(this);
                line.setBackgroundColor(CARD2);
                LinearLayout.LayoutParams lpl = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                lpl.topMargin = dp(12);
                block.addView(line, lpl);
            }
            list.addView(block);
        }

        statusView = text("", 15, TEXT, true);
        addSpaced(list, statusView, 16);
        hookView = text("", 13, MUTED, false);
        list.addView(hookView);
        tookStatusView = text("", 15, TEXT, true);
        addSpaced(list, tookStatusView, 10);

        LinearLayout st = card(c);
        st.addView(text("Standings", 16, TEXT, true));
        Integer[] order = ranking(g);
        for (int k = 0; k < order.length; k++) {
            int p = order[k];
            LinearLayout r = hbox();
            r.setPadding(0, dp(6), 0, 0);
            r.addView(text((k + 1) + ".  " + g.names[p], 15, MUTED, false), weight(1));
            r.addView(text(String.valueOf(g.total(p)), 15, TEXT, true));
            st.addView(r);
        }

        refreshRound();
    }

    TextView tookStatusView;

    TextView chip(String s) {
        TextView t = text(s, 13, TEXT, false);
        t.setBackground(rounded(CARD2, 20));
        t.setPadding(dp(12), dp(6), dp(12), dp(6));
        return t;
    }

    void refreshRound() {
        Game g = game;
        for (int i = 0; i < g.n(); i++) {
            int p = g.bidder(i);
            bidViews.get(i).setText(String.valueOf(g.curBids[p]));
            tookViews.get(i).setText(String.valueOf(g.curTricks[p]));
            int sc = Game.score(g.curBids[p], g.curTricks[p]);
            TextView pts = noteViews.get(i);
            pts.setText(signed(sc));
            pts.setTextColor(sc >= 0 ? GREEN : RED);
        }
        int bsum = g.sum(g.curBids);
        int diff = bsum - g.cards();
        String rel = diff == 0 ? "even" : (diff > 0 ? "over by " + diff : "under by " + (-diff));
        statusView.setText("Bids: " + bsum + " of " + g.cards() + "  (" + rel + ")");
        boolean bidsOk = g.bidsValid();
        statusView.setTextColor(bidsOk ? TEXT : RED);
        if (g.hook) {
            int f = g.forbiddenDealerBid();
            String dn = g.names[g.dealer()];
            if (!bidsOk) hookView.setText("Hook rule: " + dn + " can't bid " + f + ". Bids can't add up to " + g.cards() + ".");
            else hookView.setText(f >= 0 ? "Hook rule: " + dn + " (dealer) can't bid " + f + "." : "Hook rule is on.");
            hookView.setTextColor(bidsOk ? MUTED : RED);
            hookView.setVisibility(View.VISIBLE);
        } else {
            hookView.setVisibility(View.GONE);
        }
        int tsum = g.sum(g.curTricks);
        boolean tricksOk = tsum == g.cards();
        String tmsg = "Tricks taken: " + tsum + " of " + g.cards();
        if (tsum > g.cards()) tmsg += "  (" + (tsum - g.cards()) + " too many)";
        else if (tsum < g.cards()) tmsg += "  (" + (g.cards() - tsum) + " to assign)";
        tookStatusView.setText(tmsg);
        tookStatusView.setTextColor(tricksOk ? GREEN : (tsum > g.cards() ? RED : TEXT));
        setEnabled(primaryBtn, bidsOk && tricksOk);
    }

    void roundMenu() {
        final boolean canUndo = !game.bids.isEmpty();
        List<String> items = new ArrayList<String>();
        items.add("Scoreboard");
        if (canUndo) items.add("Undo last round");
        items.add("Home");
        items.add("End game now");
        final String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this).setItems(arr, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                String s = arr[w];
                if (s.equals("Scoreboard")) showScores();
                else if (s.equals("Home")) showHome();
                else if (s.equals("Undo last round")) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Undo round " + game.round() + "?")
                            .setMessage("Its bids and tricks come back so you can fix them.")
                            .setPositiveButton("Undo", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d2, int w2) {
                                    game.undoRound(); save(); showRound();
                                }
                            })
                            .setNegativeButton("Cancel", null).show();
                } else if (s.equals("End game now")) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("End the game now?")
                            .setMessage("Final scores use the rounds finished so far. The game will be cleared.")
                            .setPositiveButton("End game", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d2, int w2) {
                                    endEarly();
                                }
                            })
                            .setNegativeButton("Cancel", null).show();
                }
            }
        }).show();
    }

    boolean endedEarly = false;

    void leaveOver() {
        if (endedEarly) { game = null; endedEarly = false; save(); }
        showHome();
    }

    void endEarly() {
        endedEarly = true;
        showOver();
    }

    void finishRound() {
        final Game g = game;
        final int r = g.round();
        g.finishRound();
        save();
        StringBuilder sb = new StringBuilder();
        Integer[] order = ranking(g);
        for (int p = 0; p < g.n(); p++) {
            int sc = g.roundScore(r, p);
            sb.append(g.names[p]).append(":  ").append(signed(sc))
                    .append("   (bid ").append(g.bids.get(r)[p]).append(", took ").append(g.tricks.get(r)[p]).append(")\n");
        }
        sb.append("\nLeader: ").append(g.names[order[0]]).append(" with ").append(g.total(order[0]));
        new AlertDialog.Builder(this)
                .setTitle("Round " + (r + 1) + " results")
                .setMessage(sb.toString())
                .setCancelable(false)
                .setPositiveButton(g.isOver() ? "Final results" : "Next round", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { showRound(); }
                })
                .setNegativeButton("Undo", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { g.undoRound(); save(); showRound(); }
                })
                .show();
    }

    Integer[] ranking(final Game g) {
        Integer[] order = new Integer[g.n()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) { return g.total(b) - g.total(a); }
        });
        return order;
    }

    // ------------------------------------------------------------------ scoreboard

    void showScores() {
        screen = "scores";
        final Game g = game;
        TextView back = button("Back", true, new View.OnClickListener() {
            public void onClick(View v) { onBackPressed(); }
        });
        LinearLayout c = page(twoButtons(null, back));
        c.setPadding(dp(12), dp(16), dp(12), dp(24));
        c.addView(text("Scoreboard", 24, TEXT, true));
        c.addView(text("Bid and Took for each round, then the running score (round points underneath). Swipe sideways for more players.", 13, MUTED, false));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout table = vbox();
        table.setBackground(rounded(CARD, 14));
        table.setPadding(dp(6), dp(8), dp(6), dp(8));
        hs.addView(table);
        addSpaced(c, hs, 12);

        int bidW = dp(34), tookW = dp(40), scoreW = dp(56), gapW = dp(6);
        int colW = bidW + tookW + scoreW + gapW;
        int rdW = dp(40);
        int wrap = ViewGroup.LayoutParams.WRAP_CONTENT;

        LinearLayout hr = hbox();
        hr.addView(cell("", MUTED, true, 12), lp(rdW, wrap));
        for (int p = 0; p < g.n(); p++) {
            TextView t = cell(g.names[p], GOLD, true, 14);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setBackground(rounded(CARD2, 8));
            t.setPadding(dp(4), dp(4), dp(4), dp(4));
            LinearLayout.LayoutParams p2 = lp(colW - gapW, wrap);
            p2.leftMargin = gapW;
            hr.addView(t, p2);
        }
        table.addView(hr);

        LinearLayout sub = hbox();
        sub.setPadding(0, dp(4), 0, dp(4));
        sub.addView(cell("Rd", MUTED, true, 11), lp(rdW, wrap));
        for (int p = 0; p < g.n(); p++) {
            sub.addView(new View(this), lp(gapW, 1));
            sub.addView(cell("Bid", MUTED, true, 11), lp(bidW, wrap));
            sub.addView(cell("Took", MUTED, true, 11), lp(tookW, wrap));
            sub.addView(cell("Score", MUTED, true, 11), lp(scoreW, wrap));
        }
        table.addView(sub);

        for (int r = 0; r < g.bids.size(); r++) {
            LinearLayout row = hbox();
            row.setPadding(0, dp(5), 0, dp(5));
            if (r % 2 == 0) row.setBackground(rounded(0xFF2A2455, 8));
            row.addView(cell(String.valueOf(r + 1), TEXT, true, 14), lp(rdW, wrap));
            for (int p = 0; p < g.n(); p++) {
                int b = g.bids.get(r)[p], t = g.tricks.get(r)[p];
                int sc = g.roundScore(r, p);
                row.addView(new View(this), lp(gapW, 1));
                row.addView(cell(String.valueOf(b), TEXT, false, 15), lp(bidW, wrap));
                row.addView(cell(String.valueOf(t), b == t ? GREEN : RED, true, 15), lp(tookW, wrap));
                LinearLayout sc2 = vbox();
                sc2.addView(cell(String.valueOf(g.totalThrough(p, r)), TEXT, true, 15));
                sc2.addView(cell(signed(sc), sc >= 0 ? GREEN : RED, false, 11));
                row.addView(sc2, lp(scoreW, wrap));
            }
            table.addView(row);
        }
        if (!g.isOver()) {
            LinearLayout row = hbox();
            row.setPadding(0, dp(5), 0, dp(5));
            row.addView(cell(String.valueOf(g.cards()), MUTED, true, 14), lp(rdW, wrap));
            for (int p = 0; p < g.n(); p++) {
                row.addView(new View(this), lp(gapW, 1));
                row.addView(cell(String.valueOf(g.curBids[p]), MUTED, false, 15), lp(bidW, wrap));
                row.addView(cell("…", MUTED, false, 15), lp(tookW, wrap));
                row.addView(cell("", MUTED, false, 15), lp(scoreW, wrap));
            }
            table.addView(row);
        }
        View line = new View(this);
        line.setBackgroundColor(GOLD);
        table.addView(line, lp(rdW + colW * g.n(), dp(1)));
        LinearLayout tr = hbox();
        tr.setPadding(0, dp(8), 0, dp(4));
        tr.addView(cell("Total", GOLD, true, 12), lp(rdW, wrap));
        for (int p = 0; p < g.n(); p++) {
            tr.addView(new View(this), lp(gapW + bidW + tookW, 1));
            tr.addView(cell(String.valueOf(g.total(p)), GOLD, true, 18), lp(scoreW, wrap));
        }
        table.addView(tr);

        int remaining = g.totalRounds() - g.round();
        TextView foot = text(remaining > 0 ? plural(remaining, "round") + " left, including the current one."
                : "Game complete.", 13, MUTED, false);
        addSpaced(c, foot, 12);
    }

    String initials(String s) {
        return s.length() <= 3 ? s : s.substring(0, 3);
    }

    TextView cell(String s, int color, boolean bold, float sp) {
        TextView t = text(s, sp, color, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    // ------------------------------------------------------------------ game over

    void showOver() {
        if (game == null) { showHome(); return; }
        screen = "over";
        final Game g = game;
        TextView rematch = button("Play again", true, new View.OnClickListener() {
            public void onClick(View v) {
                game = new Game(g.names.clone(), g.hook, (g.firstDealer + 1) % g.n());
                endedEarly = false;
                save();
                showRound();
            }
        });
        TextView home = button("Home", false, new View.OnClickListener() {
            public void onClick(View v) { leaveOver(); }
        });
        LinearLayout c = page(twoButtons(home, rematch));

        Integer[] order = ranking(g);
        int best = g.total(order[0]);
        List<String> winners = new ArrayList<String>();
        for (int p : order) if (g.total(p) == best) winners.add(g.names[p]);

        TextView trophy = text("🏆", 64, TEXT, false);
        trophy.setGravity(Gravity.CENTER);
        addSpaced(c, trophy, 12);
        String title = winners.size() == 1 ? winners.get(0) + " wins!"
                : "Tie: " + android.text.TextUtils.join(" & ", winners) + "!";
        TextView tv = text(title, 28, GOLD, true);
        tv.setGravity(Gravity.CENTER);
        c.addView(tv);
        TextView sub = text((g.isOver() ? "After all " : "After ") + plural(g.round(), "round")
                + " with " + best + " points", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        c.addView(sub);

        LinearLayout st = card(c);
        st.addView(text("Final standings", 16, TEXT, true));
        int place = 0;
        int prev = Integer.MIN_VALUE;
        for (int k = 0; k < order.length; k++) {
            int p = order[k];
            if (g.total(p) != prev) { place = k + 1; prev = g.total(p); }
            int hits = 0;
            for (int r = 0; r < g.bids.size(); r++) if (g.bids.get(r)[p] == g.tricks.get(r)[p]) hits++;
            LinearLayout r = hbox();
            r.setPadding(0, dp(10), 0, 0);
            LinearLayout nm = vbox();
            nm.addView(text(place + ".  " + g.names[p], 17, place == 1 ? GOLD : TEXT, true));
            nm.addView(text("Made " + hits + " of " + g.bids.size() + " bids", 12, MUTED, false));
            r.addView(nm, weight(1));
            r.addView(text(String.valueOf(g.total(p)), 20, TEXT, true));
            st.addView(r);
        }

        TextView sb = button("Full scoreboard", false, new View.OnClickListener() {
            public void onClick(View v) { showScores(); }
        });
        addSpaced(c, sb, 14);
    }
}
