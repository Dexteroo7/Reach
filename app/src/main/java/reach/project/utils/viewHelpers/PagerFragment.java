package reach.project.utils.viewHelpers;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;

import com.astuetz.PagerSlidingTabStrip;

import java.lang.reflect.Method;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.SearchResultsActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.music.myLibrary.MyLibraryFragment;
import reach.project.coreViews.fileManager.myfiles_search.MyFilesSearchFragment;
import reach.project.utils.MiscUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;

/**
 * A placeholder fragment containing a simple view.
 */
public class PagerFragment extends Fragment {

    public static final String PARCEL_PAGER = "PARCEL_PAGER";
    private static final int TRIGGER_SERACH =123;
    public SearchView searchView;
    private boolean isSearchViewFragVisible = false;
    private static final String TAG = PagerFragment.class.getSimpleName();
    private MyFilesSearchFragment frag;

    public static class Pages implements Parcelable {

        public Pages(Class<Fragment>[] classes, String[] titles, String headerText) {

            setClasses(classes);
            this.titles = titles;
            this.header = headerText;
        }

        public static final Parcelable.Creator<Pages> CREATOR = new Parcelable.Creator<Pages>() {
            @Override
            public Pages createFromParcel(Parcel in) {
                return new Pages(in);
            }

            @Override
            public Pages[] newArray(int size) {
                return new Pages[size];
            }
        };

        public void setClasses(@NonNull Class<Fragment>[] realClasses) {

            classes = new String[realClasses.length];
            for (int index = 0; index < realClasses.length; index++)
                classes[index] = realClasses[index].getName();
        }

        public String[] classes;
        public String[] titles;
        public String header;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(classes);
            dest.writeStringArray(titles);
            dest.writeString(header);
        }

