package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.xyzreader.R;

import java.lang.reflect.MalformedParameterizedTypeException;

/**
 * Created by Julio on 03/12/2017.
 */
class PaginationAsynkTask extends AsyncTask<Void, Void, Pagination> {

    String mPageText;
    Context mContext;
    ViewGroup mParent;
    PagingListener mCallback;

    interface PagingListener {
        void onResult(Pagination pagination);
    }

    PaginationAsynkTask(Context context, String pageText, ViewGroup parent, PagingListener callback) {
        mParent = parent;
        mContext = context;
        mPageText = pageText;
        mCallback = callback;
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    @Override
    protected Pagination doInBackground(Void... voids) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.page_text_item, mParent, false);
        final PageAdapter.PageViewHolder vh = new PageAdapter.PageViewHolder(view);

        /*WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        int width_px = displayMetrics.widthPixels;
        int height_px =displayMetrics.heightPixels;
        int pixeldpi = displayMetrics.densityDpi;

        int width_dp = (width_px/pixeldpi)*160;
        int height_dp = (height_px/pixeldpi)*160;

        displayMetrics = mContext.getResources().getDisplayMetrics();
        final float screenWidthInDp=displayMetrics.widthPixels/displayMetrics.density;
        final float screenHeightInDp=displayMetrics.heightPixels/displayMetrics.density;


        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(size.x, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);*/

        Pagination mPagination = new Pagination(mPageText,
                mContext.getResources().getDisplayMetrics().widthPixels,
                mContext.getResources().getDisplayMetrics().heightPixels,
                vh.article_body.getPaint(),
                vh.article_body.getLineSpacingMultiplier(),
                vh.article_body.getLineSpacingExtra(),
                vh.article_body.getIncludeFontPadding());
        return mPagination;
    }

    @Override
    protected void onPostExecute(Pagination pagination) {
        super.onPostExecute(pagination);
        mContext = null;
        mPageText = null;
        mParent = null;
        if (mCallback != null) {
            mCallback.onResult(pagination);
        }
    }
}
