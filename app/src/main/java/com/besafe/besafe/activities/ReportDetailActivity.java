package com.besafe.besafe.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besafe.besafe.R;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.Comment;
import com.besafe.besafe.models.CommentRequest;
import com.besafe.besafe.models.UpdateReportRequest;
import com.besafe.besafe.utils.TokenManager;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDetailActivity extends AppCompatActivity {

    private String reportId, imageUrl, description, status, userRole;
    private SupabaseService apiService;

    // UI Elements
    private ImageView ivReportImage;
    private TextView tvStatus, tvDescription, tvSpamWarning;
    private MaterialCardView cardImage;
    private RecyclerView recyclerComments;
    private EditText etComment;
    private ImageButton btnSend;
    private View chatInputContainer;

    // Security Controls
    private View layoutSecurityControls;
    private Button btnMarkReview, btnMarkResolved;

    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();

    // Replace with your actual Supabase URL
    private static final String SUPABASE_STORAGE_BASE_URL = "https://xbmitdvkxbpinghvpgzv.supabase.co/storage/v1/object/public/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Fetch Data from Intent
        reportId = getIntent().getStringExtra("REPORT_ID");
        imageUrl = getIntent().getStringExtra("IMAGE_URL");
        description = getIntent().getStringExtra("DESCRIPTION");
        status = getIntent().getStringExtra("STATUS");
        userRole = TokenManager.getRole(this); // "student" or "security"

        apiService = ApiClient.getClient().create(SupabaseService.class);

        initUI();
        loadReportData();
        fetchComments();
    }

    private void initUI() {
        ivReportImage = findViewById(R.id.iv_report_image);
        tvStatus = findViewById(R.id.tv_report_status);
        tvDescription = findViewById(R.id.tv_report_description);
        tvSpamWarning = findViewById(R.id.tv_spam_warning);
        cardImage = findViewById(R.id.card_image);
        recyclerComments = findViewById(R.id.recycler_comments);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send_comment);
        chatInputContainer = findViewById(R.id.chat_input_container);

        layoutSecurityControls = findViewById(R.id.layout_security_controls);
        btnMarkReview = findViewById(R.id.btn_mark_review);
        btnMarkResolved = findViewById(R.id.btn_mark_resolved);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerComments.setAdapter(commentAdapter);

        // Chat Button
        btnSend.setOnClickListener(v -> postComment());

        // Security Guard Control Buttons
        if ("security".equals(userRole) && !"RESOLVED".equals(status)) {
            layoutSecurityControls.setVisibility(View.VISIBLE);
        }

        btnMarkReview.setOnClickListener(v -> updateReportStatus("UNDER REVIEW"));
        btnMarkResolved.setOnClickListener(v -> updateReportStatus("RESOLVED"));
    }

    private void loadReportData() {
        tvDescription.setText(description);
        tvStatus.setText(status);

        // Color coding for status and hiding buttons
        if ("RESOLVED".equals(status)) {
            tvStatus.setBackgroundColor(Color.parseColor("#D4EDDA"));
            tvStatus.setTextColor(Color.parseColor("#155724"));

            // Disable Chat and Controls if resolved!
            chatInputContainer.setVisibility(View.GONE);
            layoutSecurityControls.setVisibility(View.GONE);
            tvSpamWarning.setVisibility(View.VISIBLE);
            tvSpamWarning.setText("This report is resolved. Chat is closed.");
            tvSpamWarning.setTextColor(Color.parseColor("#6C757D"));

        } else if ("UNDER REVIEW".equals(status)) {
            tvStatus.setBackgroundColor(Color.parseColor("#CCE5FF"));
            tvStatus.setTextColor(Color.parseColor("#004085"));

            // ✨ THE FIX: Hide the "Under Review" button so they can only click "Resolve"
            if (btnMarkReview != null) {
                btnMarkReview.setVisibility(View.GONE);
            }
        }

        // Load Image using Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            cardImage.setVisibility(View.VISIBLE);
            String fullImageUrl = SUPABASE_STORAGE_BASE_URL + imageUrl;
            Glide.with(this)
                    .load(fullImageUrl)
                    .centerCrop()
                    .into(ivReportImage);
        }
    }

    private void fetchComments() {
        String token = "Bearer " + TokenManager.getToken(this);
        String exactMatchId = "eq." + reportId;

        apiService.getComments(token, exactMatchId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentList = response.body();
                    commentAdapter.notifyDataSetChanged();
                    enforceAntiSpamLogic();
                }
            }
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Failed to load comments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        btnSend.setEnabled(false);
        String token = "Bearer " + TokenManager.getToken(this);
        CommentRequest request = new CommentRequest(reportId, userRole, text);

        apiService.postComment(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSend.setEnabled(true);
                if (response.isSuccessful()) {
                    etComment.setText("");
                    fetchComments(); // Refresh list to show new comment and run anti-spam check
                } else {
                    Toast.makeText(ReportDetailActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSend.setEnabled(true);
            }
        });
    }

    private void updateReportStatus(String newStatus) {
        String token = "Bearer " + TokenManager.getToken(this);
        UpdateReportRequest request = new UpdateReportRequest(newStatus);

        // Disable buttons so they don't double click
        btnMarkReview.setEnabled(false);
        btnMarkResolved.setEnabled(false);

        apiService.updateReportStatus(token, "eq." + reportId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReportDetailActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    finish(); // Close the screen, the Live Feed / Calendar will refresh automatically!
                } else {
                    // ✨ THE FIX: Print the exact database error so we know why it failed!
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown DB Error";
                        Toast.makeText(ReportDetailActivity.this, "DB Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ReportDetailActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                    btnMarkReview.setEnabled(true);
                    btnMarkResolved.setEnabled(true);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                btnMarkReview.setEnabled(true);
                btnMarkResolved.setEnabled(true);
            }
        });
    }

    // ==========================================
    // THE ANTI-SPAM LOGIC
    // ==========================================
    private void enforceAntiSpamLogic() {
        if ("RESOLVED".equals(status)) return; // Already handled in loadReportData()

        if (commentList.size() >= 2) {
            Comment lastComment = commentList.get(commentList.size() - 1);
            Comment secondLastComment = commentList.get(commentList.size() - 2);

            // If the last two comments were sent by the CURRENT user's role, block them from sending a 3rd.
            if (lastComment.getSenderRole().equals(userRole) && secondLastComment.getSenderRole().equals(userRole)) {
                chatInputContainer.setVisibility(View.GONE);
                tvSpamWarning.setVisibility(View.VISIBLE);
                tvSpamWarning.setText("Please wait for the other party to reply to avoid spamming.");
            } else {
                chatInputContainer.setVisibility(View.VISIBLE);
                tvSpamWarning.setVisibility(View.GONE);
            }
        } else {
            chatInputContainer.setVisibility(View.VISIBLE);
            tvSpamWarning.setVisibility(View.GONE);
        }
    }

    // ==========================================
    // Simple Inner Adapter for Comments
    // ==========================================
    class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Creating a simple text view programmatically for the chat bubbles
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 24, 32, 24);
            tv.setTextSize(14f);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 8);
            tv.setLayoutParams(params);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Comment c = commentList.get(position);
            TextView tv = (TextView) holder.itemView;

            // Format: "Role: Message"
            tv.setText(c.getSenderRole().toUpperCase() + ": " + c.getMessage());

            // Make the user's own messages blue, and the other person's gray
            if (c.getSenderRole().equals(userRole)) {
                tv.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light Blue
                tv.setTextColor(Color.parseColor("#084298"));
                tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                tv.setBackgroundColor(Color.parseColor("#E9ECEF")); // Light Gray
                tv.setTextColor(Color.parseColor("#212529"));
                tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
        }

        @Override
        public int getItemCount() { return commentList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) { super(itemView); }
        }
    }
}