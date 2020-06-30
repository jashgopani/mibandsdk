package jashgopani.github.io.mibandsdkexample.adapters;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

import jashgopani.github.io.mibandsdkexample.R;

public class ScanResultsAdapter extends RecyclerView.Adapter<ScanResultsAdapter.ScanResultsViewHolder> {
    private static String TAG = "ScanResultsAdapter";
    private OnScannedDeviceListener mOnScannedDeviceListener;
    private ArrayList<BluetoothDevice> dataList;

    public ScanResultsAdapter(ArrayList<BluetoothDevice> devices, OnScannedDeviceListener mOnScannedDeviceListener) {
        this.dataList = devices;
        this.mOnScannedDeviceListener = mOnScannedDeviceListener;
        setHasStableIds(true);
    }


    @NonNull
    @Override
    public ScanResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.scan_item_layout,parent,false);
        return new ScanResultsViewHolder(view,mOnScannedDeviceListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanResultsViewHolder holder, int position) {
        String name = dataList.get(position).getName();
        String address = dataList.get(position).getAddress();
        holder.deviceName.setText(name==null?"UNKNOWN":name);
        holder.deviceAdd.setText(address);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void updateList(ArrayList<BluetoothDevice> devices) {
        dataList.clear();
        dataList = (ArrayList<BluetoothDevice>) devices.clone();
        this.notifyDataSetChanged();
    }

    public class ScanResultsViewHolder extends RecyclerView.ViewHolder{
        TextView deviceName,deviceAdd;
        OnScannedDeviceListener onScannedDeviceListener;

        public ScanResultsViewHolder(@NonNull View itemView, final OnScannedDeviceListener onScannedDeviceListener) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.nameTv);
            deviceAdd = itemView.findViewById(R.id.addressTv);

            this.onScannedDeviceListener = onScannedDeviceListener;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onScannedDeviceListener.onDeviceClick(getAdapterPosition());
                }
            });
        }

    }

    public interface OnScannedDeviceListener {
        void onDeviceClick(int position);
    }
}
