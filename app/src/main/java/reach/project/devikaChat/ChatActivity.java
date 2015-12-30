//package reach.project.devikaChat;
//
//import android.graphics.Bitmap;
//import android.graphics.drawable.BitmapDrawable;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.support.v7.app.ActionBar;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//
//import reach.project.R;
//import reach.project.core.StaticData;
//import reach.project.utils.MiscUtils;
//
//public class ChatActivity extends AppCompatActivity {
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_chat);
//
//        //toolbar stuff
//        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        //actionbar stuff
//        final ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setTitle("Devika");
//            actionBar.setSubtitle("Reach Manager");
//            new SetManagerIcon().executeOnExecutor(StaticData.TEMPORARY_FIX);
//        }
//    }
//
//    private class SetManagerIcon extends AsyncTask<Void,Void,Bitmap> {
//
//        private final int margin = MiscUtils.dpToPx(44);
//
//        @Override
//        protected Bitmap doInBackground(Void... params) {
////            try {
////                return Picasso.with(ChatActivity.this).load(StaticData.DROP_BOX_MANAGER)
////                        .resize(margin, margin)
////                        .centerCrop()
////                        .transform(new CircleTransform()).get();
////            } catch (IOException e) {
////                e.printStackTrace();
////                return null;
////            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Bitmap bitmap) {
//            super.onPostExecute(bitmap);
//            ActionBar actionBar = getSupportActionBar();
//            if (actionBar!=null)
//                actionBar.setIcon(new BitmapDrawable(getResources(), bitmap));
//        }
//    }
//}