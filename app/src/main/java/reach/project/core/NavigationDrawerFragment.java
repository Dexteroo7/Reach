package reach.project.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.database.ReachDatabase;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.viewHelpers.CircleTransform;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private SuperInterface mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private String[] priList;
    private View mFragmentContainerView;
    private View rootView;

    private int mCurrentSelectedPosition = 0;
    private boolean mUserLearnedDrawer;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);
        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }
        selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView = (ListView) rootView.findViewById(R.id.primaryListView);
        //mDrawerSecListView = (ListView) rootView.findViewById(R.id.secondaryListView);
        final SharedPreferences sharedPrefs = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final SwitchCompat netToggle = (SwitchCompat) rootView.findViewById(R.id.netToggle);
        if (SharedPrefUtils.getMobileData(sharedPrefs))
            netToggle.setChecked(true);
        else
            netToggle.setChecked(false);
        netToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    SharedPrefUtils.setDataOn(sharedPrefs.edit());
                else {
                    SharedPrefUtils.setDataOff(sharedPrefs.edit());
                    ////////////////////purge all upload operations, but retain paused operations
                    getActivity().getContentResolver().delete(
                            ReachDatabaseProvider.CONTENT_URI,
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                            ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                            new String[]{1 + "", ReachDatabase.PAUSED_BY_USER + ""});
                }
            }
        });
        priList = new String[]{
                getString(R.string.title_section1),
                getString(R.string.title_section2),
                getString(R.string.title_section3),
                getString(R.string.title_section4),
                getString(R.string.title_section5),
                getString(R.string.title_section6)
        };
        int priImageRes[] = {
                R.drawable.icon_grey,
                R.drawable.audio_grey,
                R.drawable.promo,
                R.drawable.clock,
                R.drawable.add_icon_grey,
                R.drawable.sheet_grey
        };
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setSelected(true);
                selectItem(position);
            }
        });
        final RelativeLayout navHeader = (RelativeLayout) rootView.findViewById(R.id.navHeader);
        navHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onOpenProfile();
            }
        });


        FrameLayout footer = (FrameLayout) rootView.findViewById(R.id.footer);
        footer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=reach.project")));
            }
        });
        mDrawerListView.setAdapter(new DrawerListAdapter(getActivity(), R.layout.listview_item, priList, priImageRes));
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        setNavViews(getActivity());
        return rootView;
    }

    public void setNavViews(Context context){

        final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final String path = SharedPrefUtils.getImageId(sharedPreferences);
        if(path != null && !path.equals("") && !path.equals("hello_world")) {
            //Path must not be empty exception
            Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl + path).transform(new CircleTransform())
                    .into(((ImageView) rootView.findViewById(R.id.userImageNav)));
        }
        ((TextView)rootView.findViewById(R.id.userNameNav))
                .setText(SharedPrefUtils.getUserName(sharedPreferences));

        ////////////////////
        final Cursor countCursor = context.getContentResolver().query(
                ReachSongProvider.CONTENT_URI,
                ReachSongHelper.projection,
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{SharedPrefUtils.getServerId(sharedPreferences) + ""},
                null);
        if(countCursor == null) return;
        if(!countCursor.moveToFirst()) {
            countCursor.close();
            return;
        }
        final long count = countCursor.getCount();
        countCursor.close();
        ((TextView) rootView.findViewById(R.id.numberOfSongsNav)).setText(count + " Songs");
    }

    private class DrawerListAdapter extends ArrayAdapter<String>{

        private final Context mContext;
        private final int layoutResourceId;
        private final String txt[];
        private final int imageRes[];

        private DrawerListAdapter(Context context, int resource, String[] text, int images[]) {

            super(context, resource, text);
            this.layoutResourceId = resource;
            this.mContext = context;
            this.txt = text;
            this.imageRes = images;
        }

        private final class ViewHolder {

            private final ImageView listImage;
            private final TextView textViewItem;

            private ViewHolder(ImageView listImage, TextView textViewItem) {
                this.listImage = listImage;
                this.textViewItem = textViewItem;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final ViewHolder viewHolder;

            if(convertView==null){

                convertView = ((Activity) mContext).getLayoutInflater().inflate(layoutResourceId, parent, false);
                viewHolder = new ViewHolder(
                        (ImageView) convertView.findViewById(R.id.listImage),
                        (TextView) convertView.findViewById(R.id.listTitle));
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            Picasso.with(parent.getContext()).load(imageRes[position]).into(viewHolder.listImage);
            viewHolder.textViewItem.setText(txt[position]);
            return convertView;
        }
    }


    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {

        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {

                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }
                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {

                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }
                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    final SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }
                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        /*if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }*/
        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void selectItem(int position) {

        mCurrentSelectedPosition = position;
        if (priList!=null) {
            if (position < priList.length) {
                if (mDrawerListView != null)
                    mDrawerListView.setItemChecked(position, true);
            } /*else {
                if (mDrawerSecListView != null)
                    mDrawerSecListView.setItemChecked(position, true);
            }*/
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        try {
            mCallbacks = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle!=null&&mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        /*if (item.getItemId() == R.id.action_example) {
            Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT).show();
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        //actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getSupportActionBar() {
        return ((ActionBarActivity)getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
}