        private Pages(Parcel in) {

            this.classes = in.createStringArray();
            this.titles = in.createStringArray();
            this.header = in.readString();
        }
    }

    public static Bundle getBundle(String pageTitle, @NonNull Pages... pages) {

        //sanity check
        for (Pages page : pages) {

            if (page.classes == null || page.titles == null)
                throw new IllegalArgumentException("Page title or fragment not found");

            if (page.classes.length != page.titles.length)
                throw new IllegalArgumentException("Page title and fragment count mismatch");
        }

        final Bundle bundle = new Bundle(2);
        bundle.putParcelableArray(PARCEL_PAGER, pages);
        bundle.putString("pageTitle", pageTitle);
        return bundle;
    }

    public void setInnerItem(int position, int innerPosition) {

        setItem(position);
        if (viewPager == null || fragments == null || fragments[position] == null)
            return;
        PagerInnerFragment pagerInnerFragment = (PagerInnerFragment) fragments[position];
        pagerInnerFragment.setItem(innerPosition);
    }

    public void setItem(int position) {

        if (viewPager != null)
            viewPager.setCurrentItem(position, true);
    }

    @Nullable
    private ViewPager viewPager = null;
    @Nullable
    private SuperInterface mListener = null;

    private Fragment[] fragments = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_pager, container, false);
        Log.d("Ashish", "PagerFragment - onCreateView");

        final PagerSlidingTabStrip tabLayout = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabLayoutPager);
        final Bundle arguments = getArguments();
        FrameLayout searchFragmentContainer = (FrameLayout) rootView.findViewById(R.id.search_results_fragment_container);
        FrameLayout viewPagerContainer = (FrameLayout) rootView.findViewById(R.id.viewPagerContainer);
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.pagerToolbar);
        String title = arguments.getString("pageTitle");
        toolbar.setTitle(title);
        toolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);
        viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        if (title != null) {

            if (title.equals("Share"))
                toolbar.inflateMenu(R.menu.menu_push);
            else {
                toolbar.inflateMenu(R.menu.pager_menu);
                SearchManager searchManager =
                        (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
                MenuItem searchViewMenuItem = toolbar.getMenu().findItem(R.id.search);
                searchView =
                        (SearchView) searchViewMenuItem.getActionView();

                //ComponentName componentName = new ComponentName(getContext(), SearchResultsActivity.class);
                searchView.setQueryHint("Search your files");
                //searchView.setSearchableInfo(
                 //       searchManager.getSearchableInfo(componentName));
                frag = new MyFilesSearchFragment();

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    Bundle bundle = new Bundle();
                    Message message = new Message();
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {

                        if(frag!=null){
                            if(!frag.isAdded())
                                return true;
                            handler.removeMessages(TRIGGER_SERACH);
                            final Message obtained = handler.obtainMessage(TRIGGER_SERACH);
                            bundle.putString(StaticData.FILTER_STRING_KEY,newText.toLowerCase());
                            if(obtained == null) {
                                message.setData(bundle);
                                message.what = TRIGGER_SERACH;
                                handler.sendMessageDelayed(message, 150);
                            }
                            else{
                                obtained.setData(bundle);
                                handler.sendMessageDelayed(obtained,150);
                            }
                            //frag.filter(newText.toLowerCase());
                        }

                        return true;
                    }
                });

                MenuItemCompat.setOnActionExpandListener(searchViewMenuItem, new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        if(!isSearchViewFragVisible) {
                            tabLayout.setVisibility(View.GONE);
                            searchFragmentContainer.setVisibility(View.VISIBLE);
                            if(frag == null){
                                frag = new MyFilesSearchFragment();
                            }
                            getFragmentManager().beginTransaction().replace(R.id.search_results_fragment_container,frag ).commit();
                            isSearchViewFragVisible = true;
                            viewPagerContainer.setVisibility(View.GONE);
                            //CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) viewPagerContainer.getLayoutParams();
                            //params.setBehavior( null );
                            AppBarLayout.LayoutParams params1 = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                            params1.setScrollFlags(0);
                            Log.d(TAG, "onClick: searchview frag is now visible");
                            FragmentTabHost mTabHost = ((ReachActivity) getActivity()).mTabHost;
                            if(mTabHost!=null){
                                mTabHost.setVisibility(View.GONE);
                            }


                        }
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {

                        getFragmentManager().beginTransaction().remove(frag).commit();
                        isSearchViewFragVisible = false;
                        tabLayout.setVisibility(View.VISIBLE);
                        searchFragmentContainer.setVisibility(View.GONE);
                        FragmentTabHost mTabHost = ((ReachActivity) getActivity()).mTabHost;
                        AppBarLayout.LayoutParams params1 = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                        params1.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL| AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                        if(mTabHost!=null){
                            mTabHost.setVisibility(View.VISIBLE);
                        }

                        viewPagerContainer.setVisibility(View.VISIBLE);
                        Log.d(TAG, "onClick: searchview frag is now invisible");
                        return true;
                    }
                });

                /*searchView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Searchview onClick: ");
                    }
                });



                searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                    @Override
                    public boolean onClose() {

                        return false;
                    }
                });
                searchView.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });*/

                /*final AutoCompleteTextView searchEditText = (AutoCompleteTextView) searchView.findViewById(R.id.search_src_text);
                //searchEditText.setTextColor(Color.BLACK);
                final View dropDownAnchor = searchView.findViewById(searchEditText.getDropDownAnchor());
                if (dropDownAnchor != null) {
                    dropDownAnchor.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                        @Override
                        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {

                            // calculate width of DropdownView


                            int point[] = new int[2];
                            dropDownAnchor.getLocationOnScreen(point);
                            // x coordinate of DropDownView
                            int dropDownPadding = point[0] + searchEditText.getDropDownHorizontalOffset();

                            Rect screenSize = new Rect();
                            getActivity().getWindowManager().getDefaultDisplay().getRectSize(screenSize);
                            // screen width
                            int screenWidth = screenSize.width();

                            // set DropDownView width
                            searchEditText.setDropDownWidth(screenWidth *//*- dropDownPadding * 2*//*);
                            searchEditText.setDropDownVerticalOffset(MiscUtils.dpToPx(16));
                        }
                    });
                }*/
            }
        }

        final Parcelable[] parcelables = arguments.getParcelableArray(PARCEL_PAGER);
        if (parcelables == null || parcelables.length == 0 || !(parcelables instanceof Pages[]))
            return null;

        final Pages[] pages = (Pages[]) parcelables;
        final String[] titles = new String[pages.length];
        fragments = new Fragment[pages.length];

        for (int index = 0; index < pages.length; index++) {

            titles[index] = pages[index].header;
            if (pages[index].classes.length > 1)
                fragments[index] = PagerInnerFragment.getNewInstance(pages[index]);
            else if (pages[index].classes.length == 1) {

                try {
                    final Class fragmentClass = Class.forName(pages[index].classes[0]);
                    final Method invoker = fragmentClass.getMethod("getInstance", String.class);
                    fragments[index] = (Fragment) invoker.invoke(null, pages[index].header);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        //noinspection ConstantConditions
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }

            @Override
            public int getCount() {
                return fragments.length;
            }
        });

        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(20)));
        /*viewPager.setPageTransformer(true, (view, position) -> {

            if (position <= 1) {

                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                if (position < 0)
                    view.setTranslationX(horzMargin - vertMargin / 2);
                else
                    view.setTranslationX(-horzMargin + vertMargin / 2);

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        });*/
        tabLayout.setViewPager(viewPager);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TRIGGER_SERACH) {
                if(frag == null)
                    return;
                else{
                    frag.filter(msg.getData().getString(StaticData.FILTER_STRING_KEY));
                }
            }
        }
    };


}