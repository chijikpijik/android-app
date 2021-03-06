package com.easy.pointapp.views;

import com.easy.pointapp.R;
import com.easy.pointapp.model.RestClient;
import com.easy.pointapp.model.api.v1.Comment;
import com.easy.pointapp.model.api.v1.Post;
import com.easy.pointapp.vcs.CommentAdapter;
import com.easy.pointapp.vcs.IAsyncVC;
import com.easy.pointapp.vcs.RecyclerItemClickListener;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by mini1 on 10.08.15.
 */
public class CommentsView extends RelativeLayout {

    private Post mPost;

    private List<Comment> mComments;

    private CommentAdapter mCommentAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ImageButton mBtnAdd;

    private Typeface mTypeface;

    public CommentsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            mTypeface = Typeface
                    .createFromAsset(context.getAssets(), "fonts/webhostinghub-glyphs.ttf");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setPost(Post post) {
        mPost = post;
        if (mPost != null) {
            mCommentAdapter.setPost(mPost);
            loadComments();
        }
    }

    public Post getPost() {
        return mPost;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBtnAdd = (ImageButton) findViewById(R.id.btn_add);
        mBtnAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendComment();
            }
        });
        RecyclerView rv = (RecyclerView) findViewById(R.id.itemsRecyclerView);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        rv.setLayoutManager(llm);
        mComments = new ArrayList<Comment>();
        mCommentAdapter = new CommentAdapter();
        mCommentAdapter.typeface = mTypeface;
        rv.setAdapter(mCommentAdapter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                loadComments();
            }
        });
        rv.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), rv,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, final int position) {
                        //likePost comment
                        if (position > 0) {
                            Comment comment = mComments.get(position - 1);
                            likeComment(comment);
                        } else {
                            likePost();
                        }

                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        // ...
                    }
                }));
    }

    private void likePost() {
        if (mPost != null) {
            ((IAsyncVC) getContext()).backgroundWorkStarted();
            RestClient.likePost(getContext(), mPost.getID()).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Void>() {
                @Override
                public void call(Void aVoid) {
                    ((IAsyncVC) getContext()).backgroundWorkFinished();
                    postLiked(true);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    ((IAsyncVC) getContext()).backgroundWorkFinished();
                    throwable.printStackTrace();
                    postLiked(false);
                }
            });
        }
    }

    public void sendComment() {
        if (mPost != null) {
            EditText postEdit = (EditText) findViewById(R.id.editComment);
            if (TextUtils.isEmpty(postEdit.getText())) {
                Toast.makeText(getContext(), "Too short", Toast.LENGTH_SHORT).show();
            } else {
                ((IAsyncVC) getContext()).backgroundWorkStarted();
                RestClient.sendComment(getContext(), mPost.getID(), postEdit.getText().toString())
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        ((IAsyncVC) getContext()).backgroundWorkFinished();
                        commentAdded(true);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ((IAsyncVC) getContext()).backgroundWorkFinished();
                        throwable.printStackTrace();
                        commentAdded(false);
                    }
                });
            }
        }
    }

    public void postLiked(boolean result) {
        ((IAsyncVC) getContext()).backgroundWorkFinished();
        this.refreshPost();
    }

    public void commentAdded(boolean result) {
        if (mPost != null) {
            EditText myEditText = (EditText) findViewById(R.id.editComment);
            if (myEditText != null) {
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(myEditText.getWindowToken(), 0);
            }

            if (result) {
                loadComments();
                myEditText.setText("");
            } else {
                Toast.makeText(getContext(), "Error occured(", Toast.LENGTH_LONG).show();
            }
            refreshPost();
        }
    }

    private void refreshPost() {
        if (mPost != null) {
            ((IAsyncVC) getContext()).backgroundWorkStarted();
            RestClient.loadPost(getContext(), mPost.getID()).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Post>() {
                @Override
                public void call(Post post) {
                    ((IAsyncVC) getContext()).backgroundWorkFinished();
                    loadedPost(post);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    ((IAsyncVC) getContext()).backgroundWorkFinished();
                    throwable.printStackTrace();
                }
            });
        }
    }

    public void loadedPost(Post post) {
        if (post != null) {
            setPost(post);
        }
    }

    private void likeComment(Comment comment) {
        if (mPost != null) {
            ((IAsyncVC) getContext()).backgroundWorkStarted();
            RestClient.likeComment(getContext(), comment.getID())
                    .subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Void>() {
                        @Override
                        public void call(Void aVoid) {
                            ((IAsyncVC) getContext()).backgroundWorkFinished();
                            loadComments();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            ((IAsyncVC) getContext()).backgroundWorkFinished();
                            throwable.printStackTrace();
                        }
                    });
        }
    }

    public void loadComments() {
        if (mPost != null) {
            ((IAsyncVC) getContext()).backgroundWorkStarted();
            RestClient.loadComments(getContext(), mPost.getID()).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Comment>>() {
                        @Override
                        public void call(List<Comment> comments) {
                            ((IAsyncVC) getContext()).backgroundWorkFinished();
                            loadedComments(comments);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            ((IAsyncVC) getContext()).backgroundWorkFinished();
                            throwable.printStackTrace();
                            Toast.makeText(getContext(), "Error occured(", Toast.LENGTH_LONG);
                        }
                    });
        }

    }

    public void loadedComments(List<Comment> comments) {
        mComments = comments;
        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
        mCommentAdapter.setComments(mComments);
    }

}
