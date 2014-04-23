
package org.efalk.altimeter;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Simple preferences activity for my application.  Don't forget to
 * declare this in the manifest.
 */
public class AltimeterPreferences extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    addPreferencesFromResource(R.xml.preferences);
  }
}
