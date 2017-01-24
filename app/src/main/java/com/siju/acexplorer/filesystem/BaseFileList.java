package com.siju.acexplorer.filesystem;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.Formatter;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.siju.acexplorer.AceActivity;
import com.siju.acexplorer.BaseActivity;
import com.siju.acexplorer.R;
import com.siju.acexplorer.billing.BillingHelper;
import com.siju.acexplorer.billing.BillingStatus;
import com.siju.acexplorer.common.Logger;
import com.siju.acexplorer.common.SharedPreferenceWrapper;
import com.siju.acexplorer.filesystem.backstack.BackStackInfo;
import com.siju.acexplorer.filesystem.backstack.NavigationCallback;
import com.siju.acexplorer.filesystem.backstack.NavigationInfo;
import com.siju.acexplorer.filesystem.groups.Category;
import com.siju.acexplorer.filesystem.model.BackStackModel;
import com.siju.acexplorer.filesystem.model.FavInfo;
import com.siju.acexplorer.filesystem.model.FileInfo;
import com.siju.acexplorer.filesystem.model.ZipModel;
import com.siju.acexplorer.filesystem.modes.ViewMode;
import com.siju.acexplorer.filesystem.task.ExtractZipEntry;
import com.siju.acexplorer.filesystem.task.PasteConflictChecker;
import com.siju.acexplorer.filesystem.task.SearchTask;
import com.siju.acexplorer.filesystem.theme.ThemeUtils;
import com.siju.acexplorer.filesystem.theme.Themes;
import com.siju.acexplorer.filesystem.ui.CustomGridLayoutManager;
import com.siju.acexplorer.filesystem.ui.CustomLayoutManager;
import com.siju.acexplorer.filesystem.ui.DialogBrowseFragment;
import com.siju.acexplorer.filesystem.ui.DividerItemDecoration;
import com.siju.acexplorer.filesystem.ui.EnhancedMenuInflater;
import com.siju.acexplorer.filesystem.ui.GridItemDecoration;
import com.siju.acexplorer.filesystem.utils.FileUtils;
import com.siju.acexplorer.filesystem.utils.RootUtils;
import com.siju.acexplorer.filesystem.views.FastScrollRecyclerView;
import com.siju.acexplorer.helper.RootHelper;
import com.siju.acexplorer.helper.root.RootTools;
import com.siju.acexplorer.helper.root.rootshell.execution.Command;
import com.siju.acexplorer.utils.DialogUtils;
import com.siju.acexplorer.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static android.R.attr.fragment;
import static android.media.tv.TvContract.Programs.Genres.MUSIC;
import static com.siju.acexplorer.R.string.view;
import static com.siju.acexplorer.filesystem.FileConstants.ADS;
import static com.siju.acexplorer.filesystem.groups.Category.COMPRESSED;
import static com.siju.acexplorer.filesystem.groups.Category.DOCS;
import static com.siju.acexplorer.filesystem.groups.Category.DOWNLOADS;
import static com.siju.acexplorer.filesystem.groups.Category.FAVORITES;
import static com.siju.acexplorer.filesystem.groups.Category.FILES;
import static com.siju.acexplorer.filesystem.groups.Category.LARGE_FILES;
import static com.siju.acexplorer.filesystem.groups.Category.VIDEO;
import static com.siju.acexplorer.filesystem.utils.FileUtils.getInternalStorage;


