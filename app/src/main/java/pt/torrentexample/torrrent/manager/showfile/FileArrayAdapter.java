package pt.torrentexample.torrrent.manager.showfile;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import pt.torrentexample.R;

/**
 * Created by PhucThanh on 1/13/2016.
 */
public class FileArrayAdapter extends BaseAdapter {

    private Context context;
    private List<Item> listItem;

    public FileArrayAdapter(Context context, List<Item> listItem){
        this.context = context;
        this.listItem = listItem;
    }

    @Override
    public int getCount() {
        return listItem.size();
    }

    @Override
    public Object getItem(int position) {
        return listItem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public class ViewHolder{
        TextView tvName, tvData, tvDate;
        ImageView icon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_file_explorer, parent, false);
            holder = new ViewHolder();
            holder.tvName = (TextView) convertView.findViewById(R.id.tvName);
            holder.tvData = (TextView) convertView.findViewById(R.id.tvData);
            holder.tvDate = (TextView) convertView.findViewById(R.id.tvDate);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();
        Item item = listItem.get(position);
        holder.tvName.setText(item.getName());
        holder.tvData.setText(item.getData());
        holder.tvDate.setText(item.getDate());
        String uri = "drawable/" + item.getImage();
        int imageResource = context.getResources().getIdentifier(uri, null, context.getPackageName());
        Drawable imageDrawable = context.getResources().getDrawable(imageResource);
        holder.icon.setImageDrawable(imageDrawable);

        return convertView;
    }
}
