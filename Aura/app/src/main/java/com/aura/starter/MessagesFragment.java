package com.aura.starter;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*; import androidx.fragment.app.Fragment;

public class MessagesFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s){
        TextView t = new TextView(requireContext());
        t.setText("Messages (placeholder)");
        t.setPadding(40,40,40,40);
        return t;
    }
}
