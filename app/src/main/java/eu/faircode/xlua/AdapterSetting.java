package eu.faircode.xlua;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.faircode.xlua.api.XResult;

import eu.faircode.xlua.api.settings.LuaSettingExtended;
import eu.faircode.xlua.api.xstandard.interfaces.IDividerKind;
import eu.faircode.xlua.api.xstandard.interfaces.ISettingUpdate;
import eu.faircode.xlua.logger.XLog;
import eu.faircode.xlua.random.randomizers.NARandomizer;
import eu.faircode.xlua.ui.dialogs.NoRandomDialog;
import eu.faircode.xlua.ui.dialogs.SettingDeleteDialog;
import eu.faircode.xlua.random.GlobalRandoms;
import eu.faircode.xlua.random.IRandomizer;
import eu.faircode.xlua.ui.AlertMessage;
import eu.faircode.xlua.ui.SettingsQue;
import eu.faircode.xlua.utilities.SettingUtil;
import eu.faircode.xlua.utilities.UiUtil;
import eu.faircode.xlua.utilities.ViewUtil;

public class AdapterSetting extends RecyclerView.Adapter<AdapterSetting.ViewHolder> implements Filterable, IDividerKind, ISettingUpdate {
    private static final String TAG = "XLua.AdapterSetting";
    private final List<IRandomizer> randomizers = GlobalRandoms.getRandomizers();

    private final HashMap<String, Boolean> expanded = new HashMap<>();
    private final List<LuaSettingExtended> settings = new ArrayList<>();
    private List<LuaSettingExtended> filtered = new ArrayList<>();

    private SettingsQue settingsQue;

    private AppGeneric application;
    private FragmentManager fragmentManager;

    //Filter / Divider VARs
    private boolean isRandomizingAll = false;;

    private boolean isSearching = false;
    private boolean hasChanged = false;

    private boolean dataChanged = false;
    private CharSequence query = null;

    public class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener,
            View.OnLongClickListener,
            TextWatcher,
            CompoundButton.OnCheckedChangeListener,
            AdapterView.OnItemSelectedListener,
            ISettingUpdate {

        final View itemView;
        final CardView cvSetting;
        final ConstraintLayout clLayout;

        final TextView tvSettingName, tvSettingDescription, tvSettingNameFull;

        final TextInputEditText tiSettingValue;
        final ImageView ivBtSave, ivBtDelete, ivBtRandomize, ivReset, ivSettingDrop;
        final CheckBox cbSelected;

        final Spinner spRandomSelector;
        final ArrayAdapter<IRandomizer> adapterRandomizer;

        ViewHolder(View itemView) {
            super(itemView);

            this.itemView = itemView;
            ivSettingDrop = itemView.findViewById(R.id.ivExpanderSettingsSetting);
            cvSetting = itemView.findViewById(R.id.cvSetting);
            clLayout = itemView.findViewById(R.id.clSettingLayout);

            tvSettingNameFull = itemView.findViewById(R.id.tvSettingsSettingFullName);
            tvSettingName = itemView.findViewById(R.id.tvSettingNameLabel);
            tvSettingDescription = itemView.findViewById(R.id.tvSettingsSettingDescription);
            tiSettingValue = itemView.findViewById(R.id.tiSettingsSettingValue);
            cbSelected = itemView.findViewById(R.id.cbSettingEnabled);

            ivBtSave = itemView.findViewById(R.id.ivBtSaveSettingSetting);
            ivBtDelete = itemView.findViewById(R.id.ivBtDeleteSetting);
            ivReset = itemView.findViewById(R.id.ivBtSettingReset);

            adapterRandomizer = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item);
            adapterRandomizer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ivBtRandomize = itemView.findViewById(R.id.ivBtRandomSettingValue);
            spRandomSelector = itemView.findViewById(R.id.spSettingRandomizerSpinner);
            if(DebugUtil.isDebug()) Log.i(TAG, "Created the Empty Array for Configs Fragment Config");
            spRandomSelector.setTag(null);
            spRandomSelector.setAdapter(adapterRandomizer);
        }