public class BaseFileList extends Fragment implements LoaderManager
        .LoaderCallbacks<ArrayList<FileInfo>>,
        SearchView.OnQueryTextListener,
        Toolbar.OnMenuItemClickListener, SearchTask.SearchHelper,
        View.OnClickListener, NavigationCallback {

    private final String TAG = this.getClass().getSimpleName();
    private FastScrollRecyclerView recyclerViewFileList;
    private View root;
    private final int LOADER_ID = 1000;
    private FileListAdapter fileListAdapter;
    private ArrayList<FileInfo> fileInfoList;
    private String mFilePath;
    private String mFilePathOther;
    private Category category;
    private int viewMode = ViewMode.LIST;
    private boolean mIsZip;
    private SharedPreferenceWrapper sharedPreferenceWrapper;
    private TextView mTextEmpty;
    private boolean mIsDualActionModeActive;
    private boolean mIsDualModeEnabled;
    private MenuItem mViewItem;
    private SearchView mSearchView;
    private boolean isDragStarted;
    private long mLongPressedTime;
    private View mItemView;
    private ArrayList<FileInfo> mDragPaths = new ArrayList<>();
    private RecyclerView.LayoutManager llm;
    private String mLastDualPaneDir;
    private String mLastSinglePaneDir;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String mZipParentPath;
    private String mChildZipPath;
    private boolean inChildZip;
    private AceActivity aceActivity;
    private boolean isDualPaneInFocus;
    private Toolbar mBottomToolbar;
    private ActionMode mActionMode;
    private SparseBooleanArray mSelectedItemPositions = new SparseBooleanArray();
    private MenuItem mPasteItem;
    private MenuItem mRenameItem;
    private MenuItem mInfoItem;
    private MenuItem mArchiveItem;
    private MenuItem mFavItem;
    private MenuItem mExtractItem;
    private MenuItem mHideItem;
    private MenuItem mPermissionItem;
    private boolean mIsMoveOperation = false;
    private String mCurrentZipDir;
    public ArrayList<ZipModel> totalZipList = new ArrayList<>();
    public ArrayList<ZipModel> zipChildren = new ArrayList<>();
    public Archive mArchive;
    public final ArrayList<FileHeader> totalRarList = new ArrayList<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final ArrayList<FileHeader> rarChildren = new ArrayList<>();
    private boolean mInParentZip = true;
    private int mParentZipCategory;
    private boolean mIsPasteItemVisible;
    private String mSelectedPath;
    private Button buttonPathSelect;
    private final HashMap<String, Bundle> scrollPosition = new HashMap<>();
    private final HashMap<String, Bundle> scrollPositionDualPane = new HashMap<>();
    private int gridCols;
    private SharedPreferences preferences;
    private int mCurrentOrientation;
    private final ArrayList<FileInfo> mCopiedData = new ArrayList<>();
    private final boolean mIsRootMode = true;
    private boolean mIsSwipeRefreshed;
    private FileUtils mFileUtils;
    private boolean mStopAnim = true;
    private boolean mIsBackPressed;
    private DividerItemDecoration mDividerItemDecoration;
    private GridItemDecoration mGridItemDecoration;
    private boolean mIsDarkTheme;
    private boolean mInstanceStateExists;
    private final int DIALOG_FRAGMENT = 5000;
    private boolean clearCache;
    private ZipEntry zipEntry = null;
    private String zipEntryFileName;
    private boolean setRefreshSpan;
    private boolean isPremium = true;
    private AdView mAdView;
    private MenuItem mSearchItem;
    private boolean isInSelectionMode;
    private FloatingActionsMenu fabCreateMenu;
    private FloatingActionButton fabCreateFolder;
    private FloatingActionButton fabCreateFile;
    private LinearLayout mNavigationLayout;
    private FrameLayout frameLayoutFab;
    private LinearLayout navDirectory;
    private HorizontalScrollView scrollNavigation;
    private Toolbar toolbar;
    private boolean isHomeScreenEnabled;
    private String currentDir;
    private NavigationInfo navigationInfo;
    private BackStackInfo backStackInfo;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        aceActivity = (AceActivity) context;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.main_list, container, false);
        return root;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        initializeViews();
        setViewTheme();
        setupAds();
        registerReceivers();
        mFileUtils = new FileUtils();
        navigationInfo = new NavigationInfo(getContext());
        backStackInfo = new BackStackInfo();

        if (savedInstanceState == null) {
            mCurrentOrientation = getResources().getConfiguration().orientation;
            checkPreferences();
            mIsDarkTheme = ThemeUtils.isDarkTheme(getActivity());
            getArgs();
            viewMode = sharedPreferenceWrapper.getViewMode(getActivity());
            setupList();
            if (category.equals(FILES)) {
                navigationInfo.setNavDirectory(mFilePath, isHomeScreenEnabled, category);
            }
            initLoader();
            initializeListeners();
        } else {
            mInstanceStateExists = true;
        }
    }

    private void initializeViews() {
        recyclerViewFileList = (FastScrollRecyclerView) root.findViewById(R.id.recyclerViewFileList);
        mTextEmpty = (TextView) root.findViewById(R.id.textEmpty);
        sharedPreferenceWrapper = new SharedPreferenceWrapper();
        recyclerViewFileList.setOnDragListener(new myDragEventListener());
        mSwipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        int colorResIds[] = {R.color.colorPrimaryDark, R.color.colorPrimary, R.color.colorPrimaryDark};
        mSwipeRefreshLayout.setColorSchemeResources(colorResIds);
        mSwipeRefreshLayout.setDistanceToTriggerSync(500);
        mBottomToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_bottom);
        toolbar = (Toolbar) root.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        frameLayoutFab = (FrameLayout) root.findViewById(R.id.frameLayoutFab);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle(R.string.app_name);
        fabCreateMenu = (FloatingActionsMenu) getActivity().findViewById(R.id.fabCreate);
        fabCreateFolder = (FloatingActionButton) getActivity().findViewById(R.id.fabCreateFolder);
        fabCreateFile = (FloatingActionButton) getActivity().findViewById(R.id.fabCreateFile);
        navDirectory = (LinearLayout) root.findViewById(R.id.navButtons);
        scrollNavigation = (HorizontalScrollView) root.findViewById(R.id.scrollNavigation);
        frameLayoutFab.getBackground().setAlpha(0);
    }

    private void setViewTheme() {
        Themes theme = ((BaseActivity) getActivity()).getCurrentTheme();
        switch (theme) {
            case DARK:
                mNavigationLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                mBottomToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                toolbar.setPopupTheme(R.style.Dark_AppTheme_PopupOverlay);
                frameLayoutFab.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_overlay));
                break;
            case LIGHT:
                toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay);
                break;
        }
    }

    private BroadcastReceiver adsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ADS)) {
                isPremium = intent.getBooleanExtra(FileConstants.KEY_PREMIUM, false);
                if (isPremium) {
                    hideAds();
                } else {
                    showAds();
                }
            }
        }
    };

    private void setupAds() {
        isPremium = BillingHelper.getInstance().getInAppBillingStatus().equals(BillingStatus.PREMIUM);
        if (isPremium) {
            hideAds();
        } else {
            if (getActivity() != null && !getActivity().isFinishing()) {
                showAds();
            }
        }
    }


    private void hideAds() {
        LinearLayout adviewLayout = (LinearLayout) root.findViewById(R.id.adviewLayout);
        if (adviewLayout.getChildCount() != 0) {
            adviewLayout.removeView(mAdView);
        }
    }

    private void showAds() {
        // DYNAMICALLY CREATE AD START
        LinearLayout adviewLayout = (LinearLayout) root.findViewById(R.id.adviewLayout);
        // Create an ad.
        if (mAdView == null) {
            mAdView = new AdView(getActivity().getApplicationContext());
            mAdView.setAdSize(AdSize.BANNER);
            mAdView.setAdUnitId(getString(R.string.banner_ad_unit_id));
            // DYNAMICALLY CREATE AD END
            AdRequest adRequest = new AdRequest.Builder().build();
            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
            // Add the AdView to the view hierarchy. The view will have no size until the ad is loaded.
            adviewLayout.addView(mAdView);
        } else {
            ((LinearLayout) mAdView.getParent()).removeAllViews();
            adviewLayout.addView(mAdView);
            // Reload Ad if necessary.  Loaded ads are lost when the activity is paused.
            if (!mAdView.isLoading()) {
                AdRequest adRequest = new AdRequest.Builder().build();
                // Start loading the ad in the background.
                mAdView.loadAd(adRequest);
            }
        }
        // DYNAMICALLY CREATE AD END
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter(ADS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(adsReceiver, intentFilter);
    }

    private void checkPreferences() {
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mIsDualModeEnabled = preferences
                .getBoolean(FileConstants.PREFS_DUAL_PANE, false);
        gridCols = preferences.getInt(FileConstants.KEY_GRID_COLUMNS, 0);
        isHomeScreenEnabled = preferences.getBoolean(FileConstants.PREFS_HOMESCREEN, true);
    }

    private void getArgs() {
        if (getArguments() != null) {
            if (getArguments().getString(FileConstants.KEY_PATH) != null) {
                mFilePath = getArguments().getString(FileConstants.KEY_PATH);
                mFilePathOther = getArguments().getString(FileConstants.KEY_PATH_OTHER);
            }
            category = (Category) getArguments().getSerializable(FileConstants.KEY_CATEGORY);
            if (checkIfLibraryCategory(category)) {
                hideFab();
//                navigationInfo.addHomeNavButton(false);
            } else {
                showFab();
            }
            toggleNavigationVisibility(true);

            mIsZip = getArguments().getBoolean(FileConstants.KEY_ZIP, false);
            boolean isDualPaneInFocus = getArguments().getBoolean(FileConstants.KEY_FOCUS_DUAL, false);
            if (isDualPaneInFocus) {
                mLastDualPaneDir = mFilePath;
                mLastSinglePaneDir = mFilePathOther;
                Log.d(TAG, "on onActivityCreated dual focus Yes--singledir" + mLastSinglePaneDir + "dualDir=" +
                        mLastDualPaneDir);

            } else {
                mLastSinglePaneDir = mFilePath;
                mLastDualPaneDir = mFilePathOther;
                Log.d(TAG, "on onActivityCreated dual focus No--singledir" + mLastSinglePaneDir + "dualDir=" +
                        mLastDualPaneDir);
            }

        }
    }

    private boolean checkIfLibraryCategory(Category category) {
        return !category.equals(FILES);
    }

    public void showFab() {
        frameLayoutFab.setVisibility(View.VISIBLE);
    }

    public void hideFab() {
        frameLayoutFab.setVisibility(View.GONE);
    }


    private void scrollNavigation() {
        scrollNavigation.postDelayed(new Runnable() {
            public void run() {
                scrollNavigation.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);
    }

    public void onBackPressed() {
        if (isSearchVisible()) {
            hideSearchView();
            removeSearchTask();
        } else if (isZipMode()) {
            onBackPressInZip();
        } else if (checkIfBackStackExists()) {
            backOperation();
        }

    }

    private void onBackPressInZip() {
        if (checkZipMode()) {
            backStackInfo.removeEntry(backStackInfo.getBackStack().size() - 1);
            String currentDir = backStackInfo.getCurrentDir(backStackInfo.getBackStack().size() - 1);
            Category currentCategory = backStackInfo.getCurrentCategory(backStackInfo.getBackStack().size() - 1);
            reloadList(true, currentDir);
            navigationInfo.setNavDirectory(currentDir, isHomeScreenEnabled, currentCategory);
        }
    }

    private boolean checkIfBackStackExists() {
        int backStackSize = backStackInfo.getBackStack().size();
        Logger.log(TAG, "checkIfBackStackExists --size=" + backStackSize);


        if (backStackSize == 1) {
            backStackInfo.clearBackStack();
            return false;
        } else if (backStackSize > 1) {

            Logger.log(TAG, "checkIfBackStackExists--Path=" + currentDir + "  Category=" + category);
            return true;
        }
//        Logger.log(TAG, "checkIfBackStackExists --Path=" + mCurrentDir + "  Category=" + mCategory);
        return false;
    }


    private void backOperation() {

        if (checkIfBackStackExists()) {

            backStackInfo.removeEntry(backStackInfo.getBackStack().size() - 1);
            String currentDir = backStackInfo.getCurrentDir(backStackInfo.getBackStack().size() - 1);
            Category currentCategory = backStackInfo.getCurrentCategory(backStackInfo.getBackStack().size() - 1);
            if (checkIfFileCategory(currentCategory)) {
                navigationInfo.setInitialDir();
            } else {
                hideFab();
            }
            reloadList(true, currentDir);
            setTitleForCategory(currentCategory);
            if (currentCategory.equals(FILES)) {
                showFab();
                navigationInfo.setNavDirectory(currentDir, isHomeScreenEnabled, currentCategory);
            }

        } else {
            removeFragmentFromBackStack();
            if (!isHomeScreenEnabled) {
                getActivity().finish();
            }
        }
    }


    private boolean checkIfFileCategory(Category category) {
        return category.equals(FILES) ||
                category.equals(COMPRESSED) ||
                category.equals(DOWNLOADS) ||
                category.equals(FAVORITES) ||
                category.equals(LARGE_FILES);
    }

    private void setTitleForCategory(Category category) {
        switch (category) {
            case FILES:
                toolbar.setTitle(getString(R.string.app_name));
                break;
            case AUDIO:
                toolbar.setTitle(getString(R.string.nav_menu_music));
                break;
            case VIDEO:
                toolbar.setTitle(getString(R.string.nav_menu_video));
                break;
            case IMAGE:
                toolbar.setTitle(getString(R.string.nav_menu_image));
                break;
            case DOCS:
                toolbar.setTitle(getString(R.string.nav_menu_docs));
                break;
            default:
                toolbar.setTitle(getString(R.string.app_name));
        }
    }

    public boolean isFabExpanded() {
        return fabCreateMenu.isExpanded();
    }

    public void collapseFab() {
        fabCreateMenu.collapse();
    }


    /**
     * Called from {@link #onBackPressed()} . Does the following:
     * 1. If homescreen enabled, returns to home screen
     * 2. If homescreen disabled, exits the app
     */
    private void removeFragmentFromBackStack() {

        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.main_container);
        Fragment dualFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.frame_container_dual);

        mBackStackList.clear();
        cleanUpFileScreen();
        Logger.log(TAG, "RemoveFragmentFromBackStack--frag=" + fragment);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim
                .exit_to_left);

        ft.remove(fragment);

        if (dualFragment != null) {
            ft.remove(dualFragment);
        }

/*
        if (mHomeScreenFragment != null) {
            ft.show(mHomeScreenFragment);
            toggleNavigationVisibility(false);
        }*/
        ft.commitAllowingStateLoss();

    }

    private void cleanUpFileScreen() {
        if (isHomeScreenEnabled) {
            setTitleForCategory(100); // Setting title to App name
            hideFab();
            currentDir = null;
            mStartingDir = null;
            mStartingDirDualPane = null;
            toggleDualPaneVisibility(false);
        }

    }

    public void toggleNavigationVisibility(boolean isVisible) {
        mNavigationLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }


    private void setupList() {
        recyclerViewFileList.setHasFixedSize(true);
        if (viewMode == ViewMode.LIST) {
            llm = new CustomLayoutManager(getActivity());
            recyclerViewFileList.setLayoutManager(llm);
        } else {
            refreshSpan();
        }
        recyclerViewFileList.setItemAnimator(new DefaultItemAnimator());
        fileListAdapter = new FileListAdapter(getContext(), fileInfoList,
                category, viewMode);
    }

    private void initLoader() {
        Bundle args = new Bundle();
        args.putString(FileConstants.KEY_PATH, mFilePath);
        getLoaderManager().initLoader(LOADER_ID, args, this);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!mInstanceStateExists) {
            IntentFilter intentFilter = new IntentFilter(FileConstants.RELOAD_LIST);
            intentFilter.addAction(FileConstants.REFRESH);
            getActivity().registerReceiver(mReloadListReceiver, intentFilter);
        }
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
        Logger.log(TAG, "OnPause");
        if (!mInstanceStateExists) {
            getActivity().unregisterReceiver(mReloadListReceiver);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DIALOG_FRAGMENT:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    mSelectedPath = data.getStringExtra("PATH");
                    if (buttonPathSelect != null) {
                        buttonPathSelect.setText(mSelectedPath);
                    }
                }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void addItemDecoration() {

        if (viewMode == FileConstants.KEY_LISTVIEW) {
            if (mDividerItemDecoration == null) {
                mDividerItemDecoration = new DividerItemDecoration(getActivity(), mIsDarkTheme);
            } else {
                recyclerViewFileList.removeItemDecoration(mDividerItemDecoration);
            }
            recyclerViewFileList.addItemDecoration(mDividerItemDecoration);
        } else {
            Drawable divider;
            if (mIsDarkTheme) {
                divider = ContextCompat.getDrawable(getActivity(), R.drawable.divider_line_dark);
            } else {
                divider = ContextCompat.getDrawable(getActivity(), R.drawable.divider_line);
            }
            if (mGridItemDecoration == null) {
                mGridItemDecoration = new GridItemDecoration(divider, divider, gridCols);
            } else {
                recyclerViewFileList.removeItemDecoration(mGridItemDecoration);
            }
            recyclerViewFileList.addItemDecoration(mGridItemDecoration);
        }
    }


    private void initializeListeners() {

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mIsSwipeRefreshed = true;
                refreshList();
            }
        });

        fabCreateFile.setOnClickListener(this);
        fabCreateFolder.setOnClickListener(this);

        fabCreateMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu
                .OnFloatingActionsMenuUpdateListener() {

            @Override
            public void onMenuExpanded() {
                frameLayoutFab.getBackground().setAlpha(240);
                frameLayoutFab.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        fabCreateMenu.collapse();
                        return true;
                    }
                });
            }

            @Override
            public void onMenuCollapsed() {
                frameLayoutFab.getBackground().setAlpha(0);
                frameLayoutFab.setOnTouchListener(null);
            }
        });
        fileListAdapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (getActionMode() != null) {
                    if (mIsDualActionModeActive) {
                        if (checkIfDualFragment()) {
                            itemClickActionMode(position, false);
                        } else {
                            handleItemClick(position);
                        }
                    } else {
                        if (checkIfDualFragment()) {
                            handleItemClick(position);
                        } else {
                            itemClickActionMode(position, false);
                        }
                    }
                } else {
                    handleItemClick(position);
                }
            }
        });
        fileListAdapter.setOnItemLongClickListener(new FileListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                Logger.log(TAG, "On long click" + isDragStarted);
                if (position >= fileInfoList.size() || position == RecyclerView.NO_POSITION) return;

                if (!mIsZip) {
                    itemClickActionMode(position, true);
                    mLongPressedTime = System.currentTimeMillis();

                    if (getActionMode() != null && fileListAdapter
                            .getSelectedCount() >= 1) {
                        mSwipeRefreshLayout.setEnabled(false);
                        mItemView = view;
                        isDragStarted = true;
                    }
                }
            }
        });


        recyclerViewFileList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING ||
                        newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    if (fileListAdapter != null && mStopAnim) {
                        stopAnimation();
                        mStopAnim = false;
                    }
                }
            }
        });

        recyclerViewFileList.setOnTouchListener(new View.OnTouchListener() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
//                Logger.log(TAG, "On touch listener" + mStopAnim);

                int event = motionEvent.getActionMasked();

                if (fileListAdapter != null && mStopAnim) {
                    stopAnimation();
                    mStopAnim = false;
                }

                if (isDragStarted && event == MotionEvent.ACTION_UP) {
                    isDragStarted = false;
                } else if (isDragStarted && event == MotionEvent.ACTION_MOVE && mLongPressedTime != 0) {
                    long timeElapsed = System.currentTimeMillis() - mLongPressedTime;
//                    Logger.log(TAG, "On item touch time Elapsed" + timeElapsed);

                    if (timeElapsed > 1000) {
                        mLongPressedTime = 0;
                        isDragStarted = false;
                        Logger.log(TAG, "On touch drag path size=" + mDragPaths.size());
                        if (mDragPaths.size() > 0) {
                            Intent intent = new Intent();
                            intent.putParcelableArrayListExtra(FileConstants.KEY_PATH, mDragPaths);
                            ClipData data = ClipData.newIntent("", intent);
                            int count = fileListAdapter
                                    .getSelectedCount();
                            View.DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(mItemView,
                                    count);
                            if (Utils.isAtleastNougat()) {
                                view.startDragAndDrop(data, shadowBuilder, mDragPaths, 0);
                            } else {
                                view.startDrag(data, shadowBuilder, mDragPaths, 0);
                            }
                        }
                    }
                }
                return false;
            }
        });
    }


    public void openCompressedFile(String path) {
        computeScroll();

        mIsZip = true;
        mInParentZip = true;
        mCurrentZipDir = null;
        mZipParentPath = path;
        zipEntry = null;
        reloadList(false, path);
        mParentZipCategory = mCategory;
        isDualPaneInFocus = checkIfDualFragment();
        Logger.log(TAG, "Opencompressedfile--mCategory" + mCategory);
        if (mCategory == FileConstants.CATEGORY.COMPRESSED.getValue() ||
                mCategory == FileConstants.CATEGORY.APPS.getValue() ||
                mCategory == FileConstants.CATEGORY.DOWNLOADS.getValue() ||
                mCategory == FileConstants.CATEGORY.FAVORITES.getValue() ||
                mCategory == FileConstants.CATEGORY.LARGE_FILES.getValue()) {
            showFab();
            aceActivity.setCurrentDir(path, isDualPaneInFocus);
            aceActivity.setCurrentCategory(mCategory);
            aceActivity.initializeStartingDirectory();
        }
        setNavDirectory(path, isDualPaneInFocus);
        aceActivity.addToBackStack(path, mCategory);

    }


    private void handleItemClick(int position) {
        if (position >= fileInfoList.size() || position == RecyclerView.NO_POSITION) return;

        switch (mCategory) {
            case 0:
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                Logger.log(TAG, "on handleItemClick--");
                if (fileInfoList.get(position).isDirectory()) {
                    computeScroll();
                    if (mIsZip) {
                        String name = zipChildren.get(position).getName();
                        if (name.startsWith("/")) name = name.substring(1, name.length());
                        String name1 = name.substring(0, name.length() - 1); // 2 so that / doesnt come
                        zipEntry = zipChildren.get(position).getEntry();
                        zipEntryFileName = name1;
                        Logger.log(TAG, "handleItemClick--entry=" + zipEntry + " dir=" + zipEntry.isDirectory()
                                + "name=" + zipEntryFileName);
                        viewZipContents(position);
                    } else {
                        String path = mFilePath = fileInfoList.get(position).getFilePath();

                        isDualPaneInFocus = checkIfDualFragment();
                        aceActivity.setCurrentDir(path, isDualPaneInFocus);

                        // This is opCompleted when any homescreen item is clicked like Fav . Then Fav->FavList . So on
                        // clicking fav list item, category has to be set to files
                        if (mCategory != FileConstants.CATEGORY.FILES.getValue() && FileUtils.checkIfFileCategory
                                (mCategory)) {
                            mCategory = FileConstants.CATEGORY.FILES.getValue();
                            aceActivity.showFab();
                            aceActivity.setCurrentCategory(mCategory);
                            aceActivity.initializeStartingDirectory();
                        }
                        reloadList(false, mFilePath);
                        aceActivity.setNavDirectory(path, isDualPaneInFocus);
                        aceActivity.addToBackStack(path, mCategory);
                    }


                } else {
                    String filePath = fileInfoList.get(position).getFilePath();
                    String extension = fileInfoList.get(position).getExtension();

                    if (!mIsZip && isZipViewable(filePath)) {
                        openCompressedFile(filePath);
                    } else {
                        if (mIsZip) {
                            clearCache = true;
                            if (mZipParentPath.endsWith(".zip")) {
                                String name = zipChildren.get(position).getName().substring(zipChildren.get(position)
                                        .getName().lastIndexOf("/") + 1);

                                ZipEntry zipEntry = zipChildren.get(position).getEntry();
                                ZipEntry zipEntry1 = new ZipEntry(zipEntry);
                                String cacheDirPath = createCacheDirExtract();
                                Logger.log(TAG, "Zip entry NEW:" + zipEntry1 + " zip entry=" + zipEntry);

                                if (cacheDirPath != null) {
                                    if (name.endsWith(".zip")) {
                                        this.zipEntry = zipEntry1;
                                        zipEntryFileName = name;
                                        inChildZip = true;
                                        viewZipContents(position);
                                        mChildZipPath = cacheDirPath + "/" + name;
                                        return;
                                    }


                                    try {
                                        ZipFile zipFile;
                                        if (inChildZip) {
                                            zipFile = new ZipFile(mChildZipPath);
                                        } else {
                                            zipFile = new ZipFile(mZipParentPath);
                                        }
                                        zipEntry1 = zipEntry;
                                        new ExtractZipEntry(zipFile, cacheDirPath,
                                                BaseFileList.this, name, zipEntry1)
                                                .execute();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (mZipParentPath.endsWith(".rar")) {
                                String name = rarChildren.get(position).getFileNameString();
                                FileHeader fileHeader = rarChildren.get(position);
                                String cacheDirPath = createCacheDirExtract();

                                if (cacheDirPath != null) {

                                    try {
                                        Archive rarFile = new Archive(new File(mZipParentPath));
                                        new ExtractZipEntry(rarFile, cacheDirPath,
                                                BaseFileList.this, name, fileHeader)
                                                .execute();

                                    } catch (IOException | RarException e) {
                                        e.printStackTrace();
                                    }

                                }

                            }

                        } else {
                            FileUtils.viewFile(BaseFileList.this, filePath, extension);
                        }
                    }
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                FileUtils.viewFile(BaseFileList.this, fileInfoList.get(position).getFilePath(),
                        fileInfoList.get
                                (position).getExtension());
                break;

        }
    }


    private boolean isZipViewable(String filePath) {
        return filePath.toLowerCase().endsWith("zip") ||
                filePath.toLowerCase().endsWith("jar") ||
                filePath.toLowerCase().endsWith("rar");
    }

    private void viewZipContents(int position) {
        if (mZipParentPath.endsWith("rar")) {
            String name = rarChildren.get(position).getFileNameString();
            mCurrentZipDir = name.substring(0, name.length() - 1);
        } else {
            mCurrentZipDir = zipChildren.get(position).getName();
        }

        mInParentZip = false;
        reloadList(false, mZipParentPath);
        isDualPaneInFocus = checkIfDualFragment();
        String newPath;
        if (mCurrentZipDir.startsWith(File.separator)) {
            newPath = mZipParentPath + mCurrentZipDir;
        } else {
            newPath = mZipParentPath + File.separator + mCurrentZipDir;
        }
        aceActivity.setCurrentDir(newPath, isDualPaneInFocus);
        aceActivity.setNavDirectory(newPath, isDualPaneInFocus);
    }

    private String createCacheDirExtract() {
        String cacheTempDir = ".tmp";
        File cacheDir = getActivity().getExternalCacheDir();
        if (cacheDir == null) return null;
        File file = new File(cacheDir.getParent(), cacheTempDir);

        if (!file.exists()) {
            boolean result = file.mkdir();
            if (result) {
                String nomedia = ".nomedia";
                File noMedia = new File(file + File.separator + nomedia);
                try {
                    //noinspection ResultOfMethodCallIgnored
                    noMedia.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return file.getAbsolutePath();
            }
        } else {
            return file.getAbsolutePath();
        }
        return null;
    }


    private final BroadcastReceiver mReloadListReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(FileConstants.RELOAD_LIST)) {
                computeScroll();
                String path = intent.getStringExtra(FileConstants.KEY_PATH);
                Logger.log(TAG, "New zip PAth=" + path);
                if (path != null) {
                    FileUtils.scanFile(getActivity().getApplicationContext(), path);
                }
                reloadList(true, mFilePath);
            } else if (action.equals(FileConstants.REFRESH)) {

                int operation = intent.getIntExtra(FileConstants.OPERATION, -1);

                switch (operation) {
                    case FileConstants.DELETE:

                        ArrayList<FileInfo> deletedFilesList = intent.getParcelableArrayListExtra("deleted_files");

                        for (FileInfo info : deletedFilesList) {
                            FileUtils.scanFile(getActivity().getApplicationContext(), info.getFilePath());
                        }

                        mIsBackPressed = true;
                        Uri uri = FileUtils.getUriForCategory(mCategory);
                        getContext().getContentResolver().notifyChange(uri, null);
                        for (int i = 0; i < deletedFilesList.size(); i++) {
                            fileInfoList.remove(deletedFilesList.get(i));
                        }
                        fileListAdapter.setStopAnimation(true);
                        fileListAdapter.updateAdapter(fileInfoList);

                        break;

                    case FileConstants.RENAME:

                        final int position = intent.getIntExtra("position", -1);
                        String oldFile = intent.getStringExtra("old_file");
                        String newFile = intent.getStringExtra("new_file");
                        int type = fileInfoList.get(position).getType();
                        FileUtils.removeMedia(getActivity(), new File(oldFile), type);
                        FileUtils.scanFile(getActivity().getApplicationContext(), newFile);
                        fileInfoList.get(position).setFilePath(newFile);
                        fileInfoList.get(position).setFileName(new File(newFile).getName());
                        fileListAdapter.setStopAnimation(true);
                        Logger.log(TAG, "Position changed=" + position);
                        FileUtils.scanFile(getActivity().getApplicationContext(), newFile);
                        fileListAdapter.notifyItemChanged(position);
                        break;

                    case FileConstants.MOVE:
                    case FileConstants.FOLDER_CREATE:
                    case FileConstants.FILE_CREATE:
                    case FileConstants.COPY:
                        boolean isSuccess = intent.getBooleanExtra(FileConstants.IS_OPERATION_SUCCESS, true);
                        ArrayList<String> copiedFiles = intent.getStringArrayListExtra(FileConstants.KEY_PATH);

                        if (copiedFiles != null) {
                            for (String path : copiedFiles) {
                                FileUtils.scanFile(getActivity().getApplicationContext(), path);
                            }
                        }

                        if (!isSuccess) {
                            Toast.makeText(getActivity(), getString(R.string.msg_operation_failed), Toast
                                    .LENGTH_LONG).show();
                        } else {
                            computeScroll();
                            reloadList(true, mFilePath);
                        }
                        break;

                }
            }
        }
    };

    private boolean isInitialSearch;

    private void addSearchResult(FileInfo fileInfo) {
        // initially clearing the array for new result set
        if (!isInitialSearch) {
            fileInfoList.clear();
            fileListAdapter.clear();

        }
        isInitialSearch = true;
        fileInfoList.add(fileInfo);
        fileListAdapter.updateSearchResult(fileInfo);
        stopAnimation();
//        aceActivity.addToBackStack(mFilePath, mCategory);

    }

    public void removeHomeFromNavPath() {
        Logger.log(TAG, "Nav directory count=" + navDirectory.getChildCount());

        for (int i = 0; i < Math.min(navDirectory.getChildCount(), 2); i++) {
            navDirectory.removeViewAt(0);
        }
    }

    private RefreshData refreshData;

    @Override
    public void onPreExecute() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onPostExecute() {
        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void onProgressUpdate(FileInfo val) {
        addSearchResult(val);
    }

    @Override
    public void onCancelled() {
        mSwipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabCreateFile:
            case R.id.fabCreateFolder:
                if (view.getId() == R.id.fabCreateFolder || view.getId() == R.id.fabCreateFile) {
                    fabCreateMenu.collapse();
                }
                if (view.getId() == R.id.fabCreateFolder || view.getId() == R.id
                        .fabCreateFolderDual) {
                    mOperation = FileConstants.FOLDER_CREATE;
                    String path = isDualPaneInFocus ? mCurrentDirDualPane : mCurrentDir;
                    new FileUtils().createDirDialog(getActivity(), mIsRootMode, path);
                } else {
                    mOperation = FileConstants.FILE_CREATE;
                    String path = isDualPaneInFocus ? mCurrentDirDualPane : mCurrentDir;
                    new FileUtils().createFileDialog(this, mIsRootMode, path);
                }
                setBackPressed();
/*                if (isDualPaneInFocus) {
                    Fragment dualFragment = getSupportFragmentManager().findFragmentById(R.id.frame_container_dual);
                    ((FileListDualFragment) dualFragment).setBackPressed();
                }*/
                break;
        }
    }

    @Override
    public void addViewToNavigation(View view) {
        navDirectory.addView(view);
    }

    @Override
    public void clearNavigation() {
        navDirectory.removeAllViews();
    }

    @Override
    public void onHomeClicked() {
        endActionMode();
        removeFragmentFromBackStack();
    }

    @Override
    public void onNavButtonClicked(String dir) {

        if (!currentDir.equals(dir)) {
            if (isZipMode()) {
                if (isInZipMode(dir)) {
                    int newSize = mBackStackList.size() - 1;
                    mBackStackList.remove(newSize);
                    currentDir = mBackStackList.get(newSize - 1).getFilePath();
                    mCategory = mBackStackList.get(newSize - 1).getCategory();
                    baseFileList.reloadList(true, currentDir);
                    if (!mIsFromHomePage) {
                        setNavDirectory(currentDir);
                    }
                }
            } else {
                currentDir = dir;
                int position = 0;
                for (int i = 0; i < mBackStackList.size(); i++) {
                    if (currentDir.equals(mBackStackList.get(i).getFilePath())) {
                        position = i;
                        break;
                    }
                }
                for (int j = mBackStackList.size() - 1; j > position; j--) {
                    mBackStackList.remove(j);
                }

                baseFileList.reloadList(true, currentDir);
                setNavDirectory(currentDir);

            }
        }
    }


    interface RefreshData {
        void refresh(int category);
    }

    public void setRefreshData(RefreshData data) {
        refreshData = data;
    }

    private void toggleDragData(FileInfo fileInfo) {
        if (mDragPaths.contains(fileInfo)) {
            mDragPaths.remove(fileInfo);
        } else {
            mDragPaths.add(fileInfo);
        }
    }

    private void itemClickActionMode(int position, boolean isLongPress) {
        fileListAdapter.toggleSelection(position, isLongPress);

        boolean hasCheckedItems = fileListAdapter.getSelectedCount() > 0;
        ActionMode actionMode = getActionMode();
        if (hasCheckedItems && actionMode == null) {
            // there are some selected items, start the actionMode
//            aceActivity.updateDrawerIcon(true);

            startActionMode();


            mIsDualActionModeActive = checkIfDualFragment();
//            ((AceActivity) getActivity()).setFileList(fileInfoList);
        } else if (!hasCheckedItems && actionMode != null) {
            // there no selected items, finish the actionMode

//            mActionModeCallback.endActionMode();
            actionMode.finish();
        }
        if (getActionMode() != null) {
            FileInfo fileInfo = fileInfoList.get(position);
            toggleDragData(fileInfo);
            SparseBooleanArray checkedItemPos = fileListAdapter.getSelectedItemPositions();
            setSelectedItemPos(checkedItemPos);
            mActionMode.setTitle(String.valueOf(fileListAdapter
                    .getSelectedCount()) + " selected");
        }
    }

    private void computeScroll() {
        View vi = recyclerViewFileList.getChildAt(0);
        int top = (vi == null) ? 0 : vi.getTop();
        int position;
        if (viewMode == FileConstants.KEY_LISTVIEW) {
            position = ((LinearLayoutManager) llm).findFirstVisibleItemPosition();
        } else {
            position = ((GridLayoutManager) llm).findFirstVisibleItemPosition();
        }
        Bundle bundle = new Bundle();
        bundle.putInt(FileConstants.KEY_POSITION, position);
        bundle.putInt(FileConstants.KEY_OFFSET, top);
        if (checkIfDualFragment()) {
            scrollPositionDualPane.put(mFilePath, bundle);
        } else {
            scrollPosition.put(mFilePath, bundle);
        }
    }

    private void setSelectedItemPos(SparseBooleanArray selectedItemPos) {
        mSelectedItemPositions = selectedItemPos;
        if (selectedItemPos.size() > 1) {
            mRenameItem.setVisible(false);
            mInfoItem.setVisible(false);
        } else {
            mRenameItem.setVisible(true);
            mInfoItem.setVisible(true);
        }
    }


    // 1 Extra for Footer since {#getItemCount has footer
    // TODO Remove this 1 when if footer removed in future
    private void toggleSelectAll(boolean selectAll) {
        fileListAdapter.clearSelection();
        for (int i = 0; i < fileListAdapter.getItemCount() - 1; i++) {
            fileListAdapter.toggleSelectAll(i, selectAll);
        }
        SparseBooleanArray checkedItemPos = fileListAdapter.getSelectedItemPositions();
        setSelectedItemPos(checkedItemPos);
        mActionMode.setTitle(String.valueOf(fileListAdapter.getSelectedCount()) + " " + getString(R.string.selected));
        fileListAdapter.notifyDataSetChanged();
    }

    private void clearSelection() {
        fileListAdapter.removeSelection();
    }


    public void refreshList() {
        Bundle args = new Bundle();
        args.putString(FileConstants.KEY_PATH, mFilePath);
        args.putBoolean(FileConstants.KEY_ZIP, mIsZip);
        getLoaderManager().restartLoader(LOADER_ID, args, this);
    }

    public void setCategory(int category) {
        mCategory = category;
        setRefreshSpan = true;

    }


    public void reloadList(boolean isBackPressed, String path) {
        mFilePath = path;
        mIsBackPressed = isBackPressed;
        refreshList();
    }

/*    public void loadZipEntry() {
        reloadList();
    }*/

    private void stopAnimation() {
        if ((!fileListAdapter.mStopAnimation)) {
            for (int i = 0; i < recyclerViewFileList.getChildCount(); i++) {
                View view = recyclerViewFileList.getChildAt(i);
                if (view != null) view.clearAnimation();
            }
        }
        fileListAdapter.mStopAnimation = true;
    }


    public boolean isZipMode() {
        return mIsZip;
    }

    public BackStackModel endZipMode() {

        mIsZip = false;
        inChildZip = false;
        mInParentZip = true;
        mCurrentZipDir = null;
        totalZipList.clear();
        zipChildren.clear();
        return new BackStackModel(mZipParentPath, mParentZipCategory);
    }


    private boolean checkZipMode() {
        if (mCurrentZipDir == null || mCurrentZipDir.length() == 0) {
            endZipMode();
            return true;
        } else {
            inChildZip = false;
            Logger.log(TAG, "checkZipMode--currentzipdir B4=" + mCurrentZipDir);
            mCurrentZipDir = new File(mCurrentZipDir).getParent();
            if (mCurrentZipDir != null && mCurrentZipDir.equals(File.separator)) {
                mCurrentZipDir = null;
            }
            Logger.log(TAG, "checkZipMode--currentzipdir AFT=" + mCurrentZipDir);
            reloadList(true, mZipParentPath);
            isDualPaneInFocus = checkIfDualFragment();
            String newPath;
            if (mCurrentZipDir == null || mCurrentZipDir.equals(File.separator)) {
                newPath = mZipParentPath;
            } else {
                if (mCurrentZipDir.startsWith(File.separator)) {
                    newPath = mZipParentPath + File.separator + mCurrentZipDir;
                } else {
                    newPath = mZipParentPath + mCurrentZipDir;
                }
            }
            aceActivity.setCurrentDir(newPath, isDualPaneInFocus);
            aceActivity.setNavDirectory(newPath, isDualPaneInFocus);
            return false;
        }
    }

    public boolean isInZipMode(String path) {
        if (mCurrentZipDir == null || mCurrentZipDir.length() == 0 || !path.contains(mZipParentPath)) {
            endZipMode();
            return true;
        } else if (path.equals(mZipParentPath)) {
            mInParentZip = true;
            mCurrentZipDir = null;
            reloadList(true, mZipParentPath);
            aceActivity.setCurrentDir(mZipParentPath, isDualPaneInFocus);
            aceActivity.setNavDirectory(mZipParentPath, isDualPaneInFocus);
            return false;
        } else {
            String newPath = path.substring(mZipParentPath.length() + 1, path.length());
            Logger.log(TAG, "New zip path=" + newPath);
            mCurrentZipDir = newPath;
            mInParentZip = false;
            reloadList(false, mZipParentPath);
            aceActivity.setCurrentDir(path, isDualPaneInFocus);
            aceActivity.setNavDirectory(path, isDualPaneInFocus);
            return false;
        }
    }

    public void setBackPressed() {
        mIsBackPressed = true;
    }


    @Override
    public Loader<ArrayList<FileInfo>> onCreateLoader(int id, Bundle args) {
        fileInfoList = new ArrayList<>();
        if (fileListAdapter != null) {
            fileListAdapter.clearList();
        }
        String path = args.getString(FileConstants.KEY_PATH);
        mSwipeRefreshLayout.setRefreshing(true);
        Logger.log(TAG, "onCreateLoader---path=" + path + "category=" + mCategory + "zip entry=" + zipEntry);

        if (mIsZip) {
            if (inChildZip) {
                if (zipEntry.isDirectory()) {
                    return new FileListLoader(this, mChildZipPath, createCacheDirExtract(),
                            zipEntryFileName, zipEntry);
                } else
                    return new FileListLoader(this, mZipParentPath, createCacheDirExtract(),
                            zipEntryFileName, zipEntry);
            }
            return new FileListLoader(this, path, FileConstants.CATEGORY.ZIP_VIEWER.getValue(),
                    mCurrentZipDir, isDualPaneInFocus, mInParentZip);
        } else {
            return new FileListLoader(this, path, mCategory, false);
        }
    }


    @Override
    public void onLoadFinished(Loader<ArrayList<FileInfo>> loader, ArrayList<FileInfo> data) {
        if (mIsSwipeRefreshed) {
            mSwipeRefreshLayout.setRefreshing(false);
            mIsSwipeRefreshed = false;
        }
        mSwipeRefreshLayout.setRefreshing(false);

        if (data != null) {

            Log.d(TAG, "on onLoadFinished--" + data.size());
/*            if (FileUtils.checkIfLibraryCategory(mCategory)) {
                aceActivity.addCountText(data.size());
            }*/
            mStopAnim = true;
            fileInfoList = data;
            fileListAdapter.setCategory(mCategory);
            fileListAdapter.updateAdapter(fileInfoList);
            recyclerViewFileList.setAdapter(fileListAdapter);
            addItemDecoration();

            if (!data.isEmpty()) {

                if (setRefreshSpan) {
                    refreshSpan();
                    setRefreshSpan = false;
                }

                if (mIsBackPressed) {
                    if (checkIfDualFragment()) {
                        if (scrollPositionDualPane.containsKey(mFilePath)) {
                            Bundle b = scrollPositionDualPane.get(mFilePath);
                            if (viewMode == FileConstants.KEY_LISTVIEW)
                                ((LinearLayoutManager) llm).scrollToPositionWithOffset(b.getInt(FileConstants
                                        .KEY_POSITION), b.getInt(FileConstants.KEY_OFFSET));
                            else
                                ((GridLayoutManager) llm).scrollToPositionWithOffset(b.getInt(FileConstants
                                        .KEY_POSITION), b.getInt(FileConstants.KEY_OFFSET));
                            recyclerViewFileList.stopScroll();
                        }
                    } else {
                        Log.d("TEST", "on onLoadFinished scrollpos--" + scrollPosition.entrySet());

                        if (scrollPosition.containsKey(mFilePath)) {
                            Bundle b = scrollPosition.get(mFilePath);
                            if (viewMode == FileConstants.KEY_LISTVIEW)
                                ((LinearLayoutManager) llm).scrollToPositionWithOffset(b.getInt(FileConstants
                                        .KEY_POSITION), b.getInt(FileConstants.KEY_OFFSET));
                            else
                                ((GridLayoutManager) llm).scrollToPositionWithOffset(b.getInt(FileConstants
                                        .KEY_POSITION), b.getInt(FileConstants.KEY_OFFSET));
                        }
                    }
                    mIsBackPressed = false;
                }
                recyclerViewFileList.stopScroll();
                mTextEmpty.setVisibility(View.GONE);
            } else {
                mTextEmpty.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<ArrayList<FileInfo>> loader) {
        // Clear the data in the adapter.
        fileListAdapter.updateAdapter(null);
    }

    private boolean checkIfDualFragment() {
        return false;
//        return BaseFileList.this instanceof FileListDualFragment;
    }

    private void startActionMode() {

        aceActivity.toggleFab(true);
//        toggleDummyView(true);
        mBottomToolbar.setVisibility(View.VISIBLE);
        mBottomToolbar.startActionMode(new ActionModeCallback());
        mBottomToolbar.inflateMenu(R.menu.action_mode_bottom);
        mBottomToolbar.getMenu().clear();
        EnhancedMenuInflater.inflate(getActivity().getMenuInflater(), mBottomToolbar.getMenu(),
                mCategory);
        mBottomToolbar.setOnMenuItemClickListener(this);

    }

    private ActionMode getActionMode() {
        return mActionMode;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_cut:
                if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                    FileUtils.showMessage(getActivity(), mSelectedItemPositions.size() + " " +
                            getString(R.string.msg_cut_copy));
                    mCopiedData.clear();
                    for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                        mCopiedData.add(fileInfoList.get(mSelectedItemPositions.keyAt(i)));
                    }
                    mIsMoveOperation = true;
                    togglePasteVisibility(true);
                    getActivity().supportInvalidateOptionsMenu();
                    mActionMode.finish();
                }
                break;
            case R.id.action_copy:

                if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                    mIsMoveOperation = false;
                    FileUtils.showMessage(getActivity(), mSelectedItemPositions.size() + " " + getString(R.string
                            .msg_cut_copy));
                    mCopiedData.clear();
                    for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                        mCopiedData.add(fileInfoList.get(mSelectedItemPositions.keyAt(i)));
                    }
                    togglePasteVisibility(true);
                    getActivity().supportInvalidateOptionsMenu();
                    mActionMode.finish();
                }
                break;
            case R.id.action_delete:

                if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                    ArrayList<FileInfo> filesToDelete = new ArrayList<>();
                    for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                        FileInfo info = fileInfoList.get(mSelectedItemPositions.keyAt(i));
                        filesToDelete.add(info);
                    }
                    if (mCategory == FileConstants.CATEGORY.FAVORITES.getValue()) {
                        removeFavorite(filesToDelete);
                        Toast.makeText(getContext(), getString(R.string.fav_removed), Toast.LENGTH_SHORT).show();
                    } else {
                        mFileUtils.showDeleteDialog(aceActivity, filesToDelete, RootUtils.isRooted(getActivity()));
                    }
                    mActionMode.finish();
                }
                break;
            case R.id.action_share:
                if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                    ArrayList<FileInfo> filesToShare = new ArrayList<>();
                    for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                        FileInfo info = fileInfoList.get(mSelectedItemPositions.keyAt(i));
                        if (!info.isDirectory()) {
                            filesToShare.add(info);
                        }
                    }
                    FileUtils.shareFiles(getActivity(), filesToShare, mCategory);
                    mActionMode.finish();
                }
                break;

            case R.id.action_select_all:
                if (mSelectedItemPositions != null) {
                    if (mSelectedItemPositions.size() < fileListAdapter.getItemCount() - 1) {
                        toggleSelectAll(true);
                    } else {
                        toggleSelectAll(false);

                    }
                }
                break;
        }
        return false;
    }

    /**
     * Triggered on long press click on item
     */
    private final class ActionModeCallback implements ActionMode.Callback {


        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            isInSelectionMode = true;
            MenuInflater inflater = mActionMode.getMenuInflater();
            inflater.inflate(R.menu.action_mode, menu);
            mRenameItem = menu.findItem(R.id.action_rename);
            mInfoItem = menu.findItem(R.id.action_info);
            mArchiveItem = menu.findItem(R.id.action_archive);
            mFavItem = menu.findItem(R.id.action_fav);
            mExtractItem = menu.findItem(R.id.action_extract);
            mHideItem = menu.findItem(R.id.action_hide);
            mPermissionItem = menu.findItem(R.id.action_permissions);
            setupMenu();
            return true;
        }

        @SuppressLint("NewApi")
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            // Dont show Fav and Archive option for Non file mode
            if (mCategory != 0) {
                mArchiveItem.setVisible(false);
                mFavItem.setVisible(false);
                mHideItem.setVisible(false);
            }

            if (mSelectedItemPositions.size() > 1) {
                mRenameItem.setVisible(false);
                mInfoItem.setVisible(false);

            } else {
                mRenameItem.setVisible(true);
                mInfoItem.setVisible(true);
                if (mSelectedItemPositions.size() == 1) {


                    boolean isDirectory = fileInfoList.get(mSelectedItemPositions.keyAt(0))
                            .isDirectory();
                    String filePath = fileInfoList.get(mSelectedItemPositions.keyAt(0))
                            .getFilePath();

                    @SuppressLint("SdCardPath")
                    boolean isRoot = preferences.getBoolean(FileConstants.PREFS_ROOTED, false);
                    if (FileUtils.isFileCompressed(filePath)) {
                        mExtractItem.setVisible(true);
                        mArchiveItem.setVisible(false);
                    }
                    if (isRoot) {
                        mPermissionItem.setVisible(true);
                    }
                    if (!isDirectory) {
                        mFavItem.setVisible(false);
                    }

                }
                String fileName = fileInfoList.get(mSelectedItemPositions.keyAt(0)).getFileName();

                if (fileName.startsWith(".")) {
                    SpannableStringBuilder hideBuilder = new SpannableStringBuilder(" " + "  " +
                            "" + getString(R.string
                            .unhide));
                    if (mIsDarkTheme) {
                        hideBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_unhide_white), 0, 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        hideBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_unhide_black), 0, 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    mHideItem.setTitle(hideBuilder);
                } else {
                    SpannableStringBuilder hideBuilder = new SpannableStringBuilder(" " + "  " +
                            "" + getString(R.string
                            .hide));

                    if (mIsDarkTheme) {
                        hideBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_hide_white), 0, 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        hideBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_hide_black), 0, 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    mHideItem.setTitle(hideBuilder);
                }
            }

            return false;
        }

        private void setupMenu() {
            SpannableStringBuilder builder = new SpannableStringBuilder(" " + "  " + getString(R
                    .string
                    .action_archive));
            if (mIsDarkTheme) {
                builder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_archive_white), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                builder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_archive_black), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            mArchiveItem.setTitle(builder);


            SpannableStringBuilder extractBuilder = new SpannableStringBuilder(" " + "  " + getString(R
                    .string.extract_to));
            if (mIsDarkTheme) {
                extractBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_extract_white), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                extractBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_extract_black), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            mExtractItem.setTitle(extractBuilder);

            SpannableStringBuilder favBuilder = new SpannableStringBuilder(" " + "  " + getString
                    (R.string
                            .add_fav));
            if (mIsDarkTheme) {
                favBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_favorite_white), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                favBuilder.setSpan(new ImageSpan(getActivity(), R.drawable.ic_favorite_black), 0, 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mFavItem.setTitle(favBuilder);

            SpannableStringBuilder hideBuilder = new SpannableStringBuilder(" " + "  " +
                    getString(R.string
                            .hide));
            mHideItem.setTitle(hideBuilder);
        }


        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_rename:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        final String oldFilePath = fileInfoList.get(mSelectedItemPositions.keyAt(0)
                        ).getFilePath();
                        int renamedPosition = mSelectedItemPositions.keyAt(0);
                        String newFilePath = new File(oldFilePath).getParent();
                        renameDialog(oldFilePath, newFilePath, renamedPosition);
                    }
                    return true;

                case R.id.action_info:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        FileInfo fileInfo = fileInfoList.get(mSelectedItemPositions.keyAt(0));
                        showInfoDialog(fileInfo);
                    }
                    mActionMode.finish();
                    return true;
                case R.id.action_archive:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        ArrayList<FileInfo> paths = new ArrayList<>();
                        for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                            FileInfo info = fileInfoList.get(mSelectedItemPositions.keyAt(i));
                            paths.add(info);
                        }
                        setBackPressed();
                        mFileUtils.showCompressDialog(aceActivity, mFilePath, paths);
                    }
                    mActionMode.finish();
                    return true;
                case R.id.action_fav:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        int count = 0;
                        ArrayList<FileInfo> favList = new ArrayList<>();
                        for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                            FileInfo info = fileInfoList.get(mSelectedItemPositions.keyAt(i));
                            // Fav option meant only for directories
                            if (info.isDirectory()) {
                                favList.add(info);
//                                updateFavouritesGroup(info);
                                count++;
                            }
                        }


                        if (count > 0) {
                            FileUtils.showMessage(getActivity(), getString(R.string.msg_added_to_fav));
                            updateFavouritesGroup(favList);
                        }
                    }
                    mActionMode.finish();
                    return true;

                case R.id.action_extract:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        FileInfo fileInfo = fileInfoList.get(mSelectedItemPositions.keyAt(0));
                        String currentFile = fileInfo.getFilePath();
                        showExtractOptions(currentFile, mFilePath);
                    }

                    mActionMode.finish();
                    return true;

                case R.id.action_hide:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        ArrayList<FileInfo> infoList = new ArrayList<>();
                        ArrayList<Integer> pos = new ArrayList<>();
                        for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                            infoList.add(fileInfoList.get(mSelectedItemPositions.keyAt(i)));
                            pos.add(mSelectedItemPositions.keyAt(i));

                        }
                        hideUnHideFiles(infoList, pos);
                    }
                    mActionMode.finish();
                    return true;

                case R.id.action_permissions:

                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        FileInfo info = fileInfoList.get(mSelectedItemPositions.keyAt(0));
                        showPermissionsDialog(info);
                    }
                    mActionMode.finish();
                    return true;
                default:
                    return false;
            }

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            isInSelectionMode = false;
            clearSelection();
