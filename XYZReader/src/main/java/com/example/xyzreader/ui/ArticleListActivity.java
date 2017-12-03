package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * An activity representing a list of Articles. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a {@link
 * ArticleDetailActivity} representing item details. On tablets, the activity
 * presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
    LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    StaggeredGridLayoutManager sglm;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat
        ("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,
        1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getTitle());
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout
            .OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        if (ItemsContract.getCount(this) == 0) {
            refresh();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver, new IntentFilter(UpdaterService
            .BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private final BroadcastReceiver mRefreshingReceiver = new
        BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent
                .getAction())) {

                if(intent.getExtras().get(UpdaterService.EXTRA_REFRESHING) instanceof Boolean) {
                    return;
                }

                UpdaterService.RefreshStatus status = (UpdaterService.RefreshStatus)intent
                        .getSerializableExtra(UpdaterService.EXTRA_REFRESHING);

                switch (status) {
                    case STARTED:
                        mIsRefreshing = true;
                        updateRefreshingUI();
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.update_started, Snackbar.LENGTH_SHORT).show();
                        break;
                    case FINISHED:
                        mIsRefreshing = false;
                        updateRefreshingUI();
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.update_finished, Snackbar.LENGTH_SHORT).show();
                        break;
                    case ERROR_NO_NETWORK:
                        mIsRefreshing = false;
                        updateRefreshingUI();
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.update_error_no_network, Snackbar.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ArticleCardAdapter adapter = new ArticleCardAdapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer
            .list_column_count);
        sglm = new StaggeredGridLayoutManager(columnCount,
            StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.scheduleLayoutAnimation();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class ArticleCardAdapter extends RecyclerView
        .Adapter<ArticleViewHolder> {
        private Cursor mCursor;

        public ArticleCardAdapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int
            viewType) {
            View view = getLayoutInflater().inflate(R.layout
                .list_item_article, parent, false);
            final ArticleViewHolder vh = new ArticleViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(ItemsContract.Items.buildItemUri(getItemId(vh
                        .getAdapterPosition())));

                    Pair<View, String>[] pairs =
                        buildListVisibleSharedElements();
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(ArticleListActivity
                            .this, pairs);

                    //ActivityOptionsCompat options = ActivityOptionsCompat.
                    //    makeSceneTransitionAnimation(ArticleListActivity
                    // .this, vh.thumbnailView,
                    //        getString(R.string.thumbnail_transition, (int)
                    // vh.id));
                    startActivity(i, options.toBundle());
                }
            });
            return vh;
        }

        // We need to append a transition name for each image, so if user
        // changes article on view pager
        // on back animation the image animated will be correct
        // https://youtu.be/4L4fLrWDvAU?t=1928
        Pair<View, String>[] buildListVisibleSharedElements() {
            List<Pair<View, String>> pairs = new ArrayList<>();
            int[] firstPosList = sglm.findFirstVisibleItemPositions(null);
            int[] leastPosList = sglm.findLastVisibleItemPositions(null);

            int firstVisible = Integer.MAX_VALUE;
            for (int i : firstPosList) {
                if (i < firstVisible) {
                    firstVisible = i;
                }
            }

            int leastVisible = Integer.MIN_VALUE;
            for (int i : leastPosList) {
                if (i > leastVisible) {
                    leastVisible = i;
                }
            }

            if (firstVisible == Integer.MAX_VALUE || leastVisible == Integer
                .MIN_VALUE) {
                return null;
            }

            for (int i = firstVisible; i <= leastVisible; i++) {
                ArticleViewHolder vh = (ArticleViewHolder) mRecyclerView
                    .findViewHolderForLayoutPosition(i);
                if (vh != null && vh.thumbnailView != null) {
                    pairs.add(new Pair<View, String>(vh.thumbnailView,
                        getString(R.string.thumbnail_transition, (int) vh.id)));
                }
            }

            Pair<View, String>[] result = new Pair[pairs.size()];
            result = pairs.toArray(result);
            return result;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query
                    .PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(final ArticleViewHolder holder, int
            position) {
            mCursor.moveToPosition(position);
            holder.id = mCursor.getLong(ArticleLoader.Query._ID);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query
                .TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(DateUtils
                    .getRelativeTimeSpanString(publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString() + "<br/>" +
                    "" + " by " + mCursor.getString(ArticleLoader.Query
                    .AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(outputFormat.format
                    (publishedDate) + "<br/>" + " by " + mCursor.getString
                    (ArticleLoader.Query.AUTHOR)));
            }

            final int imageWidth = 512;
            final int imageHeight = (int)(imageWidth*(1.0f/mCursor.getFloat(ArticleLoader.Query
                    .ASPECT_RATIO)));

            //int t = holder.thumbnailView.getLayoutParams().width;

            /*holder.thumbnailView.getLayoutParams().width = imageWidth;
            //holder.thumbnailView.getLayoutParams().height = imageHeight;
            holder.thumbnailView.getLayoutParams().height =
                    holder.thumbnailView.getLayoutParams().width *
                            (int)(imageWidth*(1/mCursor.getFloat(ArticleLoader.Query
                                    .ASPECT_RATIO)));*/

            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query
                    .ASPECT_RATIO));

            // [START] Glide
            Glide
                .with(ArticleListActivity.this)
                .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .animate(R.anim.fade_in).crossFade()
                .override(imageWidth, imageHeight)
                .into(holder.thumbnailView);
            // [END] Glide

            String transitionName = getString(R.string.thumbnail_transition,
                (int) mCursor.getLong(ArticleLoader.Query._ID));
            holder.thumbnailView.setTransitionName(transitionName);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        public long id = -1;
        public final CardView cardView;
        public final DynamicHeightImageView thumbnailView;
        public final TextView titleView;
        public final TextView subtitleView;

        public ArticleViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.cardView);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
