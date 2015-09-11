package reach.project.coreViews;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;


import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.localytics.android.Localytics;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.SharedPrefUtils;

public class InviteFragment extends Fragment {

    private static WeakReference<InviteFragment> reference = null;
    public static InviteFragment newInstance() {
        InviteFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new InviteFragment());
        }
        else {
            Log.i("Ayush", "Reusing invite fragment object :)");
        }
        return fragment;
    }

    private class InviteListAdapter extends ArrayAdapter<String>{

        private final int layoutResourceId;
        private final int [] iconIds = {
                        R.drawable.whatsapp2,
                        R.drawable.messenger,
                        R.drawable.twitter,
                        R.drawable.google_plus2};

        private final class ViewHolder {

            private final ImageView listImage;
            private final TextView listTitle;

            private ViewHolder(ImageView listImage, TextView listTitle) {
                this.listImage = listImage;
                this.listTitle = listTitle;
            }
        }

        public InviteListAdapter(Context context, int resource, String[] list) {
            super(context, resource, list);
            this.layoutResourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final ViewHolder viewHolder;
            if(convertView==null){

                convertView = ((Activity)parent.getContext()).getLayoutInflater().inflate(layoutResourceId, parent, false);
                viewHolder = new ViewHolder(
                        (ImageView) convertView.findViewById(R.id.listImage),
                        (TextView) convertView.findViewById(R.id.listTitle));
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.listTitle.setText(getItem(position));
            Picasso.with(convertView.getContext()).load(iconIds[position]).into(viewHolder.listImage);
            return convertView;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_invite, container, false);

        Toolbar mToolbar = (Toolbar)rootView.findViewById(R.id.inviteToolbar);
        mToolbar.setTitle("Invite Friends");
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        final ListView inviteList = (ListView) rootView.findViewById(R.id.listView);
        final SharedPreferences preferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final String[] inviteOptions = {"Whatsapp","Facebook Messenger","Twitter","Google+"};
        final String [] packageNames = {"com.whatsapp","com.facebook.orca","com.twitter.android","com.google.android.apps.plus"};

        inviteList.setAdapter(new InviteListAdapter(getActivity(),R.layout.invite_list_item,inviteOptions));
        inviteList.setOnItemClickListener((parent, view, position, id) -> {

            if (!StaticData.debugMode) {
                ((ReachApplication)getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Invite Page")
                        .setAction("User Name - " + SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                        .setLabel(inviteOptions[position])
                        .setValue(1)
                        .build());
                Map<String, String> tagValues = new HashMap<>();
                tagValues.put("User Name", SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
                tagValues.put("Option", inviteOptions[position]);
                Localytics.tagEvent("Invite Page", tagValues);
            }
            final Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey! Checkout and download my phone Music collection with just a click!" +
                            ".\nhttp://msg.mn/reach\n--\n"+SharedPrefUtils.getUserName(preferences));
            sendIntent.setType("text/plain");
            sendIntent.setPackage(packageNames[position]);
            try{
                startActivity(sendIntent);
            }
            catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(getActivity(),inviteOptions[position]+" is not Installed",Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }
}