        private void unWire() {
            //For these add some cool concept to building like your Service Thing
            //.addOnClickListener(item).addOnClickListener
            itemView.setOnClickListener(null);
            ivBtRandomize.setOnClickListener(null);
            ivBtSave.setOnClickListener(null);
            tiSettingValue.removeTextChangedListener(this);
            ivSettingDrop.setOnClickListener(null);
            ivBtDelete.setOnClickListener(null);
            ivReset.setOnClickListener(null);
            cbSelected.setOnCheckedChangeListener(null);
            spRandomSelector.setOnItemSelectedListener(null);
            cbSelected.setOnLongClickListener(null);
            ivBtRandomize.setOnLongClickListener(null);
            ivBtSave.setOnLongClickListener(null);
            ivBtDelete.setOnLongClickListener(null);
            ivReset.setOnLongClickListener(null);
        }

        private void wire() {
            itemView.setOnClickListener(this);
            ivBtRandomize.setOnClickListener(this);
            ivBtSave.setOnClickListener(this);
            tiSettingValue.addTextChangedListener(this);
            ivSettingDrop.setOnClickListener(this);
            ivBtDelete.setOnClickListener(this);
            ivReset.setOnClickListener(this);
            cbSelected.setOnCheckedChangeListener(this);
            spRandomSelector.setOnItemSelectedListener(this);
            cbSelected.setOnLongClickListener(this);
            ivBtRandomize.setOnLongClickListener(this);
            ivBtSave.setOnLongClickListener(this);
            ivBtDelete.setOnLongClickListener(this);
            ivReset.setOnLongClickListener(this);
        }

