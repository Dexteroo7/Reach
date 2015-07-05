package reach.project.coreViews;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import reach.project.R;
import reach.project.utils.MiscUtils;

public class FAQFragment extends Fragment {

    public static FAQFragment newInstance() {
        return new FAQFragment();
    }

    private static final ImmutableList<String> listDataHeader = ImmutableList.of(
            "What is Reach? How does it work?",
            "Do I need to be on an internet connection to use the app?",
            "Does the Peer device have to be on internet connection?",
            "Can I hide my content? How can I do that?",
            "What happens to the new songs I add to my device?  How can I add them to Reach?",
            "Can I listen to music on Reach Queue without an internet connection?",
            "What do the “Downloads” and “Uploads” tabs in Reach Queue do?",
            "Will Reach be available on iOS?",
            "How do i contact the Reach team?");

    private static final ImmutableMap<String, String> listDataChild = ImmutableMap.<String, String>builder()
            .put(listDataHeader.get(0), "Reach is a cross platform multimedia file access application which lets you browse through the files on the mobile devices of your peers. The Beta version of the application is launched for music files only, but will soon operate for other media files too. \n" +
                    "Reach works on a P2P (Peer to Peer) platform giving its users complete autonomy in the process of file sharing. There is no middle technology involved (traditionally the Cloud) which stores each file transferred. Every file a user accesses on Reach goes from one device directly to the other, no baggage!")
            .put(listDataHeader.get(1), "Yes, Reach uses the power of the internet to bring to you the ultimate sharing experience.")
            .put(listDataHeader.get(2), "The Peer to Peer technology requires both users to be on an internet connection for the transfer to take place. ")
            .put(listDataHeader.get(3), "We at Reach are all about what our users want. Yes, we give you the Privacy settings that make you comfortable in your network. You can hide as many files as you want, just tap on the file you wish to be invisible and wait for it turn grey. Tap again to unhide it.")
            .put(listDataHeader.get(4), "You don’t have to! Reach syncs your media library to the application itself.")
            .put(listDataHeader.get(5), "Yes! Once the song is fully queued, or buffered, it doesn’t need internet to play. You can enjoy your favourite music on the go!")
            .put(listDataHeader.get(6), "The Reach Queue is like your personal music log. Every song a user gains or gives access to, is stored here. The “Downloads” tab is a list of every song queued by you from a friend’s device. The “Uploads” tab is a list of the songs your friends have listened to from your device. ")
            .put(listDataHeader.get(7), "Soon. We’re working on it.")
            .put(listDataHeader.get(8), "Mail us at info@letsreach.co")
            .build();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_faq, container, false);
        final ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("FAQ");
        final ExpandableListView expandableListView = (ExpandableListView) rootView.findViewById(R.id.faqList);
        final LinearLayout headerLayout = new LinearLayout(getActivity());
        final TextView headerText = new TextView(getActivity());
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        final int margin = MiscUtils.dpToPx(10);

        params.setMargins(margin, margin, margin, margin * 2);
        headerText.setLayoutParams(params);
        headerText.setText("We love hearing from you. Every question you ask us gives us the inspiration to see our product critically and think on lines of what our users like. Our entire team is working hard to give you the best experience of our product and if there is something you’d like to ask us, go ahead and contact us, we’re very easily Reach-able!\n\nWe’ve, however, compiled a set of FAQ to save you time. Scroll down to see if we’ve answered your question.");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        headerLayout.addView(headerText);
        expandableListView.addHeaderView(headerLayout);
        expandableListView.setAdapter(new FAQAdapter());
        return rootView;
    }

    //TODO can we use viewHolders ?
    private class FAQAdapter extends BaseExpandableListAdapter {

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return listDataChild.get(listDataHeader.get(groupPosition));
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = ((LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.list_item, null);

            ((TextView) convertView
                    .findViewById(R.id.lblListItem)).setText((String) getChild(groupPosition, childPosition));
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return listDataHeader.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return listDataHeader.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = ((LayoutInflater) parent.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_group, null);

            ((TextView) convertView.findViewById(R.id.hCount)).setText(String.valueOf(groupPosition + 1) + ". ");
            ((TextView) convertView.findViewById(R.id.lblListHeader)).setText((String) getGroup(groupPosition));
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
