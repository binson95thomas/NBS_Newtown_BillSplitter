// Generated by view binder compiler. Do not edit!
package com.newtown.billsplitter.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.newtown.billsplitter.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentItemsBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final LinearLayout addItemButton;

  @NonNull
  public final LinearLayout emptyStateLayout;

  @NonNull
  public final RecyclerView itemsRecyclerView;

  @NonNull
  public final TextView totalAmountText;

  private FragmentItemsBinding(@NonNull LinearLayout rootView, @NonNull LinearLayout addItemButton,
      @NonNull LinearLayout emptyStateLayout, @NonNull RecyclerView itemsRecyclerView,
      @NonNull TextView totalAmountText) {
    this.rootView = rootView;
    this.addItemButton = addItemButton;
    this.emptyStateLayout = emptyStateLayout;
    this.itemsRecyclerView = itemsRecyclerView;
    this.totalAmountText = totalAmountText;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentItemsBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentItemsBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_items, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentItemsBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.addItemButton;
      LinearLayout addItemButton = ViewBindings.findChildViewById(rootView, id);
      if (addItemButton == null) {
        break missingId;
      }

      id = R.id.emptyStateLayout;
      LinearLayout emptyStateLayout = ViewBindings.findChildViewById(rootView, id);
      if (emptyStateLayout == null) {
        break missingId;
      }

      id = R.id.itemsRecyclerView;
      RecyclerView itemsRecyclerView = ViewBindings.findChildViewById(rootView, id);
      if (itemsRecyclerView == null) {
        break missingId;
      }

      id = R.id.totalAmountText;
      TextView totalAmountText = ViewBindings.findChildViewById(rootView, id);
      if (totalAmountText == null) {
        break missingId;
      }

      return new FragmentItemsBinding((LinearLayout) rootView, addItemButton, emptyStateLayout,
          itemsRecyclerView, totalAmountText);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
