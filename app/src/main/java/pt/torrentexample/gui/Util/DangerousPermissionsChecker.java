/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pt.torrentexample.gui.Util;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import frostwire.util.Ref;
import pt.torrentexample.R;
import pt.torrentexample.gui.adnetwork.Offers;
import pt.torrentexample.gui.services.Engine;

import java.lang.ref.WeakReference;

/**
 * @author gubatron
 * @author aldenml
 */
public final class DangerousPermissionsChecker implements ActivityCompat.OnRequestPermissionsResultCallback {
    public interface OnPermissionsGrantedCallback {
        void onPermissionsGranted();
    }

    public interface PermissionsCheckerHolder {
        // unused, but will leave, as the abstraction was useful to get a hold of the permissionchecker instance
        // held by MainActivity. We shouldn't create these instances except at the Activity level to play ball
        // with the moronic design by Google's engineers.
        DangerousPermissionsChecker getPermissionsChecker(int requestCode);
    }

    public static final int EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE = 0x000A;

    private final WeakReference<Activity> activityRef;
    private final int requestCode;
    private OnPermissionsGrantedCallback onPermissionsGrantedCallback;

    public DangerousPermissionsChecker(Activity activity, int requestCode) {
        if (activity instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
            this.requestCode = requestCode;
            this.activityRef = Ref.weak(activity);
        } else throw new IllegalArgumentException("The activity must implement ActivityCompat.OnRequestPermissionsResultCallback");
    }

    public void setPermissionsGrantedCallback(OnPermissionsGrantedCallback onPermissionsGrantedCallback) {
        this.onPermissionsGrantedCallback = onPermissionsGrantedCallback;
    }

    public boolean noAccess() {
        // simplified until otherwise necessary.
        return requestCode == EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE && noExternalStorageAccess();
    }

    private boolean noExternalStorageAccess() {
        if (!Ref.alive(activityRef)) {
            return true;
        }
        Activity activity = activityRef.get();
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
               ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED;
    }

    public void requestPermissions() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        Activity activity = activityRef.get();

        String[] permissions = null;
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE:
                permissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                };
                break;
        }

        if (permissions != null) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean permissionWasGranted = false;
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSIONS_REQUEST_CODE:
                permissionWasGranted = onExternalStoragePermissionsResult(permissions, grantResults);
                break;
            default:
                break;
        }

        if (this.onPermissionsGrantedCallback != null && permissionWasGranted) {
            onPermissionsGrantedCallback.onPermissionsGranted();
        }
    }

    private boolean onExternalStoragePermissionsResult(String[] permissions, int[] grantResults) {
        if (!Ref.alive(activityRef)) {
            return false;
        }
        final Activity activity = activityRef.get();
        for (int i=0; i<permissions.length; i++) {
            if (grantResults[i]== PackageManager.PERMISSION_DENIED) {
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setIcon(R.drawable.sd_card_notification);
                    builder.setTitle(R.string.why_we_need_storage_permissions);
                    builder.setMessage(R.string.why_we_need_storage_permissions_summary);
                    builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            shutdownFrostWire();
                        }
                    });
                    builder.setPositiveButton(R.string.request_again, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions();
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return false;
                }
            }
        }
        return true;
    }

    public void shutdownFrostWire() {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();

        Offers.stopAdNetworks(activity);
        activity.finish();
        Engine.instance().shutdown();
    }

    public void restartFrostWire(int delayInMS) {
        if (!Ref.alive(activityRef)) {
            return;
        }
        final Activity activity = activityRef.get();
//        PendingIntent intent = PendingIntent.getActivity(activity.getBaseContext(),
//                0,
//                new Intent(activity.getIntent()), activity.getIntent().getFlags());
        AlarmManager manager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
//        manager.set(AlarmManager.RTC, System.currentTimeMillis() + delayInMS, intent);
        shutdownFrostWire();
    }
}