//            toggleDummyView(false);

            mActionMode = null;
            mBottomToolbar.setVisibility(View.GONE);
            mSelectedItemPositions = new SparseBooleanArray();
            mSwipeRefreshLayout.setEnabled(true);
            mDragPaths.clear();
            // FAB should be visible only for Files Category
            if (mCategory == 0) {
                aceActivity.toggleFab(false);
            }
        }
    }

    public void endActionMode() {
        isInSelectionMode = false;
        mActionMode.finish();
        mActionMode = null;
        mBottomToolbar.setVisibility(View.GONE);
        mSelectedItemPositions = new SparseBooleanArray();
        mSwipeRefreshLayout.setEnabled(true);
        mDragPaths.clear();
    }

    public boolean isInSelectionMode() {
        return isInSelectionMode;
    }

    public boolean isSearchVisible() {
        return mSearchView != null && !mSearchView.isIconified();
    }

    @SuppressWarnings("ConstantConditions")
    private void renameDialog(final String oldFilePath, final String newFilePath, final int
            position) {
        String fileName = oldFilePath.substring(oldFilePath.lastIndexOf("/") + 1, oldFilePath.length());
        boolean file = false;
        String extension = null;
        if (new File(oldFilePath).isFile()) {
            String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
            fileName = tokens[0];
            extension = tokens[1];
            file = true;
        }
        final boolean isFile = file;
        final String ext = extension;

        String title = getString(R.string.action_rename);
        String texts[] = new String[]{"", fileName, title, title, "",
                getString(R.string.dialog_cancel)};
        final MaterialDialog materialDialog = new DialogUtils().showEditDialog(getActivity(), texts);

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fileName = materialDialog.getInputEditText().getText().toString();
                if (FileUtils.isFileNameInvalid(fileName)) {
                    materialDialog.getInputEditText().setError(getResources().getString(R.string
                            .msg_error_valid_name));
                    return;
                }


                fileName = fileName.trim();
                String renamedName = fileName;
                if (isFile) {
                    renamedName = fileName + "." + ext;
                }

                File newFile = new File(newFilePath + "/" + renamedName);
                if (FileUtils.isFileExisting(newFilePath, newFile.getName())) {
                    materialDialog.getInputEditText().setError(getResources().getString(R.string
                            .dialog_title_paste_conflict));
                    return;
                }
                File oldFile = new File(oldFilePath);
                aceActivity.mFileOpsHelper.renameFile(mIsRootMode, oldFile, newFile,
                        position, checkIfDualFragment());
                materialDialog.dismiss();
            }
        });

        materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDialog.dismiss();
            }
        });

        materialDialog.show();
        mActionMode.finish();
    }


    private void hideUnHideFiles(ArrayList<FileInfo> fileInfo, ArrayList<Integer> pos) {
        for (int i = 0; i < fileInfo.size(); i++) {
            String fileName = fileInfo.get(i).getFileName();
            String renamedName;
            if (fileName.startsWith(".")) {
                renamedName = fileName.substring(1);
            } else {
                renamedName = "." + fileName;
            }
            String path = fileInfo.get(i).getFilePath();
            File oldFile = new File(path);
            String temp = path.substring(0, path.lastIndexOf(File.separator));

            File newFile = new File(temp + File.separator + renamedName);
            aceActivity.mFileOpsHelper.renameFile(mIsRootMode, oldFile, newFile,
                    pos.get(i), checkIfDualFragment());
        }
    }


    private void showPermissionsDialog(final FileInfo fileInfo) {

        String texts[] = new String[]{getString(R.string.permissions), getString(R.string.msg_ok),
                "", getString(R.string.dialog_cancel)};
        final MaterialDialog materialDialog = new DialogUtils().showCustomDialog(getActivity(),
                R.layout.dialog_permission, texts);
        final CheckBox readown = (CheckBox) materialDialog.findViewById(R.id.creadown);
        final CheckBox readgroup = (CheckBox) materialDialog.findViewById(R.id.creadgroup);
        final CheckBox readother = (CheckBox) materialDialog.findViewById(R.id.creadother);
        final CheckBox writeown = (CheckBox) materialDialog.findViewById(R.id.cwriteown);
        final CheckBox writegroup = (CheckBox) materialDialog.findViewById(R.id.cwritegroup);
        final CheckBox writeother = (CheckBox) materialDialog.findViewById(R.id.cwriteother);
        final CheckBox exeown = (CheckBox) materialDialog.findViewById(R.id.cexeown);
        final CheckBox exegroup = (CheckBox) materialDialog.findViewById(R.id.cexegroup);
        final CheckBox exeother = (CheckBox) materialDialog.findViewById(R.id.cexeother);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String perm = RootHelper.getPermissions(fileInfo.getFilePath(), fileInfo.isDirectory());
                ArrayList<Boolean[]> arrayList = FileUtils.parse(perm);
                final Boolean[] read = arrayList.get(0);
                final Boolean[] write = arrayList.get(1);
                final Boolean[] exe = arrayList.get(2);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        readown.setChecked(read[0]);
                        readgroup.setChecked(read[1]);
                        readother.setChecked(read[2]);
                        writeown.setChecked(write[0]);
                        writegroup.setChecked(write[1]);
                        writeother.setChecked(write[2]);
                        exeown.setChecked(exe[0]);
                        exegroup.setChecked(exe[1]);
                        exeother.setChecked(exe[2]);
                    }
                });
            }
        };

        new Thread(runnable).start();

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialDialog.dismiss();
                int a = 0, b = 0, c = 0;
                if (readown.isChecked()) a = 4;
                if (writeown.isChecked()) b = 2;
                if (exeown.isChecked()) c = 1;
                int owner = a + b + c;
                int d = 0, e = 0, f = 0;
                if (readgroup.isChecked()) d = 4;
                if (writegroup.isChecked()) e = 2;
                if (exegroup.isChecked()) f = 1;
                int group = d + e + f;
                int g = 0, h = 0, i = 0;
                if (readother.isChecked()) g = 4;
                if (writeother.isChecked()) h = 2;
                if (exeother.isChecked()) i = 1;
                int other = g + h + i;
                String finalValue = owner + "" + group + "" + other;

                String command = "chmod " + finalValue + " " + fileInfo.getFilePath();
                if (fileInfo.isDirectory())
                    command = "chmod -R " + finalValue + " \"" + fileInfo.getFilePath() + "\"";
                Command com = new Command(1, command) {
                    @Override
                    public void commandOutput(int i, String s) {
                        Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void commandTerminated(int i, String s) {
                        Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void commandCompleted(int i, int i2) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.completed), Toast
                                .LENGTH_LONG).show();
                    }
                };
                try {//
                    RootTools.remount(fileInfo.getFilePath(), "RW");
                    RootTools.getShell(true).add(com);
                    refreshList();
                } catch (Exception e1) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.error), Toast.LENGTH_LONG)
                            .show();
                    e1.printStackTrace();
                }

            }
        });
        materialDialog.show();

    }


    @SuppressWarnings("ConstantConditions")
    private void showExtractOptions(final String currentFilePath, final String currentDir) {

        mSelectedPath = null;
        final String currentFileName = currentFilePath.substring(currentFilePath.lastIndexOf("/")
                + 1, currentFilePath.lastIndexOf("."));
        String texts[] = new String[]{getString(R.string.extract), getString(R.string.extract),
                "", getString(R.string.dialog_cancel)};
        final MaterialDialog materialDialog = new DialogUtils().showCustomDialog(getActivity(),
                R.layout.dialog_extract, texts);

        final RadioButton radioButtonSpecify = (RadioButton) materialDialog.findViewById(R.id
                .radioButtonSpecifyPath);
        buttonPathSelect = (Button) materialDialog.findViewById(R.id.buttonPathSelect);
        RadioGroup radioGroupPath = (RadioGroup) materialDialog.findViewById(R.id.radioGroupPath);
        final EditText editFileName = (EditText) materialDialog.findViewById(R.id.editFileName);
        editFileName.setText(currentFileName);
        radioGroupPath.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if (checkedId == R.id.radioButtonCurrentPath) {
                    buttonPathSelect.setVisibility(View.GONE);
                } else {
                    buttonPathSelect.setVisibility(View.VISIBLE);
                }
            }
        });

        buttonPathSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogBrowseFragment dialogFragment = new DialogBrowseFragment();
                dialogFragment.setTargetFragment(BaseFileList.this, DIALOG_FRAGMENT);
                dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, checkTheme());
                dialogFragment.show(getFragmentManager(), "Browse Fragment");
            }
        });

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fileName = editFileName.getText().toString();
                if (FileUtils.isFileNameInvalid(fileName)) {
                    editFileName.setError(getResources().getString(R.string
                            .msg_error_valid_name));
                    return;
                }
                if (radioButtonSpecify.isChecked()) {
                    File newFile = new File(mSelectedPath + "/" + currentFileName);
                    File currentFile = new File(currentFilePath);
                    if (FileUtils.isFileExisting(mSelectedPath, newFile.getName())) {
                        editFileName.setError(getResources().getString(R.string
                                .dialog_title_paste_conflict));
                        return;
                    }
                    aceActivity.mFileOpsHelper.extractFile(currentFile, newFile);
                } else {
                    File newFile = new File(currentDir + "/" + fileName);
                    File currentFile = new File(currentFilePath);
                    if (FileUtils.isFileExisting(currentDir, newFile.getName())) {
                        editFileName.setError(getResources().getString(R.string
                                .dialog_title_paste_conflict));
                        return;
                    }
                    aceActivity.mFileOpsHelper.extractFile(currentFile, newFile);
                }
                setBackPressed();
                materialDialog.dismiss();
            }
        });

        materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDialog.dismiss();
            }
        });

        materialDialog.show();


    }

    private int checkTheme() {
        int theme = ThemeUtils.getTheme(getActivity());

        if (theme == FileConstants.THEME_DARK) {
            return R.style.DarkAppTheme_NoActionBar;
        } else {
            return R.style.AppTheme_NoActionBar;
        }
    }


    private void updateFavouritesGroup(ArrayList<FileInfo> fileInfoList) {
        ArrayList<FavInfo> favInfoArrayList = new ArrayList<>();
        for (int i = 0; i < fileInfoList.size(); i++) {
            FileInfo info = fileInfoList.get(i);
            String name = info.getFileName();
            String path = info.getFilePath();
            FavInfo favInfo = new FavInfo();
            favInfo.setFileName(name);
            favInfo.setFilePath(path);
            SharedPreferenceWrapper sharedPreferenceWrapper = new SharedPreferenceWrapper();
            sharedPreferenceWrapper.addFavorite(getActivity(), favInfo);
            favInfoArrayList.add(favInfo);
        }

        aceActivity.updateFavourites(favInfoArrayList);
    }

    private void removeFavorite(ArrayList<FileInfo> fileInfoList) {
        ArrayList<FavInfo> favInfoArrayList = new ArrayList<>();
        for (int i = 0; i < fileInfoList.size(); i++) {
            FileInfo info = fileInfoList.get(i);
            String name = info.getFileName();
            String path = info.getFilePath();
            FavInfo favInfo = new FavInfo();
            favInfo.setFileName(name);
            favInfo.setFilePath(path);
            SharedPreferenceWrapper sharedPreferenceWrapper = new SharedPreferenceWrapper();
            sharedPreferenceWrapper.removeFavorite(getActivity(), favInfo);
            favInfoArrayList.add(favInfo);
        }
        refreshList();
        aceActivity.removeFavourites(favInfoArrayList);
    }


    private void togglePasteVisibility(boolean isVisible) {
        mPasteItem.setVisible(isVisible);
        mIsPasteItemVisible = isVisible;
    }

    @SuppressWarnings("ConstantConditions")
    private void showInfoDialog(FileInfo fileInfo) {
        String title = getString(R.string.properties);
        String texts[] = new String[]{title, getString(R.string.msg_ok), "", null};
        final MaterialDialog materialDialog = new DialogUtils().showCustomDialog(getActivity(),
                R.layout.dialog_file_properties, texts);
        View view = materialDialog.getCustomView();
        ImageView imageFileIcon = (ImageView) view.findViewById(R.id.imageFileIcon);
        TextView textFileName = (TextView) view.findViewById(R.id.textFileName);
        TextView textPath = (TextView) view.findViewById(R.id.textPath);
        TextView textFileSize = (TextView) view.findViewById(R.id.textFileSize);
        TextView textDateModified = (TextView) view.findViewById(R.id.textDateModified);
        TextView textHidden = (TextView) view.findViewById(R.id.textHidden);
        TextView textReadable = (TextView) view.findViewById(R.id.textReadable);
        TextView textWriteable = (TextView) view.findViewById(R.id.textWriteable);
        TextView textHiddenPlaceHolder = (TextView) view.findViewById(R.id.textHiddenPlaceHolder);
        TextView textReadablePlaceHolder = (TextView) view.findViewById(R.id
                .textReadablePlaceHolder);
        TextView textWriteablePlaceHolder = (TextView) view.findViewById(R.id
                .textWriteablePlaceHolder);
        TextView textMD5 = (TextView) view.findViewById(R.id.textMD5);
        TextView textMD5Placeholder = (TextView) view.findViewById(R.id.textMD5PlaceHolder);

        String path = fileInfo.getFilePath();
        String fileName = fileInfo.getFileName();
        String fileDate;
        if (FileUtils.isDateNotInMs(mCategory)) {
            fileDate = FileUtils.convertDate(fileInfo.getDate());
        } else {
            fileDate = FileUtils.convertDate(fileInfo.getDate() * 1000);
        }
        boolean isDirectory = fileInfo.isDirectory();
        String fileNoOrSize;
        if (isDirectory) {
            int childFileListSize = (int) fileInfo.getSize();
            if (childFileListSize == 0) {
                fileNoOrSize = getResources().getString(R.string.empty);
            } else if (childFileListSize == -1) {
                fileNoOrSize = "";
            } else {
                fileNoOrSize = getResources().getQuantityString(R.plurals.number_of_files,
                        childFileListSize, childFileListSize);
            }
        } else {
            long size = fileInfo.getSize();
            fileNoOrSize = Formatter.formatFileSize(getActivity(), size);
        }
        boolean isReadable = new File(path).canRead();
        boolean isWriteable = new File(path).canWrite();
        boolean isHidden = new File(path).isHidden();

        textFileName.setText(fileName);
        textPath.setText(path);
        textFileSize.setText(fileNoOrSize);
        textDateModified.setText(fileDate);

        if (mCategory != 0) {
            textMD5.setVisibility(View.GONE);
            textMD5Placeholder.setVisibility(View.GONE);
            textReadablePlaceHolder.setVisibility(View.GONE);
            textWriteablePlaceHolder.setVisibility(View.GONE);
            textHiddenPlaceHolder.setVisibility(View.GONE);
            textReadable.setVisibility(View.GONE);
            textWriteable.setVisibility(View.GONE);
            textHidden.setVisibility(View.GONE);
        } else {
            textReadable.setText(isReadable ? getString(R.string.yes) : getString(R.string.no));
            textWriteable.setText(isWriteable ? getString(R.string.yes) : getString(R.string.no));
            textHidden.setText(isHidden ? getString(R.string.yes) : getString(R.string.no));
        }

        if (new File(path).isDirectory()) {
            textMD5.setVisibility(View.GONE);
            textMD5Placeholder.setVisibility(View.GONE);
            Drawable apkIcon = FileUtils.getAppIconForFolder(getActivity(), fileName);
            if (apkIcon != null) {
                imageFileIcon.setImageDrawable(apkIcon);
            } else {
                imageFileIcon.setImageResource(R.drawable.ic_folder);
            }
        } else {
            if (mCategory == 0) {
                String md5 = FileUtils.getFastHash(path);
                textMD5.setText(md5);
            }

            if (fileInfo.getType() == FileConstants.CATEGORY.VIDEO.getValue()) {
                Uri videoUri = Uri.fromFile(new File(path));
                Glide.with(getActivity()).load(videoUri).centerCrop()
                        .placeholder(R.drawable.ic_movie)
                        .crossFade(2)
                        .into(imageFileIcon);
            } else if (fileInfo.getType() == FileConstants.CATEGORY.IMAGE.getValue()) {
                Uri imageUri = Uri.fromFile(new File(path));
                Glide.with(getActivity()).load(imageUri).centerCrop()
                        .crossFade(2)
                        .placeholder(R.drawable.ic_image_default)
                        .into(imageFileIcon);
            } else if (fileInfo.getType() == FileConstants.CATEGORY.AUDIO.getValue()) {
                imageFileIcon.setImageResource(R.drawable.ic_music_default);
            } else if (fileInfo.getExtension().equals(FileConstants.APK_EXTENSION)) {
                Drawable apkIcon = FileUtils.getAppIcon(getActivity(), path);
                imageFileIcon.setImageDrawable(apkIcon);
            } else {
                imageFileIcon.setImageResource(R.drawable.ic_doc_white);
            }
        }

        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                materialDialog.dismiss();
            }
        });

        materialDialog.show();

    }


    private BitmapDrawable writeOnDrawable(String text) {

        Bitmap bm = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        bm.eraseColor(Color.DKGRAY);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        int countFont = getResources()
                .getDimensionPixelSize(R.dimen.drag_shadow_font);
        paint.setTextSize(countFont);

        Canvas canvas = new Canvas(bm);
        int strLength = (int) paint.measureText(text);
        int x = bm.getWidth() / 2 - strLength;

        // int y = s.titleOffset;
        int y = (bm.getHeight() - countFont) / 2;
//        drawText(canvas, x, y, title, labelWidth - s.leftMargin - x
//                - s.titleRightMargin, mTitlePaint);

        canvas.drawText(text, x, y - paint.getFontMetricsInt().ascent, paint);
//        canvas.drawText(text, bm.getWidth() / 2, bm.getHeight() / 2, paint);

        return new BitmapDrawable(getActivity().getResources(), bm);
    }


    private class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable thing
        private final Drawable shadow;

        // Defines the constructor for myDragShadowBuilder
        MyDragShadowBuilder(View v, int count) {

            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);
            // Creates a draggable image that will fill the Canvas provided by the system.
            shadow = writeOnDrawable("" + count);

        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            // Defines local variables
            int width, height;

            // Sets the width of the shadow to half the width of the original View
            width = getView().getWidth() / 6;
