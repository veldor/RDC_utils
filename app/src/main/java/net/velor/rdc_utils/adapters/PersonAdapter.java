package net.velor.rdc_utils.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.velor.rdc_utils.R;
import net.velor.rdc_utils.handlers.ExcelHandler;
import net.velor.rdc_utils.subclasses.WorkingPerson;

import java.util.ArrayList;

import utils.App;

public class PersonAdapter extends BaseAdapter {


    private final ArrayList<WorkingPerson> mData;

    public PersonAdapter(ArrayList<WorkingPerson> personList) {
        mData = personList;

    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public WorkingPerson getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View result;

        if (convertView == null) {
            result = LayoutInflater.from(parent.getContext()).inflate(R.layout.person_item, parent, false);
        } else {
            result = convertView;
        }
        WorkingPerson item = getItem(position);
        TextView personNameView = result.findViewById(R.id.personName);
        if (personNameView != null) {
            personNameView.setText(item.name);
        }
        TextView personRoleView = result.findViewById(R.id.personRole);
        if (personRoleView != null) {
            personRoleView.setText(item.role);
            Integer color = ExcelHandler.roleColors.get(item.role);
            if(color != null){
                personRoleView.setTextColor(color);
            }
            else{
                personRoleView.setTextColor(App.getInstance().getResources().getColor(android.R.color.black));
            }
        }

        return result;
    }
}
