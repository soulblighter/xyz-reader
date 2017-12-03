package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager
    .LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat
        ("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,
        1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a
        // FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be
        // dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the
        // activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail,
            container, false);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View
            .OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder
                    .from(getActivity()).setType("text/plain").setText("Some " +
                        "" + "" + "sample text").getIntent(), getString(R
                    .string.action_share)));
            }
        });

        bindViews();
        return mRootView;
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        final TextView titleView = mRootView.findViewById(R.id.article_title);
        final TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        final TextView bodyView = mRootView.findViewById(R.id.article_body);
        final LinearLayout linerar1 = mRootView.findViewById(R.id.linerar1);
        final RecyclerView recyclerView = mRootView.findViewById(R.id.article_body_list);
        final ProgressBar progressBar = mRootView.findViewById(R.id.progressBar);

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(DateUtils
                    .getRelativeTimeSpanString(publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString() + " by <font " +
                    "" + "" + "color='#ffffff'>" + mCursor.getString
                    (ArticleLoader.Query.AUTHOR) + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(outputFormat.format
                    (publishedDate) + " by <font color='#ffffff'>" + mCursor
                    .getString(ArticleLoader.Query.AUTHOR) + "</font>"));

            }

            //bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader
            //    .Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            //bodyView.setText(R.string.lorem_ipsum);

            // [START] Glide
            Glide.with(getActivity()).load(mCursor.getString(ArticleLoader
                .Query.PHOTO_URL)).asBitmap().diskCacheStrategy
                (DiskCacheStrategy.ALL).into(new SimpleTarget<Bitmap>(100,
                (int) (100 * (1.0f / mCursor.getFloat(ArticleLoader.Query
                    .ASPECT_RATIO)))) {
                @Override
                public void onResourceReady(final Bitmap resource,
                                            GlideAnimation glideAnimation) {
                    setHeaderColor(resource, linerar1);
                }
            });
            // [END] Glide

            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            //layoutManager.setAutoMeasureEnabled(true);
            recyclerView.setLayoutManager(layoutManager);
            //recyclerView.setNestedScrollingEnabled(false);

            recyclerView.setAdapter(new PageAdapter(getActivity(),
                    mCursor.getString(ArticleLoader.Query.BODY),
                    linerar1, progressBar));
        } else {
            mRootView.setVisibility(View.GONE);
        }
    }

    protected void setHeaderColor(final Bitmap bitmap, final View header) {
        if (bitmap != null) {
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    int colorValue;
                    if (palette.getDarkVibrantSwatch() != null) {
                        colorValue = palette.getDarkVibrantSwatch().getRgb();
                    } else if (palette.getDarkMutedSwatch() != null) {
                        colorValue = palette.getDarkMutedSwatch().getRgb();
                    } else {
                        // if we don't have DarkVibrant or DarkMuted,
                        // then give up and leave default layout background
                        // that should be R.color.theme_primary_dark
                        return;
                    }

                    if (getActivity() == null || ArticleDetailFragment
                        .this.isDetached() || !isAdded()) {
                        return;
                    }
                    animateBackgroundColor(header, colorValue);
                }
            });
        }
    }

    protected void animateBackgroundColor(View view, int newColor) {
        if (view != null) {
            Drawable currentBG = view.getBackground();
            Drawable newBG = new ColorDrawable(newColor);
            if (currentBG == null) {
                view.setBackground(newBG);
            } else {
                TransitionDrawable transitionDrawable = new
                    TransitionDrawable(new Drawable[]{currentBG, newBG});
                transitionDrawable.setCrossFadeEnabled(false);
                view.setBackground(transitionDrawable);
                if (view.isAttachedToWindow()) {
                    transitionDrawable.startTransition(getResources()
                        .getInteger(R.integer.anim_duration_medium));
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
        getActivity().startPostponedEnterTransition();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