//            width = 100;
            Log.d(TAG, "width=" + width);

            // Sets the height of the shadow to half the height of the original View
            height = getView().getHeight() / 2;
//            height = 100;

            Log.d(TAG, "height=" + height);


            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow.setBounds(0, 0, width, height);

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);
            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(2 * width, height * 2);
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draws the ColorDrawable in the Canvas passed in from the system.
            shadow.draw(canvas);
        }
    }

    private void showDragDialog(final ArrayList<FileInfo> sourcePaths, final String destinationDir) {

        int color = new DialogUtils().getCurrentThemePrimary(getActivity());
        boolean canWrite = new File(destinationDir).canWrite();
        Logger.log(TAG, "Can write=" + canWrite);

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        CharSequence items[] = new String[]{getString(R.string.action_copy), getString(R.string.move)};
        builder.title(getString(R.string.drag));
        builder.content(getString(R.string.dialog_to_placeholder) + " " + destinationDir);
        builder.positiveText(getString(R.string.msg_ok));
        builder.positiveColor(color);
        builder.items(items);
        builder.negativeText(getString(R.string.dialog_cancel));
        builder.negativeColor(color);
        builder.itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {

                final boolean isMoveOperation = position == 1;
                ArrayList<FileInfo> info = new ArrayList<>();
                info.addAll(sourcePaths);
                PasteConflictChecker conflictChecker = new PasteConflictChecker(aceActivity, destinationDir,
                        mIsRootMode, isMoveOperation, info);
                conflictChecker.execute();
                clearSelectedPos();
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                return true;
            }
        });

        final MaterialDialog materialDialog = builder.build();

        materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActionMode != null)
                    mActionMode.finish();
                materialDialog.dismiss();
            }
        });

        materialDialog.show();
    }

    private class myDragEventListener implements View.OnDragListener {

        int oldPos = -1;

        // This is the method that the system calls when it dispatches a drag event to the
        // listener.
        public boolean onDrag(View v, DragEvent event) {

            // Defines a variable to store the action type for the incoming event
            final int action = event.getAction();

            // Handles each of the expected events
            switch (action) {

                case DragEvent.ACTION_DRAG_STARTED:

                    Log.d(TAG, "DRag started" + v);

                    // Determines if this View can accept the dragged data
                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT)) {

                        // returns true to indicate that the View can accept the dragged data.
                        return true;

                    }

                    // Returns false. During the current drag and drop operation, this View will
                    // not receive events again until ACTION_DRAG_ENDED is sent.
                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d(TAG, "DRag entered");
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:

                    View onTopOf = recyclerViewFileList.findChildViewUnder(event.getX(), event.getY());
                    int newPos = recyclerViewFileList.getChildAdapterPosition(onTopOf);
//                    Log.d(TAG, "DRag location --pos=" + newPos);

                    if (oldPos != newPos && newPos != RecyclerView.NO_POSITION) {
/*                        int visiblePos = ((LinearLayoutManager) llm).findLastVisibleItemPosition();
                        if (newPos + 2 >= visiblePos) {
                            ((LinearLayoutManager) llm).scrollToPosition(newPos + 1);
                        }
//                        recyclerViewFileList.smoothScrollToPosition(newPos+2);
                        Logger.log(TAG, "drag old pos=" + oldPos + "new pos=" + newPos+"Last " +
                                "visible="+visiblePos);*/
                        // For scroll up
                        if (oldPos != RecyclerView.NO_POSITION && newPos < oldPos) {
                            int changedPos = newPos - 2;
                            Logger.log(TAG, "drag Location old pos=" + oldPos + "new pos=" + newPos +
                                    "changed pos=" + changedPos);
                            if (changedPos >= 0)
                                recyclerViewFileList.smoothScrollToPosition(changedPos);
                        } else {
                            int changedPos = newPos + 2;
                            // For scroll down
                            if (changedPos < fileInfoList.size())
                                recyclerViewFileList.smoothScrollToPosition(newPos + 2);
                            Logger.log(TAG, "drag Location old pos=" + oldPos + "new pos=" + newPos +
                                    "changed pos=" + changedPos);

                        }
                        oldPos = newPos;
                        fileListAdapter.setDraggedPos(newPos);
                    }
                    // Ignore the event
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d(TAG, "DRag exit");
                    fileListAdapter.clearDragPos();
                    mDragPaths = new ArrayList<>();
                    return true;

                case DragEvent.ACTION_DROP:
//                    Log.d(TAG,"DRag drop"+pos);

                    View top = recyclerViewFileList.findChildViewUnder(event.getX(), event.getY());
                    int position = recyclerViewFileList.getChildAdapterPosition(top);
                    Logger.log(TAG, "DROP new pos=" + position);
                    fileListAdapter.clearDragPos();
                    @SuppressWarnings("unchecked")
                    ArrayList<FileInfo> draggedFiles = (ArrayList<FileInfo>) event.getLocalState();
                    ArrayList<String> paths = new ArrayList<>();

                  /*  ArrayList<FileInfo> paths = dragData.getParcelableArrayListExtra(FileConstants
                            .KEY_PATH);*/

                    String destinationDir;
                    if (position != -1) {
                        destinationDir = fileInfoList.get(position).getFilePath();
                    } else {
                        destinationDir = mFilePath;
                    }

                    for (FileInfo info : draggedFiles) {
                        paths.add(info.getFilePath());
                    }

                    String sourceParent = new File(draggedFiles.get(0).getFilePath()).getParent();
                    if (!new File(destinationDir).isDirectory()) {
                        destinationDir = new File(destinationDir).getParent();
                    }

                    boolean value = destinationDir.equals(sourceParent);
                    Logger.log(TAG, "Source parent=" + sourceParent + " " + value);


                    if (!paths.contains(destinationDir)) {
                        if (!destinationDir.equals(sourceParent)) {
                            Logger.log(TAG, "Source parent=" + sourceParent + " Dest=" +
                                    destinationDir);
                            showDragDialog(draggedFiles, destinationDir);
                        } else {
                            final boolean isMoveOperation = false;
                            ArrayList<FileInfo> info = new ArrayList<>();
                            info.addAll(draggedFiles);
                            PasteConflictChecker conflictChecker = new PasteConflictChecker(aceActivity,
                                    destinationDir,
                                    mIsRootMode, isMoveOperation, info);
                            conflictChecker.execute();
                            clearSelectedPos();
                            Logger.log(TAG, "Source=" + draggedFiles.get(0) + "Dest=" + destinationDir);
                            mActionMode.finish();
                        }
                    }

                    mDragPaths = new ArrayList<>();
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:

                    View top1 = recyclerViewFileList.findChildViewUnder(event.getX(), event.getY());
                    int position1 = recyclerViewFileList.getChildAdapterPosition(top1);
                    @SuppressWarnings("unchecked")
                    ArrayList<FileInfo> dragPaths = (ArrayList<FileInfo>) event.getLocalState();


                    Logger.log(TAG, "DRAG END new pos=" + position1);
                    Logger.log(TAG, "DRAG END Local state=" + dragPaths);
                    Logger.log(TAG, "DRAG END result=" + event.getResult());
                    Logger.log(TAG, "DRAG END currentDirSingle=" + mLastSinglePaneDir);
                    Logger.log(TAG, "DRAG END currentDirDual=" + mLastDualPaneDir);
                    Log.d(TAG, "DRag end");
                    fileListAdapter.clearDragPos();
                    if (!event.getResult() && position1 == RecyclerView.NO_POSITION) {
                        ViewParent parent1 = v.getParent().getParent();

                        if (((View) parent1).getId() == R.id.frame_container_dual) {
                            Logger.log(TAG, "DRAG END parent dual =" + true);
/*                            FileListDualFragment dualPaneFragment = (FileListDualFragment)
                                    getFragmentManager()
                                            .findFragmentById(R
                                                    .id.frame_container_dual);
                            Logger.log(TAG, "DRAG END Dual dir=" + mLastDualPaneDir);

//                            Logger.log(TAG, "Source=" + mDragPaths.get(0) + "Dest=" + mLastDualPaneDir);
                            if (dualPaneFragment != null && new File(mLastDualPaneDir).list().length == 0 &&
                                    dragPaths.size() != 0) {
//                                if (!destinationDir.equals(paths.get(0))) {
                                showDragDialog(dragPaths, mLastDualPaneDir);
//                                }
                            }*/
                        } else {
                            Logger.log(TAG, "DRAG END parent dual =" + false);
                            BaseFileList singlePaneFragment = (BaseFileList)
                                    getFragmentManager()
                                            .findFragmentById(R
                                                    .id.main_container);
                            Logger.log(TAG, "DRAG END single dir=" + mLastSinglePaneDir);

//                            Logger.log(TAG, "Source=" + mDragPaths.get(0) + "Dest=" + mLastDualPaneDir);
                            if (singlePaneFragment != null && new File(mLastSinglePaneDir).list().length == 0 &&
                                    dragPaths.size() != 0) {
//                                if (!destinationDir.equals(paths.get(0))) {
                                showDragDialog(dragPaths, mLastSinglePaneDir);
//                                }
                            }
                        }

                    }
                    mDragPaths = new ArrayList<>();
                    // returns true; the value is ignored.
                    return true;

                // An unknown action type was received.
                default:
                    Log.e(TAG, "Unknown action type received by OnDragListener.");
                    break;
            }

            return false;
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(this.getClass().getSimpleName(), "onCreateOptionsMenu" + "Fragment=");

        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_base, menu);
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mPasteItem = menu.findItem(R.id.action_paste);
        mPasteItem.setVisible(mIsPasteItemVisible);
        viewMode = sharedPreferenceWrapper.getViewMode(getActivity());
        mViewItem = menu.findItem(R.id.action_view);
        updateMenuTitle();
        setupSearchView();
    }

    private void updateMenuTitle() {
        mViewItem.setTitle(viewMode == 0 ? R.string.action_view_grid : R.string.action_view_list);
    }

    private void setupSearchView() {
        // Disable full screen keyboard in landscape
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setOnQueryTextListener(this);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
/*        mSearchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)) {
                    // Perform action on key press
                    mainActivityHelper.search(searchViewEditText.getText().toString());
                    hideSearchView();
                    return true;
                }
                return false;
            }
        });*/
    }

    public void hideSearchView() {
        MenuItemCompat.collapseActionView(mSearchItem);

//        mSearchView.onActionViewCollapsed();
    }

    public void performVoiceSearch(String query) {
        mSearchView.setQuery(query, false);
    }