        @SuppressLint({"NonConstantResourceId", "NotifyDataSetChanged"})
        @Override
        public boolean onLongClick(View v) {
            int code = v.getId();
            XLog.i("onLongClick id=" + code);
            final LuaSettingExtended setting = filtered.get(getAdapterPosition());
            switch (code) {
                case R.id.ivBtRandomSettingValue:
                    Snackbar.make(v, R.string.menu_setting_random_hint, Snackbar.LENGTH_LONG).show();
                    break;
                case R.id.ivBtSaveSettingSetting:
                    Snackbar.make(v, R.string.menu_setting_save_hint, Snackbar.LENGTH_LONG).show();
                    break;
                case R.id.ivBtDeleteSetting:
                    Snackbar.make(v, R.string.menu_setting_delete_hint, Snackbar.LENGTH_LONG).show();
                    break;
                case R.id.ivBtSettingReset:
                    Snackbar.make(v, R.string.menu_setting_reset_hint, Snackbar.LENGTH_LONG).show();
                    break;
                case R.id.cbSettingEnabled:
                    String gId = setting.getGroupId();
                    boolean isSelected = setting.isEnabled();
                    Toast.makeText(v.getContext(), R.string.menu_setting_selecting_all_hint + gId, Toast.LENGTH_SHORT).show();
                    unWire();
                    for(LuaSettingExtended s : filtered) {
                        if(s.getGroupId().equalsIgnoreCase(gId))
                            s.setIsEnabled(isSelected);
                    }

                    notifyDataSetChanged();
                    wire();
            }

            return true;
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(final View view) {
            int id = view.getId();
            final LuaSettingExtended setting = filtered.get(getAdapterPosition());
            String name = setting.getName();
            XLog.i("onClick id=" + id + " selected=" + setting);
            switch (id) {
                case R.id.ivExpanderSettingsSetting:
                case R.id.itemViewSetting:
                    ViewUtil.internalUpdateExpanded(expanded, name);
                    updateExpanded();
                    break;
                case R.id.ivBtRandomSettingValue:
                    if(NARandomizer.isNA(setting.getRandomizer())) {
                        new NoRandomDialog()
                                .show(fragmentManager,
                                        view.getResources().getString(R.string.title_no_random));
                    }else {
                        setting.randomizeValue(view.getContext());
                        SettingUtil.initCardViewColor(view.getContext(), tvSettingName, cvSetting, setting);
                    }
                    break;
                case R.id.ivBtSaveSettingSetting:
                    settingsQue.sendSetting(view.getContext(), setting, false, false, this);
                    break;
                case R.id.ivBtDeleteSetting:
                    SettingDeleteDialog setDialog = new SettingDeleteDialog(setting, application);
                    setDialog.show(fragmentManager, "Delete Setting");
                    break;
                case R.id.ivBtSettingReset:
                    if(setting.isModified()) {
                        setting.resetModified(true);
                        SettingUtil.initCardViewColor(view.getContext(), tvSettingName, cvSetting, setting);
                    }
                    break;
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onSettingUpdatedSuccessfully(Context context, LuaSettingExtended setting, XResult result) {
            setting.updateValue();
            Toast.makeText(context, "Setting updated successfully!", Toast.LENGTH_SHORT).show();
            SettingUtil.initCardViewColor(context, tvSettingName, cvSetting, setting);
            notifyDataSetChanged();
        }

        @Override
        public void onSettingUpdateFailed(Context context, LuaSettingExtended setting, XResult result) { AlertMessage.displayMessageFailed(context, setting, result); }

        @Override
        public void onBatchFinished(Context context, List<LuaSettingExtended> successful, List<LuaSettingExtended> failed) { }

        @Override
        public void onException(Context context, Exception e, LuaSettingExtended setting) { AlertMessage.displayMessageException(context, setting, e); }

        @Override
        public void afterTextChanged(Editable editable) {
            LuaSettingExtended setting = filtered.get(getAdapterPosition());
            if(!isRandomizingAll && !setting.isBusy()) {
                String s = editable.toString();
                if(TextUtils.isEmpty(s)) setting.setModifiedValue(null);
                else setting.setModifiedValue(editable.toString());
                SettingUtil.initCardViewColor(cvSetting.getContext(), tvSettingName, cvSetting, setting);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @SuppressLint("NotifyDataSetChanged")
        void updateExpanded() {
            LuaSettingExtended setting = filtered.get(getAdapterPosition());
            String name = setting.getName();
            boolean isExpanded = expanded.containsKey(name) && Boolean.TRUE.equals(expanded.get(name));
            ViewUtil.setViewsVisibility(ivSettingDrop, isExpanded, tvSettingDescription, tiSettingValue, spRandomSelector, ivReset, ivBtRandomize, ivBtSave, ivBtDelete);
        }

        @SuppressLint({"NonConstantResourceId", "NotifyDataSetChanged"})
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int code = buttonView.getId();
            if(DebugUtil.isDebug()) Log.i(TAG, "onCheckedChanged=" + code + " isChecked=" + isChecked);
            final LuaSettingExtended setting = filtered.get(getAdapterPosition());
            if (code == R.id.cbSettingEnabled) {
                setting.setIsEnabled(isChecked);
                notifyDataSetChanged();
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updateSelection(); }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { updateSelection(); }

        private void updateSelection() {
            if(UiUtil.handleSpinnerSelection(spRandomSelector, filtered, getAdapterPosition())) {
                LuaSettingExtended setting = filtered.get(getAdapterPosition());
                SettingUtil.initCardViewColor(spRandomSelector.getContext(), tvSettingName, cvSetting, setting);
            }
        }
    }

    AdapterSetting() { setHasStableIds(true); }
    AdapterSetting(FragmentManager fragmentManager) { this(); this.fragmentManager = fragmentManager;  }

    @Override
    public String getDividerID(int position) { return filtered.get(position).getGroupId(); }

    @Override
    public String getLongID(int position) { return filtered.get(position).getName(); }

    @Override
    public boolean isSearching() { return isSearching; }

    @Override
    public boolean hasChanged() { return hasChanged; }

    @Override
    public void resetHashChanged() { this.hasChanged = false; }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onSettingUpdatedSuccessfully(Context context, LuaSettingExtended setting, XResult result) {
        setting.updateValue();
        setting.setIsBusy(false);
        Log.i(TAG, "Successfully updated setting=" + setting.getName());
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onSettingUpdateFailed(Context context, LuaSettingExtended setting, XResult result) {
        Log.w(TAG, "Failed to update setting=" + setting.getName());
        setting.setIsBusy(false);
        notifyDataSetChanged();
    }

    @Override
    public void onException(Context context, Exception e, LuaSettingExtended setting) { }

    @Override
    public void onBatchFinished(Context context, List<LuaSettingExtended> successful, List<LuaSettingExtended> failed) { AlertMessage.displayMessageBatch(context, successful, failed, application); }

    @SuppressLint("NotifyDataSetChanged")
    void randomizeAll(Context context) {
        Log.i(TAG, "Invoking the randomize all.. size filtered=" + filtered.size());
        isRandomizingAll = true;
        int randomized = 0;
        for(LuaSettingExtended e : filtered) {
            if(e.getRandomizer() != null && e.isEnabled()) {
                if(NARandomizer.isNA(e.getRandomizer()))
                    continue;

                e.randomizeValue(context);
                randomized++;
            }
        }

        isRandomizingAll = false;
        Log.i(TAG, "Finished randomizing all settings randomized=" + randomized);
        Toast.makeText(context, "Finished Randomizing all props, randomized count=" + randomized, Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    void deleteSelected(Context context) {
        Log.i(TAG, "Delete Selected settings... filtered=" + filtered.size());
        List<LuaSettingExtended> batch = new ArrayList<>();
        for(LuaSettingExtended e : filtered)
            if(!e.isValueNull() && e.isEnabled()) { batch.add(e); e.setIsBusy(true); }

        notifyDataSetChanged();
        if(!batch.isEmpty()) {
            Log.i(TAG, "settings=" + settings.size() + " batch size=" + batch.size());
            settingsQue.batchUpdate(context, batch, true, this);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    void saveAll(Context context) {
        Log.i(TAG, "Saving all settings... filtered=" + filtered.size());
        List<LuaSettingExtended> batch = new ArrayList<>();
        for(LuaSettingExtended e : filtered)
            if(e.isModified()) { batch.add(e); e.setIsBusy(true); }

        notifyDataSetChanged();
        if(!batch.isEmpty()) {
            Log.i(TAG, "settings=" + settings.size() + " batch size=" + batch.size());
            settingsQue.batchUpdate(context, batch, false, this);
        }
    }

    void set(List<LuaSettingExtended> settings, AppGeneric application) {
        for(LuaSettingExtended s : settings) {
            if(s.getName() == null) {
                XLog.e("BAD NULL NAME s=" + s.getName() + " d=" + s.getDescription() + " df=" + s.getDefaultValue());
                continue;
            }

            s.resetModified();
            s.bindRandomizer(randomizers);
        }

        this.dataChanged = true;
        this.settings.clear();
        this.settings.addAll(settings);

        if(this.settingsQue == null) {
            this.application = application;
            this.settingsQue = new SettingsQue(application);
        }

        if(DebugUtil.isDebug())
            Log.i(TAG, "Internal Count=" + this.settings.size() + " app=" + application);

        getFilter().filter(query);
    }


    @Override
    public Filter getFilter() {
        return new Filter() {
            private boolean expanded1 = false;
            @Override
            protected FilterResults performFiltering(CharSequence query) {
                AdapterSetting.this.query = query;
                AdapterSetting.this.isSearching = true;
                AdapterSetting.this.hasChanged = true;
                List<LuaSettingExtended> visible = new ArrayList<>(settings);
                List<LuaSettingExtended> results = new ArrayList<>();

                try {
                    if (TextUtils.isEmpty(query)) results.addAll(visible);
                    else {
                        String q = query.toString().toLowerCase().trim();
                        for(LuaSettingExtended setting : visible) {
                            if(setting.getName().toLowerCase().contains(q))
                                results.add(setting);
                            else if(setting.getValue() != null && setting.getValue().toLowerCase().contains(q))
                                results.add(setting);
                            else if(SettingUtil.cleanSettingName(setting.getName()).toLowerCase().contains(q))
                                results.add(setting);
                        }
                    }

                    if (results.size() == 1) {
                        String settingName = results.get(0).getName();
                        if (!expanded.containsKey(settingName)) {
                            expanded1 = true;
                            expanded.put(settingName, true);
                        }
                    }
                }catch (Exception e) {
                    XLog.e("Filtering settings failed", e);
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = results;
                filterResults.count = results.size();
                return filterResults;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void publishResults(CharSequence query, FilterResults result) {
                try {
                    final List<LuaSettingExtended> settings = (result.values == null ? new ArrayList<LuaSettingExtended>() : (List<LuaSettingExtended>) result.values);
                    Log.i(TAG, "Filtered settings size=" + settings.size());

                    if(dataChanged) {
                        dataChanged = false;
                        filtered = settings;
                        isSearching = false;
                        notifyDataSetChanged();
                    }else {
                        DiffUtil.DiffResult diff =
                                DiffUtil.calculateDiff(new AppDiffCallback(expanded1, filtered, settings));
                        filtered = settings;
                        isSearching = false;
                        diff.dispatchUpdatesTo(AdapterSetting.this);
                    }
                }catch (Exception e) {
                    XLog.e("Failed to Publish Results for Adapter Settings", e);
                }
            }
        };
    }

    private static class AppDiffCallback extends DiffUtil.Callback {
        private final boolean refresh;
        private final List<LuaSettingExtended> prev;
        private final List<LuaSettingExtended> next;
        AppDiffCallback(boolean refresh, List<LuaSettingExtended> prev, List<LuaSettingExtended> next) {
            this.refresh = refresh;
            this.prev = prev;
            this.next = next;
        }

        @Override
        public int getOldListSize() { return prev.size(); }

        @Override
        public int getNewListSize() { return next.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            LuaSettingExtended s1 = prev.get(oldItemPosition);
            LuaSettingExtended s2 = next.get(newItemPosition);
            return (!refresh && s1.getName().equalsIgnoreCase(s2.getName()));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            LuaSettingExtended s1 = prev.get(oldItemPosition);
            LuaSettingExtended s2 = next.get(newItemPosition);

            if(!s1.getName().equalsIgnoreCase(s2.getName())) return false;
            if(s1.getValue() == null || s2.getValue() == null) return false;
            return s1.getValue().equalsIgnoreCase(s2.getValue());
        }
    }

    @Override
    public long getItemId(int position) { return filtered.get(position).hashCode(); }

    @Override
    public int getItemCount() { return filtered.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.settingitem, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.unWire();
        LuaSettingExtended setting = filtered.get(position);
        setting.bindInputTextBox(holder.tiSettingValue);

        holder.tvSettingName.setText(SettingUtil.cleanSettingName(setting.getName()));  //Setting Name
        holder.tvSettingNameFull.setText(setting.getName());                            //Setting Name RAW
        holder.tvSettingDescription.setText(SettingUtil.generateDescription(setting));  //Setting Description

        //Im TERRIBLE with colors....
        SettingUtil.initCardViewColor(holder.itemView.getContext(), holder.tvSettingName, holder.cvSetting, setting);

        boolean enable = UiUtil.initRandomizer(holder.adapterRandomizer, holder.spRandomSelector, setting, randomizers);
        holder.spRandomSelector.setEnabled(enable);
        holder.ivBtRandomize.setEnabled(enable);
        setting.setInputText();
        holder.cbSelected.setChecked(setting.isEnabled());

        holder.cvSetting.setEnabled(!setting.isBusy());
        holder.ivSettingDrop.setEnabled(!setting.isBusy());

        holder.updateExpanded();
        holder.wire();
    }
}
