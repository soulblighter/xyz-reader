package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.TextView;

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
                mIsRefreshing = intent.getBooleanExtra(UpdaterService
                    .EXTRA_REFRESHING, false);
                updateRefreshingUI();
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

            holder.thumbnailView.setImageUrl(mCursor.getString(ArticleLoader
                .Query.THUMB_URL), ImageLoaderHelper.getInstance
                (ArticleListActivity.this).getImageLoader());
            //holder.thumbnailView.setAspectRatio(mCursor.getFloat
            // (ArticleLoader.Query.ASPECT_RATIO));

            String transitionName = getString(R.string.thumbnail_transition,
                (int) mCursor.getLong(ArticleLoader.Query._ID));
            holder.thumbnailView.setTransitionName(transitionName);

            /*ImageLoaderHelper.getInstance(ArticleListActivity.this)
            .getImageLoader().get(mCursor.getString
                (ArticleLoader.Query.THUMB_URL), new ImageLoader
                .ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer
                imageContainer, boolean b) {
                    if(imageContainer.getBitmap() != null) {
                        Palette.from(imageContainer.getBitmap()).generate(new
                         Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                int colorValue = Color.WHITE;
                                if (palette.getVibrantSwatch() != null) {
                                    colorValue = palette.getVibrantSwatch()
                                    .getRgb();
                                } else if (palette.getLightVibrantSwatch() !=
                                 null) {
                                    colorValue = palette
                                    .getLightVibrantSwatch().getRgb();
                                } else if (palette.getDarkVibrantSwatch() !=
                                null) {
                                    colorValue = palette.getDarkVibrantSwatch
                                    ().getRgb();
                                }
                                holder.cardView.setCardBackgroundColor
                                (colorValue);
                            }
                        });
                    }
                }
                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            });*/
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        public long id = -1;
        public final CardView cardView;
        public final DynamicHeightNetworkImageView thumbnailView;
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
