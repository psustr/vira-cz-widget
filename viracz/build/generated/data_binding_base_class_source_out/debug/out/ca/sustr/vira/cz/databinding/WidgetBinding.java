// Generated by view binder compiler. Do not edit!
package ca.sustr.vira.cz.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import ca.sustr.vira.cz.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class WidgetBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final TextView emptyLine;

  @NonNull
  public final TextView quoteHeader;

  @NonNull
  public final TextView quoteOfTheDay;

  @NonNull
  public final TextView topicHeader;

  @NonNull
  public final TextView topicOfTheWeek;

  private WidgetBinding(@NonNull RelativeLayout rootView, @NonNull TextView emptyLine,
      @NonNull TextView quoteHeader, @NonNull TextView quoteOfTheDay, @NonNull TextView topicHeader,
      @NonNull TextView topicOfTheWeek) {
    this.rootView = rootView;
    this.emptyLine = emptyLine;
    this.quoteHeader = quoteHeader;
    this.quoteOfTheDay = quoteOfTheDay;
    this.topicHeader = topicHeader;
    this.topicOfTheWeek = topicOfTheWeek;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static WidgetBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static WidgetBinding inflate(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
      boolean attachToParent) {
    View root = inflater.inflate(R.layout.widget, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static WidgetBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.emptyLine;
      TextView emptyLine = ViewBindings.findChildViewById(rootView, id);
      if (emptyLine == null) {
        break missingId;
      }

      id = R.id.quoteHeader;
      TextView quoteHeader = ViewBindings.findChildViewById(rootView, id);
      if (quoteHeader == null) {
        break missingId;
      }

      id = R.id.quoteOfTheDay;
      TextView quoteOfTheDay = ViewBindings.findChildViewById(rootView, id);
      if (quoteOfTheDay == null) {
        break missingId;
      }

      id = R.id.topicHeader;
      TextView topicHeader = ViewBindings.findChildViewById(rootView, id);
      if (topicHeader == null) {
        break missingId;
      }

      id = R.id.topicOfTheWeek;
      TextView topicOfTheWeek = ViewBindings.findChildViewById(rootView, id);
      if (topicOfTheWeek == null) {
        break missingId;
      }

      return new WidgetBinding((RelativeLayout) rootView, emptyLine, quoteHeader, quoteOfTheDay,
          topicHeader, topicOfTheWeek);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
