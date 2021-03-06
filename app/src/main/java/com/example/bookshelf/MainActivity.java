package com.example.bookshelf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import edu.temple.audiobookplayer.AudiobookService;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, ControlFragment.PlayingBookInterface {


    private static final int SEARCH_BOOK_REQUEST_CODE = 21;
    private final String KEY_SELECTED_BOOK = "selectedBook";
    private static final String KEY_BOOK_LIST = "bookList";
    private static final String KEY_BOOK_PROGRESS = "bookProgress";
    private static final String KEY_BOOK_PLAYING = "bookPlaying";
    private FragmentManager fm;
    public AudiobookService.MediaControlBinder binder;

    boolean twoPane;
    private BookDetailsFragment bookDetailsFragment;
    private ControlFragment controlFragment;
    private int currentProgress;
    private boolean isBookPlaying;
    Book selectedBook;
    String booksList="";
    ArrayList<Book> bookListData=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonSecondActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookSearchActivity.class);
                startActivityForResult(intent,SEARCH_BOOK_REQUEST_CODE);
            }
        });

        //Fetch selected book if there was one
        if (savedInstanceState != null){
            selectedBook = savedInstanceState.getParcelable(KEY_SELECTED_BOOK);
            booksList=savedInstanceState.getString(KEY_BOOK_LIST);
            Gson gson=new Gson();
            Type type=new TypeToken<ArrayList<Book>>(){}.getType();
            bookListData=gson.fromJson(booksList,type);

            //check if audio was being played
            isBookPlaying = savedInstanceState.getBoolean(KEY_BOOK_PLAYING, false);
            //get current progress
            currentProgress = savedInstanceState.getInt(KEY_BOOK_PROGRESS);
        }

        twoPane = findViewById(R.id.container2) != null;

        fm = getSupportFragmentManager();

        Fragment fragment1;
        fragment1 = fm.findFragmentById(R.id.container1);


        // At this point, I only want to have BookListFragment be displayed in container_1
        if (fragment1 instanceof BookDetailsFragment) {
            fm.popBackStack();
        } else if (!(fragment1 instanceof BookListFragment))
            fm.beginTransaction()
                    .add(R.id.container1, BookListFragment.newInstance(bookListData))
                    .commit();

        // Setup ControlFragment
        controlFragment = (selectedBook == null) ? new ControlFragment() : ControlFragment.newInstance(selectedBook);
        fm.beginTransaction()
                .replace(R.id.controlContainer, controlFragment)
                .commit();
        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books
         */
        bookDetailsFragment = (selectedBook == null) ? new BookDetailsFragment() : BookDetailsFragment.newInstance(selectedBook);
        if (twoPane) {
            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment)
                    .commit();
        } else if (selectedBook != null) {
            /*
            If a book was selected, and we now have a single container, replace
            BookListFragment with BookDetailsFragment, making the transaction reversible
             */
            fm.beginTransaction()
                    .replace(R.id.container1, bookDetailsFragment)
                    .addToBackStack(null)
                    .commit();
        }

    }

    @Override
    public void bookSelected(int index) {
        //Store the selected book to use later if activity restarts
        selectedBook = bookListData.get(index);

        //update the selected book for ControlFragment
        controlFragment.selectedBook(selectedBook);
        currentProgress = 0;

        if (twoPane)
            /*
            Display selected book using previously attached fragment
             */
            bookDetailsFragment.displayBook(selectedBook);
        else {
            /*
            Display book using new fragment
             */
            fm.beginTransaction()
                    .replace(R.id.container1, BookDetailsFragment.newInstance(selectedBook))
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_BOOK, selectedBook);
        outState.putString(KEY_BOOK_LIST, booksList);
        outState.putInt(KEY_BOOK_PROGRESS, currentProgress);
        outState.putBoolean(KEY_BOOK_PLAYING, binder.isPlaying());

    }

    @Override
    public void onBackPressed() {
        // If the user hits the back button, clear the selected book
        selectedBook = null;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==SEARCH_BOOK_REQUEST_CODE){
            if (resultCode==RESULT_OK){
                assert data != null;
                booksList=data.getStringExtra("result");
                Gson gson=new Gson();
                Type type=new TypeToken<ArrayList<Book>>(){}.getType();
                bookListData=gson.fromJson(booksList,type);

                //remove previously selected book
                controlFragment.selectedBook(selectedBook);

                fm = getSupportFragmentManager();

                fm.beginTransaction()
                        .replace(R.id.container1, BookListFragment.newInstance(bookListData))
                        .commit();
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, AudiobookService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
            // We've bound to AudiobookService, cast the IBinder to get MediaControlBinder instance
            binder = (AudiobookService.MediaControlBinder) iBinder;
            binder.setProgressHandler(new IncomingHandler());

            //Check if audio was playing
            if (selectedBook != null && isBookPlaying) {
                binder.play(selectedBook.getId(), currentProgress);
                controlFragment.updateUI(currentProgress, true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            binder = null;
            //update the UI
            currentProgress = 0;
            controlFragment.updateUI(currentProgress,false);
        }
    };

    @Override
    public void play(int id)
    {
        if (binder.isBinderAlive() && !binder.isPlaying()) {
            binder.play(id,currentProgress);
            controlFragment.updateUI(currentProgress, true);
        }
    }

    @Override
    public void pause()
    {
        if (binder.isBinderAlive() && binder.isPlaying())
            binder.pause();
    }

    @Override
    public void stop()
    {
        if (binder.isBinderAlive() && binder.isPlaying()) {
            binder.stop();
            currentProgress = 0;
            controlFragment.updateUI(currentProgress,false);
        }
    }

    @Override
    public void seekTo(int position)
    {
        if (binder.isBinderAlive() && binder.isPlaying()) {
            currentProgress = position;
            binder.seekTo(position);
        }
    }

    /**
     * Handler of incoming messages from AudioBookService.
     */
    class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) msg.obj;
            if(bookProgress != null) {
                currentProgress = bookProgress.getProgress();
                controlFragment.updateProgress(currentProgress);
            }
        }
    }
}