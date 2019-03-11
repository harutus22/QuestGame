package com.example.apple.QuestGame.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.Coin;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

public class ClusterRenderer extends DefaultClusterRenderer<Coin> {

    private final IconGenerator iconGenerator;
    private final ImageView imageView;
    private final int markerWidth;
    private final int markerHeight;
    private Context mContext;
    private int bucket;
    private MarkerOptions markerOptions;

    public ClusterRenderer(Context context, GoogleMap map, ClusterManager<Coin> clusterManager) {
        super(context, map, clusterManager);

        mContext = context;
        iconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());
        markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
//        iconGenerator.setTextAppearance(R.style.AppTheme_WhiteTextAppearance);
        iconGenerator.setBackground(ContextCompat.getDrawable(mContext.getApplicationContext(), R.drawable.circle_shape));
    }


    @Override
    protected void onBeforeClusterRendered(Cluster<Coin> cluster, MarkerOptions markerOptions) {
        super.onBeforeClusterRendered(cluster, markerOptions);
        bucket = getBucket(cluster);
        Bitmap icon = iconGenerator.makeIcon(getClusterText(bucket));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));

    }

    @Override
    protected void onBeforeClusterItemRendered(Coin item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        this.markerOptions = markerOptions;
        imageView.setImageResource(item.getIconPicture());
        final Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<Coin> cluster) {
        if(cluster.getSize() > 5){
            markerOptions = new MarkerOptions();
            final BitmapDescriptor markerDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
            markerOptions.icon(markerDescriptor);
            iconGenerator.setBackground(ContextCompat.getDrawable(mContext.getApplicationContext(), R.drawable.circle_zoom_off));
            iconGenerator.setTextAppearance(R.style.AppTheme_WhiteTextAppearance);
            final Bitmap icon = iconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }
        else {
            iconGenerator.setContentView(imageView);
        }
        return cluster.getSize() > 5;
    }
}
