package com.example.bookshelf;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BooksAdapter extends BaseAdapter {

    Context context;
    ArrayList<Book> bookList;

    public BooksAdapter(Context context, ArrayList<Book> bookList) {
        this.context = context;
        this.bookList = bookList;
    }

    @Override
    public int getCount() {
        return bookList.size();
    }

    @Override
    public Object getItem(int position) {
        return bookList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView titleTextView, authorTextView;

        if (!(convertView instanceof LinearLayout)) {
            /*
            Inflate a predefined layout file that includes 2 text views.
            We could do this in code, but this seems a little easier
             */
            convertView = LayoutInflater.from(context).inflate(R.layout.books_adapter_layout, parent, false);
        }

        titleTextView = convertView.findViewById(R.id.titleTextView);
        authorTextView = convertView.findViewById(R.id.authorTextView);

        titleTextView.setText(((Book) getItem(position)).getTitle());
        authorTextView.setText(((Book) getItem(position)).getAuthor());

        return convertView;
    }
}

