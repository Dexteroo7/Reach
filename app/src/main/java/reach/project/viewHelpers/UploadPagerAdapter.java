package reach.project.viewHelpers;

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 6/7/14.
 */
public class UploadPagerAdapter extends FragmentPagerAdapter {

    private final Drawable[] items;
    private final Fragment[] fragments;

    public UploadPagerAdapter(final FragmentManager fm, final Drawable[] tags,
                              final Fragment[] fragments) {

        super(fm);
        this.items = tags;
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        final Drawable image = items[position];
        final int size = MiscUtils.dpToPx(20);
        image.setBounds(0, 0, size, size);
        final SpannableString sb = new SpannableString(" ");
        final ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

}
