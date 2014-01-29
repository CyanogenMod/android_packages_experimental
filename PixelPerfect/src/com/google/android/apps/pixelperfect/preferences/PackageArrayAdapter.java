package com.google.android.apps.pixelperfect.preferences;

import com.google.android.apps.pixelperfect.R;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * {@link ArrayAdapter} for displaying and selecting packages. It is used in two contexts:
 * <ul>
 *   <li> The autocomplete drop-down used for selecting a package to exclude. In that case, package
 *   icon, package name and application name are shown.
 *   <li> The custom exclusion list. In that case, the same information as above is shown, plus
 *   a button to remove the package from the list of excluded ones.
 * </ul>
 */
@SuppressLint("DefaultLocale")
public class PackageArrayAdapter extends ArrayAdapter<PackageItem> implements Filterable {

    @SuppressWarnings("unused")
    private static final String TAG = "PixelPerfect.PackageArrayAdapter";

    private final List<PackageItem> mAllItems;
    private List<PackageItem> mItems;

    /**
     * If true, items are featured in the auto-complete drop-down. Otherwise, it's used in the list
     * of excluded packages.
     */
    private final boolean mForDropDown;

    /** Filter that's used for the autocomplete drop-down. */
    private final PackageFilter mFilter;

    /** Callback used for removing a package name from the list of excluded packages. */
    private final ReIncludePackageCallback mReIncludePackageCallback;

    /**
     * Constructs a {@link PackageArrayAdapter}.
     *
     * @param context the {@link Context}
     * @param resourceId the resource ID for a layout file containing a layout to use when
     *     instantiating views
     * @param items the {@link PackageItem}s
     * @param forDropDown if true, this {@link ArrayAdapter} is for the package selection drop-down,
     *     otherwise, it's for rendering the list of excluded packages
     * @param reIncludePackageCallback this callback is called when the user wants to
     *     remove a given package name from the list of excluded packages. It must be
     *     non-{@code null} if and only if {@code forDropDown} is false
     */
    public PackageArrayAdapter(Context context, int resourceId, List<PackageItem> items,
            boolean forDropDown, @Nullable ReIncludePackageCallback reIncludePackageCallback) {
        super(context, resourceId, items);
        // reIncludePackageCallback is null iff forDropDown is true
        Preconditions.checkArgument(forDropDown != (reIncludePackageCallback != null));
        mAllItems = items;
        mForDropDown = forDropDown;
        // Note, the filter is only exercised for the drop-down.
        mFilter = forDropDown ? new PackageFilter() : null;
        // For the drop-down, start with nothing in the list of items. For the list case, start with
        // all the items (and that won't change since filtering never occurs).
        mItems = forDropDown ? null : mAllItems;
        mReIncludePackageCallback = reIncludePackageCallback;
    }

    @Override
    public int getCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public PackageItem getItem(int position) {
        return mItems.get(position);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Shows the trash button (to remove from the list of excluded packages) if this adapter is
     * used in the list of excluded packages.
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        final PackageItem item = getItem(position);
        if (row == null) {
            row = inflater.inflate(R.layout.package_item, parent, false);
        }

        TextView applicationNameView = (TextView) row.findViewById(R.id.applicationName);
        TextView packageNameView = (TextView) row.findViewById(R.id.packageName);
        if (item.getApplicationName() != null) {
            applicationNameView.setText(item.getApplicationName());
        }
        packageNameView.setText(item.getPackageName());
        if (item.getIcon() != null) {
            ImageView icon = (ImageView) row.findViewById(R.id.icon);
            icon.setImageDrawable(item.getIcon());
        }

        // Don't show the trash button if in the drop-down.
        ImageButton button = (ImageButton) row.findViewById(R.id.button);
        button.setVisibility(mForDropDown ? View.GONE : View.VISIBLE);
        if (!mForDropDown) {
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReIncludePackageCallback.onReIncludePackage(item.getPackageName());
                }
            });
        }

        return row;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    /**
     * A {@link Filter} used to autocomplete on the package name.
     */
    private class PackageFilter extends Filter {

        /**
         * {@inheritDoc}
         *
         * Filters based on the presence of {@code constraint} as a substring of the package name
         * or the app name.
         */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            ArrayList<PackageItem> results = Lists.newArrayList();

            if (constraint != null) {
                if (mAllItems != null && !mAllItems.isEmpty()) {
                    String noramlizedConstraint = PackageItem.getNormalizedString(
                            constraint.toString());
                    for (PackageItem item : mAllItems) {
                        if (item.matches(noramlizedConstraint)) {
                            results.add(item);
                        }
                    }
                }
                filterResults.values = results;
                filterResults.count = results.size();
            }
            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mItems = (ArrayList<PackageItem>) results.values;
            notifyDataSetChanged();
        }
    }

}
