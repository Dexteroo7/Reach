package reach.project.coreViews;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;


public class InviteFragment extends Fragment {

    private static WeakReference<InviteFragment> reference = null;
    public static InviteFragment newInstance(boolean first) {

        final Bundle args;
        InviteFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new InviteFragment());
            fragment.setArguments(args = new Bundle());
        }
        else {
            Log.i("Ayush", "Reusing invite fragment object :)");
            args = fragment.getArguments();
        }
        args.putBoolean("first", first);
        return fragment;
    }

    public InviteFragment() {
    }
    private SuperInterface mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnInviteDialogListener");
        }
    }

    private class InviteListAdapter extends ArrayAdapter<String>{

        private final int layoutResourceId;
        private final int [] iconIds = {
                        R.drawable.whatsapp2,
                        R.drawable.messenger,
                        R.drawable.twitter,
                        R.drawable.google_plus2};
        private final int [] divider = {
                R.color.reach_color,
                R.color.reach_color,
                R.color.reach_color,0};

        private final class ViewHolder {

            private final ImageView listImage;
            private final TextView listTitle;
            private final View dividerFooter;

            private ViewHolder(ImageView listImage, TextView listTitle, View dividerFooter) {
                this.listImage = listImage;
                this.listTitle = listTitle;
                this.dividerFooter = dividerFooter;
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
                        (TextView) convertView.findViewById(R.id.listTitle),
                        convertView.findViewById(R.id.dividerFooter));
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.listTitle.setText(getItem(position));
            Picasso.with(convertView.getContext()).load(iconIds[position]).into(viewHolder.listImage);
            viewHolder.dividerFooter.setBackgroundResource(divider[position]);
            return convertView;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_invite, container, false);
        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("Invite Friends");
        final ListView inviteList = (ListView) rootView.findViewById(R.id.listView);

        final SharedPreferences preferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final String [] refCode = new String[]{SharedPrefUtils.getInviteCode(preferences)};
        if(TextUtils.isEmpty(refCode[0]))
            SharedPrefUtils.storeInviteCode(preferences.edit(), (refCode[0] = MiscUtils.getInviteCode()));
        ((TextView)rootView.findViewById(R.id.refCode)).setText(refCode[0]);

        final String[] inviteOptions = {"Whatsapp","Facebook Messenger","Twitter","Google+"};
        final String [] packageNames = {"com.whatsapp","com.facebook.orca","com.twitter.android","com.google.android.apps.plus"};

        inviteList.setAdapter(new InviteListAdapter(getActivity(),R.layout.invite_list_item,inviteOptions));
        inviteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Hey! Checkout and download my phone music collection with just a click! Use my invite code " + refCode[0] +
                                ". https://play.google.com/store/apps/details?id=reach.project");
                sendIntent.setType("text/plain");
                sendIntent.setPackage(packageNames[position]);
                try{
                    startActivity(sendIntent);
                }
                catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(),inviteOptions[position]+" is not Installed",Toast.LENGTH_SHORT).show();
                }
            }
        });
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        if (getArguments()!=null&&getArguments().getBoolean("first"))
            inflater.inflate(R.menu.invite_menu, menu);
        else
            inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch(id){

            case R.id.next_button: {
                mListener.onNextClicked();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
