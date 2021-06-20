package com.example.boardtest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.boardtest.models.Comment;
import com.example.boardtest.models.FirebaseID;
import com.example.boardtest.models.Post;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.boardtest.models.FirebaseID.documentId;

public class PostDetailActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PostDetailActivity";

    public static final String EXTRA_POST_KEY = "post_key";

    private DocumentReference mPostReference;

    private TextView mTitleTExt, mContentsText, mNameText;

    private CommentAdapter mAdapter;

    private TextView mTitleView;
    private TextView mBodyView;

    private EditText mCommentField;
    private Button mCommentButton;
    private RecyclerView mCommentsRecycler;

    private String id;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);


        mTitleView = findViewById(R.id.detail_title);
        mBodyView = findViewById(R.id.detail_contents);

        //mNameText = findViewById(R.id.detail_name);
        mCommentField = findViewById(R.id.field_comment_text);
        mCommentButton = findViewById(R.id.button_post_comment);
        mCommentsRecycler = findViewById(R.id.recycler_comments);

        mPostReference = FirebaseFirestore.getInstance().collection("post").document(documentId);

        mCommentButton.setOnClickListener(this);
        mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));

        Intent getIntent = getIntent();
        id = getIntent().getStringExtra(documentId);
        Log.e("ITEM DOCUMENT ID: ", id);

        String postId = mPostReference.collection(FirebaseID.post).document().getId();
        mPostReference.collection(FirebaseID.post).document(postId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().exists()) {
                                if (task.getResult() != null) {
                                    Map<String, Object> snap = task.getResult().getData();
                                    String title = String.valueOf(snap.get(FirebaseID.title));
                                    String contents = String.valueOf(snap.get(FirebaseID.contents));
                                    String name = String.valueOf(snap.get(FirebaseID.nickname));

                                    mTitleTExt.setText(title);
                                    mContentsText.setText(contents);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Add value event listener to the post
        // [START post_value_event_listener]
        mPostReference.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }
            Post post = snapshot.toObject(Post.class);
            // [START_EXCLUDE]

            mTitleView.setText(FirebaseID.title);
            mBodyView.setText(FirebaseID.contents);
        });
        // [END post_value_event_listener]

        // Keep copy of post listener so we can remove it when app stops

        // Listen for comments
        mAdapter = new CommentAdapter(mPostReference.collection("post-comments"));
        mCommentsRecycler.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter != null) {
            mAdapter.cleanupListener();
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_post_comment) {
            postComment();
        }
    }

    private void postComment() {
        final String uid = getUid();
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document();
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                FirebaseID user = documentSnapshot.toObject(FirebaseID.class);
//                if (user == null) {
//                    Log.e(TAG, "User " + uid + " is unexpectedly null");
//                    Toast.makeText(PostDetailActivity.this,
//                            "Error: could not fetch user.",
//                            Toast.LENGTH_SHORT).show();
//                } else {
                    Comment comment = new Comment(uid, FirebaseID.username, mCommentField.getText().toString());
                    mPostReference.collection("post-comments").document().set(comment);

                    mCommentField.setText(null);
//                }
            }
        });
    }

    private static class CommentViewHolder extends RecyclerView.ViewHolder {

        public TextView authorView;
        public TextView bodyView;

        CommentViewHolder(View itemView) {
            super(itemView);

            authorView = itemView.findViewById(R.id.commentAuthor);
            bodyView = itemView.findViewById(R.id.commentBody);
        }
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentViewHolder> {
        private List<String> mCommentIds = new ArrayList<>();
        private List<Comment> mComments = new ArrayList<>();

        private ListenerRegistration listenerRegistration;

        public CommentAdapter(Query query) {
            // Create child event listener
            // [START child_event_listener_recycler]
            EventListener childEventListener = new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshots,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null) {return;}
                    String commentKey;
                    int commentIndex;
                    Comment comment;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        switch (dc.getType()) {
                            case ADDED:
                                // A new comment has been added, add it to the displayed list
                                comment = dc.getDocument().toObject(Comment.class);
                                // [START_EXCLUDE]
                                // Update RecyclerView
                                mCommentIds.add(dc.getDocument().getId());
                                mComments.add(comment);
                                notifyItemInserted(mComments.size() - 1);
                                break;
                            case MODIFIED:
                                // A comment has changed, use the key to determine if we are displaying this
                                // comment and if so displayed the changed comment.
                                comment = dc.getDocument().toObject(Comment.class);
                                commentKey = dc.getDocument().getId();
                                // [START_EXCLUDE]
                                commentIndex = mCommentIds.indexOf(commentKey);
                                if (commentIndex > -1) {
                                    // Replace with the new data
                                    mComments.set(commentIndex, comment);

                                    // Update the RecyclerView
                                    notifyItemChanged(commentIndex);
                                } else {
                                    Log.w(TAG, "onChildChanged:unknown_child:" + commentKey);
                                }
                                // [END_EXCLUDE]
                                break;
                            case REMOVED:
                                // A comment has changed, use the key to determine if we are displaying this
                                // comment and if so remove it.
                                commentKey = dc.getDocument().getId();
                                // [START_EXCLUDE]
                                commentIndex = mCommentIds.indexOf(commentKey);
                                if (commentIndex > -1) {
                                    // Remove data from the list
                                    mCommentIds.remove(commentIndex);
                                    mComments.remove(commentIndex);

                                    // Update the RecyclerView
                                    notifyItemRemoved(commentIndex);
                                } else {
                                    Log.w(TAG, "onChildRemoved:unknown_child:" + commentKey);
                                }
                                // [END_EXCLUDE]
                                break;
                        }
                    }

                }
            };
            // [END child_event_listener_recycler]
            listenerRegistration = query.addSnapshotListener(childEventListener);
        }

        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CommentViewHolder holder, int position) {
            Comment comment = mComments.get(position);
            holder.authorView.setText(comment.author);
            holder.bodyView.setText(comment.text);
        }

        @Override
        public int getItemCount() {
            return mComments.size();
        }

        public void cleanupListener() {
            listenerRegistration.remove();
        }
    }
}