// Generated by view binder compiler. Do not edit!
package com.newtown.billsplitter.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.google.android.flexbox.FlexboxLayout;
import com.newtown.billsplitter.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ItemBillBinding implements ViewBinding {
  @NonNull
  private final CardView rootView;

  @NonNull
  public final ImageButton editItemButton;

  @NonNull
  public final TextView itemName;

  @NonNull
  public final TextView itemPrice;

  @NonNull
  public final FlexboxLayout memberCheckboxContainer;

  private ItemBillBinding(@NonNull CardView rootView, @NonNull ImageButton editItemButton,
      @NonNull TextView itemName, @NonNull TextView itemPrice,
      @NonNull FlexboxLayout memberCheckboxContainer) {
    this.rootView = rootView;
    this.editItemButton = editItemButton;
    this.itemName = itemName;
    this.itemPrice = itemPrice;
    this.memberCheckboxContainer = memberCheckboxContainer;
  }

  @Override
  @NonNull
  public CardView getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemBillBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemBillBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_bill, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemBillBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.editItemButton;
      ImageButton editItemButton = ViewBindings.findChildViewById(rootView, id);
      if (editItemButton == null) {
        break missingId;
      }

      id = R.id.itemName;
      TextView itemName = ViewBindings.findChildViewById(rootView, id);
      if (itemName == null) {
        break missingId;
      }

      id = R.id.itemPrice;
      TextView itemPrice = ViewBindings.findChildViewById(rootView, id);
      if (itemPrice == null) {
        break missingId;
      }

      id = R.id.memberCheckboxContainer;
      FlexboxLayout memberCheckboxContainer = ViewBindings.findChildViewById(rootView, id);
      if (memberCheckboxContainer == null) {
        break missingId;
      }

      return new ItemBillBinding((CardView) rootView, editItemButton, itemName, itemPrice,
          memberCheckboxContainer);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
