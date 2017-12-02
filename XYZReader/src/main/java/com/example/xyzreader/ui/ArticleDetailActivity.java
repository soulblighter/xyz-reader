package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe
 * between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity implements
    LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId = -1;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private Toolbar mToolbar;
    private ImageView mPhotoView;
    private ImageView mLogoView;
    private View mBackgroundProtection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View
                .SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View
                .SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


        mBackgroundProtection = findViewById(R.id.backgroundProtection);
        Animation animation = AnimationUtils.loadAnimation(this,R.anim
            .fade_in);
        mBackgroundProtection.startAnimation(animation);

        mLogoView = findViewById(R.id.logoView);
        mLogoView.startAnimation(animation);


        mPhotoView = findViewById(R.id.thumbnail);

        getLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager
            .SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);

                changePhoto(mSelectedItemId,
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            }
        });

        //if (savedInstanceState == null) {
        if (getIntent() != null && getIntent().getData() != null) {
            mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            mSelectedItemId = mStartId;
        }
        //}

        postponeEnterTransition();
    }

    @Override
    public void onBackPressed() {
        Animation slide_down = AnimationUtils.loadAnimation
            (getApplicationContext(), R.anim.slide_down);
        mPager.startAnimation(slide_down);
        //overridePendingTransition(0, 0);
        super.onBackPressed();
    }

    private void changePhoto(long id, String url, float aspectRatio) {

        // [START] Glide
        Glide
            .with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .animate(R.anim.fade_in).crossFade()
            .override(1024, (int)(1024*(1.0f/aspectRatio)))
            .into(mPhotoView);
        // [END] Glide

        String transitionName = getString(R.string.thumbnail_transition, (int) id);
        mPhotoView.setTransitionName(transitionName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //supportFinishAfterTransition();
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        mCursor.moveToFirst();
        // TODO: optimize
        while (!mCursor.isAfterLast()) {
            long cursorId = mCursor.getLong(ArticleLoader.Query._ID);
            if (cursorId == mStartId && mSelectedItemId == mStartId) {
                // Select the start ID
                if (mStartId > 0) {
                    int mStartPos = mCursor.getPosition();
                    mPager.setCurrentItem(mStartPos, false);
                    //mStartId = 0;
                }
                changePhoto(mCursor.getLong(ArticleLoader.Query._ID),
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
                break;
            }
            mCursor.moveToNext();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }


    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong
                (ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
