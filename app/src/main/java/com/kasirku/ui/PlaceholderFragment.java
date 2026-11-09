package com.kasirku.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kasirku.R;

/** Layar penanda untuk fitur yang belum dibuat. */
public class PlaceholderFragment extends Fragment {

  private static final String ARG_TITLE = "title";
  private static final String ARG_MESSAGE = "message";

  public static PlaceholderFragment newInstance(String title, String message) {
    PlaceholderFragment fragment = new PlaceholderFragment();
    Bundle args = new Bundle();
    args.putString(ARG_TITLE, title);
    args.putString(ARG_MESSAGE, message);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    int pad = (int) (24 * getResources().getDisplayMetrics().density);
    Bundle args = getArguments();

    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER);
    root.setPadding(pad, pad, pad, pad);

    TextView title = new TextView(requireContext());
    title.setText(args == null ? "" : args.getString(ARG_TITLE, ""));
    title.setTextSize(22);
    title.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_dark));
    title.setGravity(Gravity.CENTER);

    TextView message = new TextView(requireContext());
    message.setText(args == null ? "" : args.getString(ARG_MESSAGE, ""));
    message.setTextSize(15);
    message.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    message.setGravity(Gravity.CENTER);
    message.setPadding(0, pad / 2, 0, 0);

    root.addView(title);
    root.addView(message);
    return root;
  }
}
