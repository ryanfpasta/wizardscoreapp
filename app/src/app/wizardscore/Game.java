package app.wizardscore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One game of Wizard: 3-6 players, 60 / players rounds, round r deals r cards. */
public class Game {
    public static final int DECK = 60;

    public final String[] names;
    public final boolean hook;      // dealer may not make total bids equal the cards dealt
    public final int firstDealer;
    public final List<int[]> bids = new ArrayList<int[]>();    // completed rounds
    public final List<int[]> tricks = new ArrayList<int[]>();
    public int[] curBids;
    public int[] curTricks;
    public boolean biddingDone;

    public Game(String[] names, boolean hook, int firstDealer) {
        this.names = names;
        this.hook = hook;
        this.firstDealer = firstDealer;
        resetCurrent();
    }

    public int n() { return names.length; }
    public int totalRounds() { return DECK / n(); }
    public int round() { return bids.size(); }               // 0-based index of the round being played
    public int cards() { return round() + 1; }
    public boolean isOver() { return round() >= totalRounds(); }
    public int dealer() { return dealer(round()); }
    public int dealer(int r) { return (firstDealer + r) % n(); }
    /** Player index of the i-th bidder this round (bidding starts left of the dealer, dealer bids last). */
    public int bidder(int i) { return (dealer() + 1 + i) % n(); }

    public static int score(int bid, int took) {
        return bid == took ? 20 + 10 * took : -10 * Math.abs(bid - took);
    }

    public int roundScore(int r, int p) { return score(bids.get(r)[p], tricks.get(r)[p]); }

    public int total(int p) { return totalThrough(p, round() - 1); }

    public int totalThrough(int p, int lastRound) {
        int t = 0;
        for (int r = 0; r <= lastRound && r < bids.size(); r++) t += roundScore(r, p);
        return t;
    }

    public int sum(int[] a) { int s = 0; for (int v : a) s += v; return s; }

    /** The bid the dealer is not allowed to make under the hook rule, or -1 if none. */
    public int forbiddenDealerBid() {
        if (!hook) return -1;
        int others = sum(curBids) - curBids[dealer()];
        int f = cards() - others;
        return (f >= 0 && f <= cards()) ? f : -1;
    }

    public boolean bidsValid() {
        return !(hook && sum(curBids) == cards());
    }

    public boolean tricksValid() { return sum(curTricks) == cards(); }

    public void finishRound() {
        bids.add(curBids.clone());
        tricks.add(curTricks.clone());
        resetCurrent();
    }

    public void undoRound() {
        if (bids.isEmpty()) return;
        curBids = bids.remove(bids.size() - 1);
        curTricks = tricks.remove(tricks.size() - 1);
        biddingDone = true;
    }

    private void resetCurrent() {
        curBids = new int[names.length];
        curTricks = new int[names.length];
        biddingDone = false;
    }

    // ---- persistence ----

    private static JSONArray arr(int[] a) {
        JSONArray j = new JSONArray();
        for (int v : a) j.put(v);
        return j;
    }

    private static int[] ints(JSONArray j) throws JSONException {
        int[] a = new int[j.length()];
        for (int i = 0; i < a.length; i++) a[i] = j.getInt(i);
        return a;
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            JSONArray nm = new JSONArray();
            for (String s : names) nm.put(s);
            o.put("names", nm);
            o.put("hook", hook);
            o.put("firstDealer", firstDealer);
            JSONArray rounds = new JSONArray();
            for (int r = 0; r < bids.size(); r++) {
                JSONObject ro = new JSONObject();
                ro.put("b", arr(bids.get(r)));
                ro.put("t", arr(tricks.get(r)));
                rounds.put(ro);
            }
            o.put("rounds", rounds);
            o.put("curBids", arr(curBids));
            o.put("curTricks", arr(curTricks));
            o.put("biddingDone", biddingDone);
            return o.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public static Game fromJson(String s) {
        if (s == null) return null;
        try {
            JSONObject o = new JSONObject(s);
            JSONArray nm = o.getJSONArray("names");
            String[] names = new String[nm.length()];
            for (int i = 0; i < names.length; i++) names[i] = nm.getString(i);
            Game g = new Game(names, o.getBoolean("hook"), o.getInt("firstDealer"));
            JSONArray rounds = o.getJSONArray("rounds");
            for (int r = 0; r < rounds.length(); r++) {
                g.bids.add(ints(rounds.getJSONObject(r).getJSONArray("b")));
                g.tricks.add(ints(rounds.getJSONObject(r).getJSONArray("t")));
            }
            g.curBids = ints(o.getJSONArray("curBids"));
            g.curTricks = ints(o.getJSONArray("curTricks"));
            g.biddingDone = o.getBoolean("biddingDone");
            return g;
        } catch (JSONException e) {
            return null;
        }
    }
}
