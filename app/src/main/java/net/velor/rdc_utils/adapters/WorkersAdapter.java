package net.velor.rdc_utils.adapters;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.velor.rdc_utils.R;
import net.velor.rdc_utils.databinding.WorkerLinearItemBinding;
import net.velor.rdc_utils.subclasses.WorkingPerson;

import java.util.ArrayList;

public class WorkersAdapter extends RecyclerView.Adapter<WorkersAdapter.ViewHolder> {
    private final ArrayList<WorkingPerson> mWorkingPeople;
    private LayoutInflater mLayoutInflater;

    public WorkersAdapter(ArrayList<WorkingPerson> workingPeople) {
        mWorkingPeople = workingPeople;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        }
        WorkerLinearItemBinding binding = DataBindingUtil.inflate(mLayoutInflater, R.layout.worker_linear_item, viewGroup, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(mWorkingPeople.get(i));
    }

    @Override
    public int getItemCount() {
        return mWorkingPeople.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private final WorkerLinearItemBinding mBinding;

        ViewHolder(@NonNull WorkerLinearItemBinding itemView) {
            super(itemView.getRoot());
            mBinding = itemView;
        }

        void bind(WorkingPerson workingPerson) {
            mBinding.setVariable(net.velor.rdc_utils.BR.person, workingPerson);
        }
    }
}