//    private  SearchTask searchTask;

    @Override
    public boolean onQueryTextChange(String query) {

        fileListAdapter.filter(query);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
/*        if (!query.isEmpty()) {
            if (searchTask == null) {
                searchTask = new SearchTask(this,query,mFilePath);

            }
            else {
                searchTask.execute(query);
            }
        }
        hideSearchView();*/
        return false;
    }


    private void clearSelectedPos() {
        mSelectedItemPositions = new SparseBooleanArray();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_paste:
                if (mCopiedData.size() > 0) {
                    ArrayList<FileInfo> info = new ArrayList<>();
                    info.addAll(mCopiedData);
                    PasteConflictChecker conflictChecker = new PasteConflictChecker(aceActivity, mFilePath,
                            mIsRootMode, mIsMoveOperation, info);
                    conflictChecker.execute();
                    clearSelectedPos();
                    mCopiedData.clear();
                    togglePasteVisibility(false);
                }
                break;

            case R.id.action_view:
                if (viewMode == FileConstants.KEY_LISTVIEW) {
                    viewMode = FileConstants.KEY_GRIDVIEW;
                } else {
                    viewMode = FileConstants.KEY_LISTVIEW;
                }
                sharedPreferenceWrapper.savePrefs(getActivity(), viewMode);
                switchView();
                updateMenuTitle();
                break;

            case R.id.action_sort:
                showSortDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        int color = new DialogUtils().getCurrentThemePrimary(getActivity());

        final MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        CharSequence items[] = new String[]{getString(R.string.sort_name), getString(R.string.sort_name_desc),
                getString(R.string.sort_type), getString(R.string.sort_type_desc),
                getString(R.string.sort_size), getString(R.string.sort_size_desc),
                getString(R.string.sort_date), getString(R.string.sort_date_desc)};
        builder.title(getString(R.string.action_sort));
        builder.positiveText(getString(R.string.dialog_cancel));
        builder.positiveColor(color);
        builder.items(items);

        builder.alwaysCallSingleChoiceCallback();
        builder.itemsCallbackSingleChoice(getSortMode(), new MaterialDialog.ListCallbackSingleChoice() {
            @Override
            public boolean onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                persistSortMode(position);
                refreshList();
                dialog.dismiss();
                return true;
            }
        });

        final MaterialDialog materialDialog = builder.build();
        materialDialog.show();
    }

    private void persistSortMode(int sortMode) {
        preferences.edit().putInt(FileConstants.KEY_SORT_MODE, sortMode).apply();
    }

    private int getSortMode() {
        return preferences.getInt(FileConstants.KEY_SORT_MODE, FileConstants.KEY_SORT_NAME);
    }

    private void switchView() {
        fileListAdapter = null;
        recyclerViewFileList.setHasFixedSize(true);

        if (viewMode == FileConstants.KEY_LISTVIEW) {
            llm = new CustomLayoutManager(getActivity());
            recyclerViewFileList.setLayoutManager(llm);

        } else {
            refreshSpan();
        }

        mStopAnim = true;

        fileListAdapter = new FileListAdapter(getContext(), fileInfoList,
                mCategory, viewMode);

        recyclerViewFileList.setAdapter(fileListAdapter);
        if (viewMode == FileConstants.KEY_LISTVIEW) {
            if (mGridItemDecoration != null) {
                recyclerViewFileList.removeItemDecoration(mGridItemDecoration);
            }
            if (mDividerItemDecoration == null) {
                mDividerItemDecoration = new DividerItemDecoration(getActivity(), mIsDarkTheme);
            }
            mDividerItemDecoration.setOrientation();
            recyclerViewFileList.addItemDecoration(mDividerItemDecoration);
        } else {
            if (mDividerItemDecoration != null) {
                recyclerViewFileList.removeItemDecoration(mDividerItemDecoration);
            }
            addItemDecoration();
        }

        initializeListeners();

    }

    /**
     * Show dual pane in Landscape mode
     */
    public void showDualPane() {

        // For Files category only, show dual pane
            mIsDualModeEnabled = true;
            refreshSpan();
//            createDualFragment();
    }


    public void refreshSpan() {
        if (viewMode == ViewMode.GRID) {
            mIsDualModeEnabled = preferences
                    .getBoolean(FileConstants.PREFS_DUAL_PANE, false);
            if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT || !mIsDualModeEnabled ||
                    checkIfLibraryCategory(category)) {
                gridCols = getResources().getInteger(R.integer.grid_columns);
            } else {
                gridCols = getResources().getInteger(R.integer.grid_columns_dual);
            }
            Log.d(TAG, "Refresh span--columns=" + gridCols + "category=" + category + " dual mode=" +
                    mIsDualModeEnabled);

            llm = new CustomGridLayoutManager(getActivity(), gridCols);
            recyclerViewFileList.setLayoutManager(llm);
        }
    }

    @Override
    public void onDestroyView() {
        recyclerViewFileList.stopScroll();
        if (!mInstanceStateExists) {
            preferences.edit().putInt(FileConstants.KEY_GRID_COLUMNS, gridCols).apply();
            sharedPreferenceWrapper.savePrefs(getActivity(), viewMode);
        }
        removeSearchTask();

        refreshData = null;
        if (clearCache) {
            clearCache();
            clearCache = false;
        }
        if (fileListAdapter != null) {
            fileListAdapter.onDetach();
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @SuppressWarnings("EmptyMethod")
    public void removeSearchTask() {

     /*   if (searchTask != null) {
            searchTask.searchAsync.cancel(true);
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        aceActivity = null;
    }

    private void clearCache() {
        String path = createCacheDirExtract();
        if (path != null) {
            File[] files = new File(path).listFiles();

            if (files != null) {
                for (File file : files)
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
            }
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        Logger.log(TAG, "onConfigurationChanged " + newConfig.orientation);
        if (mCurrentOrientation != newConfig.orientation) {
            mCurrentOrientation = newConfig.orientation;
            refreshSpan();
        }

    }
}
