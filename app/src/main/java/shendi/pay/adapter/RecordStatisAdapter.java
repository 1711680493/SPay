package shendi.pay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import shendi.kit.time.TimeUtils;
import shendi.pay.R;
import shendi.pay.bean.NotifyPay;

/**
 * 列表适配器.
 * 创建时间：2023/11/9
 * @author Shendi
 */
public class RecordStatisAdapter extends ArrayAdapter<NotifyPay> {

    public RecordStatisAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NotifyPay np = getItem(position);

        // 初始化此项UI
        View view = LayoutInflater.from(getContext()).inflate(R.layout.record_statis_item,parent, false);

        ((TextView) view.findViewById(R.id.rsiIdText)).setText(String.valueOf(np.getId()));
        ((TextView) view.findViewById(R.id.rsiTitleText)).setText(np.getTitle());
        ((TextView) view.findViewById(R.id.rsiContentText)).setText(np.getContent());
        if (np.getState() == 0) {
            ((TextView) view.findViewById(R.id.rsiReasonText)).setText(np.getReason());
        }
        ((TextView) view.findViewById(R.id.rsiTypeText)).setText(np.getType());
        ((TextView) view.findViewById(R.id.rsiTimeText)).setText(TimeUtils.getFormatTime().getString(np.getTime()));
        ((TextView) view.findViewById(R.id.rsiAmountText)).setText(String.valueOf(np.getAmount() / 100f));

        return view;
    }

}
