package com.jarvis.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.graphics.drawable.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    // ── UI ────────────────────────────────────
    TextView tvStatus, tvResponse, tvUser;
    ImageView ivOrb;
    LinearLayout llActions, llChat;
    ScrollView svChat;
    EditText etInput;
    ImageButton btnMic;
    Button btnSend;
    View orbPulse;

    // ── Engine ────────────────────────────────
    TextToSpeech tts;
    SpeechRecognizer speechRec;
    Handler handler = new Handler(Looper.getMainLooper());
    boolean ttsReady = false;
    boolean listening = false;

    // ── AI ────────────────────────────────────
    String GROQ_KEY = "Rkbrbrjididydhrekisb736363@";
    List<JSONObject> chatHistory = new ArrayList<>();
    static final int MIC_PERM = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen dark
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#020c18"));

        buildUI();

        tts = new TextToSpeech(this, this);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERM);
        } else {
            initSpeech();
        }

        greet();
    }

    // ── Build entire UI in code (no XML needed) ──
    void buildUI() {
        // Root
        RelativeLayout root = new RelativeLayout(this);
        root.setBackgroundColor(Color.parseColor("#020c18"));
        setContentView(root);

        // ── TOP BAR ──
        LinearLayout topBar = new LinearLayout(this);
        topBar.setId(View.generateViewId());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#050f1f"));
        topBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams topLP = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        topLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        root.addView(topBar, topLP);
        int topBarId = topBar.getId();

        // Logo dot
        View dot = new View(this);
        dot.setBackgroundColor(Color.parseColor("#00ff88"));
        LinearLayout.LayoutParams dotLP = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotLP.setMargins(0, 0, dp(10), 0);
        topBar.addView(dot, dotLP);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("J.A.R.V.I.S");
        tvTitle.setTextColor(Color.parseColor("#00d4ff"));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvTitle.setLetterSpacing(0.3f);
        LinearLayout.LayoutParams titleLP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        topBar.addView(tvTitle, titleLP);

        // Subtitle
        tvStatus = new TextView(this);
        tvStatus.setText("ONLINE");
        tvStatus.setTextColor(Color.parseColor("#00ff88"));
        tvStatus.setTextSize(10);
        tvStatus.setTypeface(Typeface.MONOSPACE);
        topBar.addView(tvStatus);

        // ── BOTTOM INPUT BAR ──
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setId(View.generateViewId());
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.parseColor("#050f1f"));
        bottomBar.setPadding(dp(12), dp(8), dp(12), dp(8));
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams botLP = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        botLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        root.addView(bottomBar, botLP);
        int botBarId = bottomBar.getId();

        // Mic button
        btnMic = new ImageButton(this);
        btnMic.setBackgroundColor(Color.parseColor("#0a1628"));
        btnMic.setImageDrawable(makeMicDrawable());
        LinearLayout.LayoutParams micLP = new LinearLayout.LayoutParams(dp(48), dp(48));
        micLP.setMargins(0, 0, dp(8), 0);
        bottomBar.addView(btnMic, micLP);

        // Text input
        etInput = new EditText(this);
        etInput.setHint("Ask JARVIS...");
        etInput.setHintTextColor(Color.parseColor("#334466"));
        etInput.setTextColor(Color.WHITE);
        etInput.setTextSize(14);
        etInput.setTypeface(Typeface.MONOSPACE);
        etInput.setBackgroundColor(Color.parseColor("#0a1628"));
        etInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#0a1628"));
        inputBg.setStroke(1, Color.parseColor("#00d4ff44"));
        inputBg.setCornerRadius(dp(4));
        etInput.setBackground(inputBg);
        LinearLayout.LayoutParams inputLP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        inputLP.setMargins(0, 0, dp(8), 0);
        bottomBar.addView(etInput, inputLP);

        // Send button
        btnSend = new Button(this);
        btnSend.setText("GO");
        btnSend.setTextColor(Color.parseColor("#020c18"));
        btnSend.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        btnSend.setTextSize(12);
        GradientDrawable sendBg = new GradientDrawable();
        sendBg.setColor(Color.parseColor("#00d4ff"));
        sendBg.setCornerRadius(dp(4));
        btnSend.setBackground(sendBg);
        LinearLayout.LayoutParams sendLP = new LinearLayout.LayoutParams(dp(56), dp(44));
        bottomBar.addView(btnSend, sendLP);

        // ── SCROLL AREA (between top and bottom bars) ──
        svChat = new ScrollView(this);
        RelativeLayout.LayoutParams scrollLP = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        scrollLP.addRule(RelativeLayout.BELOW, topBarId);
        scrollLP.addRule(RelativeLayout.ABOVE, botBarId);
        root.addView(svChat, scrollLP);

        llChat = new LinearLayout(this);
        llChat.setOrientation(LinearLayout.VERTICAL);
        llChat.setPadding(dp(16), dp(16), dp(16), dp(16));
        svChat.addView(llChat);

        // ── ORB ──
        ivOrb = new ImageView(this);
        ivOrb.setImageDrawable(makeOrbDrawable(false));
        LinearLayout.LayoutParams orbLP = new LinearLayout.LayoutParams(dp(120), dp(120));
        orbLP.gravity = Gravity.CENTER_HORIZONTAL;
        orbLP.setMargins(0, dp(24), 0, dp(24));
        llChat.addView(ivOrb, orbLP);

        // Response text
        tvResponse = new TextView(this);
        tvResponse.setText("Good day, sir. How may I assist you?");
        tvResponse.setTextColor(Color.parseColor("#00d4ff"));
        tvResponse.setTextSize(15);
        tvResponse.setTypeface(Typeface.MONOSPACE);
        tvResponse.setLineSpacing(dp(4), 1.2f);
        tvResponse.setPadding(dp(4), 0, dp(4), dp(16));
        tvResponse.setGravity(Gravity.CENTER_HORIZONTAL);
        llChat.addView(tvResponse);

        // Action buttons container
        llActions = new LinearLayout(this);
        llActions.setOrientation(LinearLayout.VERTICAL);
        llChat.addView(llActions);

        // Wire up clicks
        btnMic.setOnClickListener(v -> toggleMic());
        btnSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((v, a, e) -> { sendMessage(); return true; });
    }

    // ── SEND MESSAGE ──────────────────────────
    void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty() || GROQ_KEY.equals("PASTE_YOUR_GROQ_KEY_HERE")) {
            if (GROQ_KEY.equals("PASTE_YOUR_GROQ_KEY_HERE"))
                showResponse("Please add your Groq API key in the app settings.");
            return;
        }
        etInput.setText("");
        showUserMsg(text);
        showResponse("Thinking...");
        tvStatus.setText("THINKING");
        tvStatus.setTextColor(Color.parseColor("#ffd700"));
        btnSend.setEnabled(false);
        new Thread(() -> callGroq(text)).start();
    }

    void showUserMsg(String text) {
        TextView tv = new TextView(this);
        tv.setText("You: " + text);
        tv.setTextColor(Color.parseColor("#aabbcc"));
        tv.setTextSize(13);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0a1628"));
        bg.setStroke(1, Color.parseColor("#00d4ff22"));
        bg.setCornerRadius(dp(4));
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        handler.post(() -> {
            llChat.addView(tv, lp);
            svChat.post(() -> svChat.fullScroll(View.FOCUS_DOWN));
        });
    }

    // ── CALL GROQ AI ──────────────────────────
    void callGroq(String userMsg) {
        try {
            JSONObject userObj = new JSONObject();
            userObj.put("role", "user");
            userObj.put("content", userMsg);
            chatHistory.add(userObj);
            if (chatHistory.size() > 10) chatHistory.remove(0);

            JSONArray messages = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content",
                "You are JARVIS from Iron Man, an advanced AI assistant. " +
                "Reply ONLY with valid JSON: " +
                "{\"response\":\"your reply\",\"intent\":\"general\",\"actions\":[{\"label\":\"Button\",\"url\":\"https://...\"}]} " +
                "Intent: music/search/navigate/weather/general. " +
                "For music add YouTube URL. For search add Google URL. For navigate add Maps URL. " +
                "Max 2 sentences. Address user as Sir.");
            messages.put(sys);
            for (JSONObject h : chatHistory) messages.put(h);

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 300);
            body.put("response_format", new JSONObject().put("type", "json_object"));

            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + GROQ_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(bytes);

            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject resp = new JSONObject(sb.toString());
            String content = resp.getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content");
            JSONObject result = new JSONObject(content);

            String response = result.optString("response", "I'm here, sir.");
            JSONArray actions = result.optJSONArray("actions");

            // Add to history
            JSONObject assistantObj = new JSONObject();
            assistantObj.put("role", "assistant");
            assistantObj.put("content", response);
            chatHistory.add(assistantObj);

            handler.post(() -> {
                showResponse(response);
                showActions(actions);
                speak(response);
                tvStatus.setText("ONLINE");
                tvStatus.setTextColor(Color.parseColor("#00ff88"));
                btnSend.setEnabled(true);
            });

        } catch (Exception e) {
            handler.post(() -> {
                showResponse("Error: " + e.getMessage());
                tvStatus.setText("ERROR");
                tvStatus.setTextColor(Color.parseColor("#ff4444"));
                btnSend.setEnabled(true);
            });
        }
    }

    void showResponse(String text) {
        tvResponse.setText(text);
        ivOrb.setImageDrawable(makeOrbDrawable(text.equals("Thinking...")));
        svChat.post(() -> svChat.fullScroll(View.FOCUS_DOWN));
    }

    void showActions(JSONArray actions) {
        llActions.removeAllViews();
        if (actions == null) return;
        for (int i = 0; i < Math.min(actions.length(), 3); i++) {
            try {
                JSONObject a = actions.getJSONObject(i);
                String label = a.optString("label", "");
                String aUrl  = a.optString("url", "");
                if (label.isEmpty() || aUrl.isEmpty()) continue;

                Button btn = new Button(this);
                btn.setText("▶  " + label);
                btn.setTextColor(Color.parseColor("#00d4ff"));
                btn.setTypeface(Typeface.MONOSPACE);
                btn.setTextSize(12);
                btn.setAllCaps(false);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#051020"));
                bg.setStroke(1, Color.parseColor("#00d4ff66"));
                bg.setCornerRadius(dp(4));
                btn.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
                lp.setMargins(0, dp(6), 0, 0);
                final String finalUrl = aUrl;
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                    startActivity(intent);
                });
                llActions.addView(btn, lp);
            } catch (Exception ignored) {}
        }
    }

    // ── SPEECH RECOGNITION ────────────────────
    void initSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        speechRec = SpeechRecognizer.createSpeechRecognizer(this);
        speechRec.setRecognitionListener(new RecognitionListener() {
            @Override public void onResults(Bundle b) {
                listening = false;
                setMicState(false);
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    etInput.setText(r.get(0));
                    sendMessage();
                }
            }
            @Override public void onError(int e) {
                listening = false;
                setMicState(false);
                String msg = e == 7 ? "Nothing heard — try again" : "Mic error " + e;
                tvStatus.setText(msg);
                tvStatus.setTextColor(Color.parseColor("#ff8800"));
                handler.postDelayed(() -> {
                    tvStatus.setText("ONLINE");
                    tvStatus.setTextColor(Color.parseColor("#00ff88"));
                }, 2500);
            }
            @Override public void onPartialResults(Bundle b) {
                ArrayList<String> p = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (p != null && !p.isEmpty()) etInput.setText(p.get(0));
            }
            @Override public void onReadyForSpeech(Bundle p) {
                tvStatus.setText("LISTENING...");
                tvStatus.setTextColor(Color.parseColor("#00ff88"));
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int t, Bundle b) {}
        });
    }

    void toggleMic() {
        if (listening) {
            speechRec.stopListening();
            listening = false;
            setMicState(false);
        } else {
            if (speechRec == null) { initSpeech(); }
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRec.startListening(intent);
            listening = true;
            setMicState(true);
        }
    }

    void setMicState(boolean on) {
        btnMic.setImageDrawable(makeMicDrawable());
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(on ? Color.parseColor("#00ff8822") : Color.parseColor("#0a1628"));
        bg.setStroke(on ? 2 : 1, on ? Color.parseColor("#00ff88") : Color.parseColor("#00d4ff44"));
        bg.setCornerRadius(dp(4));
        btnMic.setBackground(bg);
    }

    // ── TTS ───────────────────────────────────
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.9f);
            tts.setPitch(0.85f);
            ttsReady = true;
        }
    }

    void speak(String text) {
        if (ttsReady && text != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis");
        }
    }

    void greet() {
        handler.postDelayed(() -> speak("Good day, sir. JARVIS online."), 1500);
    }

    // ── DRAWABLES ─────────────────────────────
    Drawable makeOrbDrawable(boolean active) {
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            active
                ? new int[]{Color.parseColor("#00d4ff"), Color.parseColor("#0044aa")}
                : new int[]{Color.parseColor("#00304a"), Color.parseColor("#020c18")}
        );
        gd.setShape(GradientDrawable.OVAL);
        gd.setStroke(2, Color.parseColor(active ? "#00d4ff" : "#00d4ff66"));
        return gd;
    }

    Drawable makeMicDrawable() {
        // Simple text-based mic icon
        return null; // we'll use emoji text below
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        initSpeech();
    }

    int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRec != null) speechRec.destroy();
        super.onDestroy();
    }
}
