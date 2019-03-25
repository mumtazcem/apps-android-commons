package fr.free.nrw.commons.nearby;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.utils.SwipableCardView;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

/**
 * Custom card view for nearby notification card view on main screen, above contributions list
 */
public class NearbyNotificationCardView extends SwipableCardView {
    private Button permissionRequestButton;
    private LinearLayout contentLayout;
    private TextView notificationTitle;
    private TextView notificationDistance;
    private ImageView notificationCompass;
    private ImageView notificationIcon;
    private ProgressBar progressBar;

    public CardViewVisibilityState cardViewVisibilityState;

    public PermissionType permissionType;

    public NearbyNotificationCardView(@NonNull Context context) {
        super(context);
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE;
        init();
    }

    public NearbyNotificationCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE;
        init();
    }

    public NearbyNotificationCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cardViewVisibilityState = CardViewVisibilityState.INVISIBLE;
        init();
    }

    /**
     * Initializes views and action listeners
     */
    private void init() {
        View rootView = inflate(getContext(), R.layout.nearby_card_view, this);

        permissionRequestButton = rootView.findViewById(R.id.permission_request_button);
        contentLayout = rootView.findViewById(R.id.content_layout);

        notificationTitle = rootView.findViewById(R.id.nearby_title);
        notificationDistance = rootView.findViewById(R.id.nearby_distance);
        notificationCompass = rootView.findViewById(R.id.nearby_compass);

        notificationIcon = rootView.findViewById(R.id.nearby_icon);

        progressBar = rootView.findViewById(R.id.progressBar);

        setActionListeners();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // If you don't setVisibility after getting layout params, then you will se an empty space in place of nearby NotificationCardView
        if (((MainActivity)getContext()).defaultKvStore.getBoolean("displayNearbyCardView", true) && this.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
            this.setVisibility(VISIBLE);
        } else {
            this.setVisibility(GONE);
        }
    }


    private void setActionListeners() {
        this.setOnClickListener(view -> ((MainActivity)getContext()).viewPager.setCurrentItem(1));
    }

    @Override public boolean onSwipe(View view) {
        view.setVisibility(GONE);
        // Save shared preference for nearby card view accordingly
        ((MainActivity) getContext()).defaultKvStore.putBoolean("displayNearbyCardView", false);
        ViewUtil.showLongToast(getContext(),
            getResources().getString(R.string.nearby_notification_dismiss_message));
        return true;
    }

    /**
     * Time is up, data for card view is not ready, so do not display it
     */
    private void errorOccurred() {
        this.setVisibility(GONE);
    }

    /**
     * Data for card view is ready, display card view
     */
    private void succeeded() {
        this.setVisibility(VISIBLE);
    }

    /**
     * Pass place information to views with bearing value of compass.
     *
     * @param place   Closes place where we will get information from
     * @param compass bearing value of compass that would show the direction
     */
    public void updateContent(Place place, float compass) {
        Timber.d("Update nearby card notification content");
        this.setVisibility(VISIBLE);
        cardViewVisibilityState = CardViewVisibilityState.READY;
        permissionRequestButton.setVisibility(GONE);
        contentLayout.setVisibility(VISIBLE);
        // Make progress bar invisible once data is ready
        progressBar.setVisibility(GONE);
        // And content views visible since they are ready
        notificationTitle.setVisibility(VISIBLE);
        notificationDistance.setVisibility(VISIBLE);
        notificationIcon.setVisibility(VISIBLE);
        notificationTitle.setText(place.name);
        notificationDistance.setText(place.distance);
        notificationCompass.setRotation(compass);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            /*
              Sometimes we need to preserve previous state of notification card view without getting
              any data from user. Ie. wen user came back from Media Details fragment to Contrib List
              fragment, we need to know what was the state of card view, and set it to exact same state.
             */
            switch (cardViewVisibilityState) {
                case READY:
                    permissionRequestButton.setVisibility(GONE);
                    contentLayout.setVisibility(VISIBLE);
                    // Make progress bar invisible once data is ready
                    progressBar.setVisibility(GONE);
                    // And content views visible since they are ready
                    notificationTitle.setVisibility(VISIBLE);
                    notificationDistance.setVisibility(VISIBLE);
                    notificationCompass.setVisibility(VISIBLE);
                    notificationIcon.setVisibility(VISIBLE);
                    break;
                case LOADING:
                    permissionRequestButton.setVisibility(GONE);
                    contentLayout.setVisibility(VISIBLE);
                    // Set visibility of elements in content layout once it become visible
                    progressBar.setVisibility(VISIBLE);
                    notificationTitle.setVisibility(GONE);
                    notificationDistance.setVisibility(GONE);
                    notificationCompass.setVisibility(GONE);
                    notificationIcon.setVisibility(GONE);
                    permissionRequestButton.setVisibility(GONE);
                    break;
                case ASK_PERMISSION:
                    contentLayout.setVisibility(GONE);
                    permissionRequestButton.setVisibility(VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * This states will help us to preserve progress bar and content layout states
     */
    public enum CardViewVisibilityState {
        LOADING,
        READY,
        INVISIBLE,
        ASK_PERMISSION,
        ERROR_OCCURRED
    }

    /**
     * We need to know which kind of permission we need to request, then update permission request
     * button action accordingly
     */
    public enum PermissionType {
        ENABLE_GPS,
        ENABLE_LOCATION_PERMISSION, // For only after Marshmallow
        NO_PERMISSION_NEEDED
    }

    public void compassAnimationSensorInfoSetter(float degree, float currentDegree){
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        notificationCompass.startAnimation(ra);
    }
}
