package com.example.xyzreader.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;

public class PageAdapter extends RecyclerView
        .Adapter<PageAdapter.PageViewHolder> implements PaginationAsynkTask.PagingListener {

    String mPageText;
    Pagination mPagination;
    Context mContext;
    ProgressBar mProgressBar;

    public PageAdapter(final Context context, final String pageText, final ViewGroup parent, final ProgressBar progressBar) {
        mPageText = pageText;
        mContext = context;
        mProgressBar = progressBar;

        new Handler().post(new Runnable() { // Tried new Handler(Looper.myLopper()) also
            @Override
            public void run() {
                new PaginationAsynkTask(context, pageText, parent, PageAdapter.this)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }

    @Override
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Log.d("julio", "onCreateViewHolder");
        View view = LayoutInflater.from(mContext).inflate(R.layout.page_text_item, parent, false);
        final PageViewHolder vh = new PageViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(PageViewHolder holder, int position) {
        //Log.d("julio", "onBindViewHolder "+position);
        //holder.article_body.setText(""+position);
        holder.article_body.setText(mPagination.get(position));
        /*if(position%2==0) {
            holder.article_body.setBackgroundColor(mContext.getColor(R.color.theme_primary_light));
        } else {
            holder.article_body.setBackgroundColor(mContext.getColor(R.color.theme_primary_dark));
        }*/
    }

    @Override
    public int getItemCount() {
        if(mPagination != null) {
            return mPagination.size();
        } else {
            return 0;
        }
    }

    @Override
    public void onResult(Pagination pagination) {
        mProgressBar.setVisibility(View.GONE);
        mPagination = pagination;
        notifyDataSetChanged();
    }

    public static class PageViewHolder extends RecyclerView.ViewHolder {
        public final TextView article_body;

        public PageViewHolder(View view) {
            super(view);
            article_body = view.findViewById(R.id.article_body);
        }
    }
}
