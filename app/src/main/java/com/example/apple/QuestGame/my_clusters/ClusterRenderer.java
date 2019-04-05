package com.example.apple.QuestGame.my_clusters;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.apple.QuestGame.my_drawable.MultiDrawable;
import com.example.apple.QuestGame.R;
import com.example.apple.QuestGame.models.Coin;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.List;

public class ClusterRenderer extends DefaultClusterRenderer<Coin> {

    private final IconGenerator iconGenerator;
    private final IconGenerator clusterIconGenerator;
    private final ImageView imageView;
    private final ImageView clusterImageView;
    private final int markerWidth;
    private final int markerHeight;
    private TextView textView;
    private Context mContext;
    private Animator animator;

    public ClusterRenderer(Context context, GoogleMap map, ClusterManager<Coin> clusterManager) {
        super(context, map, clusterManager);

        mContext = context;
        iconGenerator = new IconGenerator(context.getApplicationContext());
        clusterIconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());

        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View multiProfile = li.inflate(R.layout.multi_drawable, null);
        clusterIconGenerator.setContentView(multiProfile);
        clusterIconGenerator.setBackground(ContextCompat.getDrawable(context.getApplicationContext(), R.drawable.circle_zoom_off));
        clusterImageView =  multiProfile.findViewById(R.id.image);
        textView = multiProfile.findViewById(R.id.amu_text);


        markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
        iconGenerator.setTextAppearance(R.style.AppTheme_WhiteTextAppearance);
        iconGenerator.setBackground(ContextCompat.getDrawable(context.getApplicationContext(), R.drawable.circle_shape));
        iconGenerator.setContentView(imageView);
        textView = new TextView(context);
    }


    @Override
    protected void onBeforeClusterRendered(Cluster<Coin> cluster, MarkerOptions markerOptions) {
        super.onBeforeClusterRendered(cluster, markerOptions);
        List<Drawable> coinIcons = new ArrayList<>(Math.min(4, cluster.getSize()));
        for (Coin p : cluster.getItems()) {
            // Draw 4 at most.
            if (coinIcons.size() == 4) break;
            Drawable drawable = mContext.getResources().getDrawable(p.getIconPicture());
            drawable.setBounds(0, 0, markerWidth, markerHeight);
            coinIcons.add(drawable);
        }

        MultiDrawable multiDrawable = new MultiDrawable(coinIcons);
        multiDrawable.setBounds(0, 0, markerWidth, markerHeight);

        clusterImageView.setImageDrawable(multiDrawable);
        Bitmap icon = clusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        int bucket = getBucket(cluster);
        textView.setText(String.valueOf(bucket));
    }

    @Override
    protected void onBeforeClusterItemRendered(Coin item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        imageView.setImageResource(item.getIconPicture());
        Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster<Coin> cluster) {
        if(cluster.getSize() <= 4){
            iconGenerator.setContentView(imageView);
        }
        return cluster.getSize() > 4;
    }

    @Override
    protected void onClusterItemRendered(Coin clusterItem, Marker marker) {
        super.onClusterItemRendered(clusterItem, marker);
        animator = ObjectAnimator.ofFloat(marker
                , "alpha", 1f, 0f);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
            }
            @Override public void onAnimationStart(Animator animator) {
            }
            @Override public void onAnimationCancel(Animator animator) {}
            @Override public void onAnimationRepeat(Animator animator) {
            }
        });
        ((ObjectAnimator) animator).setRepeatCount(Animation.INFINITE);
        animator.setDuration(5000).start();
    }
}
